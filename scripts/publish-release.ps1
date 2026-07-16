# Publish the direct APK to GitHub Release through a locked, verified draft.
# Usage:
#   .\scripts\publish-release.ps1
#   .\scripts\publish-release.ps1 -Tag v4.0 -ReleaseName v4.0

param(
    [string]$Tag,
    [string]$ReleaseName
)

$ErrorActionPreference = 'Stop'

$owner = 'Francesco502'
$repo = 'glimmer-countdown-app'
$rootDir = Split-Path $PSScriptRoot -Parent
$gradleProps = Join-Path $rootDir 'gradle.properties'
$changelogPath = Join-Path $rootDir 'CHANGELOG.md'
$apkContentType = 'application/vnd.android.package-archive'

function Get-VersionName {
    param([string]$Path)

    if (-not (Test-Path $Path -PathType Leaf)) {
        throw "Unable to resolve VERSION_NAME: missing $Path"
    }
    $line = Get-Content $Path | Where-Object {
        $_ -match '^\s*VERSION_NAME\s*=\s*(.+)$'
    } | Select-Object -First 1
    if ($line -match 'VERSION_NAME\s*=\s*(.+)') {
        $value = $Matches[1].Trim()
        if (-not [string]::IsNullOrWhiteSpace($value)) {
            return $value
        }
    }
    throw "Unable to resolve VERSION_NAME from $Path"
}

function Get-ReleaseNotes {
    param([string]$Path, [string]$VersionName)

    if (-not (Test-Path $Path -PathType Leaf)) {
        throw "Missing changelog: $Path"
    }
    $content = Get-Content $Path -Raw -Encoding UTF8
    $escapedVersion = [regex]::Escape($VersionName)
    $pattern = "(?ms)^##\s+\[$escapedVersion\][^\r\n]*\r?\n(.*?)(?=\r?\n##\s+\[|\z)"
    if ($content -notmatch $pattern -or [string]::IsNullOrWhiteSpace($Matches[1])) {
        throw "Missing changelog section for $VersionName"
    }
    return $Matches[1].Trim()
}

function Get-AuthToken {
    if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_TOKEN)) {
        return $env:GITHUB_TOKEN.Trim()
    }
    try {
        $ghToken = gh auth token 2>$null
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($ghToken)) {
            return $ghToken.Trim()
        }
    } catch {
    }
    return $null
}

function Get-StatusCode {
    param([System.Management.Automation.ErrorRecord]$ErrorRecord)

    if ($null -ne $ErrorRecord.Exception -and $null -ne $ErrorRecord.Exception.Response) {
        return [int]$ErrorRecord.Exception.Response.StatusCode
    }
    return $null
}

function Resolve-RemoteTagCommit {
    param(
        [object]$GitObject,
        [hashtable]$Headers,
        [int]$MaxDepth = 8
    )

    $currentObject = $GitObject
    $seenObjectShas = @{}
    for ($depth = 0; $depth -lt $MaxDepth; $depth++) {
        $objectType = ([string]$currentObject.type).Trim().ToLowerInvariant()
        $objectSha = ([string]$currentObject.sha).Trim().ToLowerInvariant()
        if ($objectSha -notmatch '^[0-9a-f]{40,64}$') {
            throw 'Remote tag contains an invalid git object SHA.'
        }
        if ($seenObjectShas.ContainsKey($objectSha)) {
            throw 'Remote annotated tag chain contains a cycle.'
        }
        $seenObjectShas[$objectSha] = $true
        if ($objectType -eq 'commit') {
            return $objectSha
        }
        if ($objectType -ne 'tag') {
            throw "Remote tag points to unsupported git object type '$objectType'."
        }

        $encodedObjectSha = [uri]::EscapeDataString($objectSha)
        try {
            $annotatedTag = Invoke-RestMethod `
                -Uri "https://api.github.com/repos/$owner/$repo/git/tags/$encodedObjectSha" `
                -Method Get `
                -Headers $Headers
        } catch {
            $statusCode = Get-StatusCode -ErrorRecord $_
            throw "Unable to peel remote annotated tag object (HTTP $statusCode)."
        }
        if (-not [string]::Equals(
                ([string]$annotatedTag.sha).Trim(),
                $objectSha,
                [System.StringComparison]::OrdinalIgnoreCase
            )) {
            throw 'Remote annotated tag object does not match the requested object SHA.'
        }
        $currentObject = $annotatedTag.object
        if ($null -eq $currentObject) {
            throw 'Remote annotated tag object has no target.'
        }
    }
    throw "Remote tag indirection exceeds the maximum depth of $MaxDepth; Remote tag did not resolve to a commit."
}

function Normalize-CertificateFingerprint {
    param([string]$Value, [string]$InvalidMessage)

    $normalized = ($Value -replace ':', '').Trim().ToUpperInvariant()
    if ($normalized -notmatch '^[0-9A-F]{64}$') {
        throw $InvalidMessage
    }
    return $normalized
}

function Find-ApkSigner {
    param([string]$AndroidHome)

    if ([string]::IsNullOrWhiteSpace($AndroidHome)) {
        throw 'ANDROID_HOME is required to locate apksigner.'
    }
    $buildTools = Join-Path $AndroidHome 'build-tools'
    if (-not (Test-Path $buildTools -PathType Container)) {
        throw 'Unable to locate apksigner under ANDROID_HOME/build-tools.'
    }
    $executableName = if ($IsWindows -or $env:OS -eq 'Windows_NT') {
        'apksigner.bat'
    } else {
        'apksigner'
    }
    $stableCandidates = foreach ($directory in Get-ChildItem $buildTools -Directory) {
        [version]$parsedVersion = $null
        if ([version]::TryParse($directory.Name, [ref]$parsedVersion)) {
            $candidate = Join-Path $directory.FullName $executableName
            if (Test-Path $candidate -PathType Leaf) {
                [pscustomobject]@{ Version = $parsedVersion; Path = $candidate }
            }
        }
    }
    $selected = $stableCandidates | Sort-Object Version -Descending | Select-Object -First 1
    if ($null -eq $selected) {
        throw 'Unable to locate a stable apksigner under ANDROID_HOME/build-tools.'
    }
    return $selected.Path
}

function Get-ReleaseByTag {
    param([string]$ReleaseTag, [hashtable]$Headers)

    $encodedTag = [uri]::EscapeDataString($ReleaseTag)
    try {
        return Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/$encodedTag" `
            -Method Get `
            -Headers $Headers
    } catch {
        $statusCode = Get-StatusCode -ErrorRecord $_
        if ($statusCode -eq 404) {
            return $null
        }
        throw "Unable to query release tag $ReleaseTag (HTTP $statusCode)."
    }
}

function Get-ScriptOwnershipMarker {
    param([object]$Release)

    $match = [regex]::Match(
        [string]$Release.body,
        '<!-- glimmer-release-owner:[0-9a-fA-F]{32} -->'
    )
    if (-not $match.Success) {
        return $null
    }
    return $match.Value
}

# Complete all local validation and authenticated remote tag validation before
# the first network mutation (the lock ref creation).
$versionName = Get-VersionName -Path $gradleProps
$expectedTag = "v$versionName"
if ($Tag -and $Tag -ne $expectedTag) {
    throw "Tag $Tag does not match VERSION_NAME $versionName ($expectedTag)."
}
if (-not $Tag) {
    $Tag = $expectedTag
}
if (-not $ReleaseName) {
    $ReleaseName = "v$versionName"
}
$releaseNotes = Get-ReleaseNotes -Path $changelogPath -VersionName $versionName
$apkName = 'glimmer-countdown-' + ($versionName -replace '\.', '-') + '.apk'
$apkPath = Join-Path $rootDir "app/build/outputs/apk/direct/release/$apkName"
if (-not (Test-Path $apkPath -PathType Leaf) -or (Get-Item $apkPath).Length -le 0) {
    throw "APK not found or empty: $apkPath"
}
$apkSize = [long](Get-Item $apkPath).Length
$apkSha256 = (Get-FileHash -Path $apkPath -Algorithm SHA256).Hash.ToLowerInvariant()
$expectedDigest = "sha256:$apkSha256"

$expectedCertInput = $env:GLIMMER_RELEASE_CERT_SHA256
if ([string]::IsNullOrWhiteSpace($expectedCertInput)) {
    throw 'GLIMMER_RELEASE_CERT_SHA256 is required.'
}
$expectedCert = Normalize-CertificateFingerprint `
    -Value $expectedCertInput `
    -InvalidMessage 'Invalid GLIMMER_RELEASE_CERT_SHA256; expected 64 hexadecimal SHA-256 characters.'
$apksigner = Find-ApkSigner -AndroidHome $env:ANDROID_HOME
$verifyOutput = & $apksigner verify --print-certs $apkPath 2>&1
if ($LASTEXITCODE -ne 0) {
    throw 'apksigner verification failed for the Direct APK.'
}
$certMatch = $verifyOutput |
    Select-String -Pattern '^Signer #1 certificate SHA-256 digest:\s*(.+)$' |
    Select-Object -First 1
if (-not $certMatch) {
    throw 'Unable to read the APK signer SHA-256 certificate digest.'
}
$actualCert = Normalize-CertificateFingerprint `
    -Value $certMatch.Matches[0].Groups[1].Value `
    -InvalidMessage 'apksigner returned an invalid SHA-256 certificate digest.'
if ($actualCert -ne $expectedCert) {
    throw 'APK signer certificate does not match GLIMMER_RELEASE_CERT_SHA256.'
}

$token = Get-AuthToken
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'Unable to resolve GitHub authentication token.'
}
$headers = @{
    Authorization          = "Bearer $token"
    Accept                 = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent'           = 'glimmer-countdown-release-script'
}

$localTagRef = "refs/tags/$Tag"
& git -C $rootDir show-ref --verify --quiet $localTagRef 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "Local git tag $Tag does not exist."
}
$localTagCommit = [string](& git -C $rootDir rev-parse --verify "$localTagRef^{commit}" 2>$null)
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($localTagCommit)) {
    throw "Local git tag $Tag does not peel to a commit."
}
$localTagCommit = $localTagCommit.Trim().ToLowerInvariant()
$encodedTag = [uri]::EscapeDataString($Tag)
try {
    $remoteTagRef = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/git/ref/tags/$encodedTag" `
        -Method Get `
        -Headers $headers
} catch {
    $statusCode = Get-StatusCode -ErrorRecord $_
    throw "Unable to resolve exact remote git tag ref $Tag (HTTP $statusCode)."
}
if (-not [string]::Equals(
        [string]$remoteTagRef.ref,
        $localTagRef,
        [System.StringComparison]::Ordinal
    )) {
    throw 'Remote git tag ref does not match exactly.'
}
$remoteTagCommit = Resolve-RemoteTagCommit -GitObject $remoteTagRef.object -Headers $headers
if ($remoteTagCommit -ne $localTagCommit) {
    throw 'Remote tag commit does not match the local tag commit.'
}
$preflightRelease = Get-ReleaseByTag -ReleaseTag $Tag -Headers $headers
$existingReleaseId = [long]0
$previousOwnershipMarker = $null
if ($null -ne $preflightRelease) {
    $existingReleaseId = [long]$preflightRelease.id
    if ($existingReleaseId -le 0 -or
        -not [string]::Equals([string]$preflightRelease.tag_name, $Tag, [System.StringComparison]::Ordinal)) {
        throw "Existing release $Tag has an invalid identity."
    }
    if (-not [bool]$preflightRelease.draft) {
        throw "Release $Tag is already published; refusing to mutate."
    }
    if ([bool]$preflightRelease.prerelease) {
        throw "Existing draft $Tag is a prerelease; refusing to mutate."
    }
    $previousOwnershipMarker = Get-ScriptOwnershipMarker -Release $preflightRelease
    if ([string]::IsNullOrWhiteSpace($previousOwnershipMarker)) {
        throw "Existing draft $Tag is not owned by this publisher; refusing to mutate it."
    }
}

$ownershipNonce = [guid]::NewGuid().ToString('N')
$ownershipMarker = "<!-- glimmer-release-owner:$ownershipNonce -->"
$ownedReleaseNotes = "$releaseNotes`n`n$ownershipMarker"
$lockRef = "refs/heads/release-locks/$Tag"
$lockApiRef = "heads/release-locks/$encodedTag"
$expectedReleaseUrl = "https://github.com/$owner/$repo/releases/tag/$encodedTag"
$encodedApkName = [uri]::EscapeDataString($apkName)
$expectedAssetUrl = "https://github.com/$owner/$repo/releases/download/$encodedTag/$encodedApkName"

function New-ReleaseLock {
    $body = @{ ref = $lockRef; sha = $remoteTagCommit } | ConvertTo-Json
    try {
        $created = Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/git/refs" `
            -Method Post `
            -Headers $headers `
            -Body $body `
            -ContentType 'application/json; charset=utf-8'
    } catch {
        $statusCode = Get-StatusCode -ErrorRecord $_
        if ($statusCode -in @(409, 422)) {
            throw "Another publisher owns the release lock for $Tag; refusing to continue."
        }
        throw "Unable to create the release lock for $Tag (HTTP $statusCode)."
    }
    if (-not [string]::Equals([string]$created.ref, $lockRef, [System.StringComparison]::Ordinal) -or
        -not [string]::Equals([string]$created.object.type, 'commit', [System.StringComparison]::Ordinal) -or
        -not [string]::Equals([string]$created.object.sha, $remoteTagCommit, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Created release lock does not match the requested tag and commit.'
    }
    return ([string]$created.object.sha).Trim().ToLowerInvariant()
}

function Get-ReleaseLock {
    try {
        return Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/git/ref/$lockApiRef" `
            -Method Get `
            -Headers $headers
    } catch {
        $statusCode = Get-StatusCode -ErrorRecord $_
        throw "Unable to re-read the release lock before cleanup (HTTP $statusCode)."
    }
}

function Remove-ReleaseLock {
    param([string]$ExpectedObjectSha)

    # GitHub's ref DELETE has no compare-and-swap precondition. This re-read
    # prevents deletion after a visible ownership change, but operators must
    # not delete/recreate this lock at the same SHA between this GET and DELETE.
    $currentLock = Get-ReleaseLock
    if (-not [string]::Equals([string]$currentLock.ref, $lockRef, [System.StringComparison]::Ordinal)) {
        throw 'Release lock ref changed before cleanup; refusing to delete it.'
    }
    if (-not [string]::Equals(
            ([string]$currentLock.object.sha).Trim(),
            $ExpectedObjectSha,
            [System.StringComparison]::OrdinalIgnoreCase
        ) -or
        -not [string]::Equals(
            ([string]$currentLock.object.sha).Trim(),
            $remoteTagCommit,
            [System.StringComparison]::OrdinalIgnoreCase
        )) {
        throw 'Release lock object changed before cleanup; refusing to delete it.'
    }
    Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/git/refs/$lockApiRef" `
        -Method Delete `
        -Headers $headers
}

function Get-OwnedDraftRelease {
    param([long]$ExpectedReleaseId)

    $current = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/$ExpectedReleaseId" `
        -Method Get `
        -Headers $headers
    if ([long]$current.id -ne $ExpectedReleaseId) {
        throw 'Release id changed during publication.'
    }
    if (-not [string]::Equals([string]$current.tag_name, $Tag, [System.StringComparison]::Ordinal)) {
        throw 'Release tag changed during publication.'
    }
    if (-not [bool]$current.draft -or [bool]$current.prerelease) {
        throw 'Release is no longer the owned draft.'
    }
    if (-not ([string]$current.body).Contains($ownershipMarker)) {
        throw 'Release ownership marker does not match.'
    }
    return $current
}

function Get-ResumableDraftRelease {
    param([long]$ExpectedReleaseId, [string]$ExpectedOwnershipMarker)

    $current = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/$ExpectedReleaseId" `
        -Method Get `
        -Headers $headers
    if ([long]$current.id -ne $ExpectedReleaseId) {
        throw 'Release id changed before draft recovery.'
    }
    if (-not [string]::Equals([string]$current.tag_name, $Tag, [System.StringComparison]::Ordinal)) {
        throw 'Release tag changed before draft recovery.'
    }
    if (-not [bool]$current.draft -or [bool]$current.prerelease) {
        throw 'Release is no longer a resumable draft.'
    }
    if (-not ([string]$current.body).Contains($ExpectedOwnershipMarker)) {
        throw 'Previous release ownership marker does not match.'
    }
    return $current
}

function Upload-ReleaseAsset {
    param([object]$Release)

    $expectedUploadTemplate = "https://uploads.github.com/repos/$owner/$repo/releases/$([long]$Release.id)/assets{?name,label}"
    if (-not [string]::Equals(
            [string]$Release.upload_url,
            $expectedUploadTemplate,
            [System.StringComparison]::Ordinal
        )) {
        throw 'Release upload URL is not the expected GitHub uploads endpoint for this release id.'
    }
    $uploadUrl = "https://uploads.github.com/repos/$owner/$repo/releases/$([long]$Release.id)/assets?name=$encodedApkName"
    $bytes = [System.IO.File]::ReadAllBytes($apkPath)
    $uploadHeaders = @{
        Authorization  = $headers.Authorization
        Accept         = $headers.Accept
        'Content-Type' = $apkContentType
        'User-Agent'   = $headers.'User-Agent'
    }
    return Invoke-RestMethod `
        -Uri $uploadUrl `
        -Method Post `
        -Headers $uploadHeaders `
        -Body $bytes
}

function Assert-UploadedAsset {
    param([object]$Asset)

    if (-not [string]::Equals([string]$Asset.name, $apkName, [System.StringComparison]::Ordinal)) {
        throw 'Upload response asset name does not match exactly.'
    }
    if ([long]$Asset.id -le 0) {
        throw 'Upload response asset id is invalid.'
    }
    if (-not [string]::Equals([string]$Asset.state, 'uploaded', [System.StringComparison]::Ordinal)) {
        throw 'Upload response state is not uploaded.'
    }
    if (-not [string]::Equals([string]$Asset.content_type, $apkContentType, [System.StringComparison]::Ordinal)) {
        throw 'Upload response content type does not match.'
    }
    if ([long]$Asset.size -ne $apkSize) {
        throw 'Upload response size does not match the local APK.'
    }
    if (-not [string]::Equals([string]$Asset.digest, $expectedDigest, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Upload response digest does not match the local APK.'
    }
}

function Assert-RefetchedAsset {
    param([object]$Release, [long]$ExpectedAssetId)

    $apkAssets = @($Release.assets | Where-Object {
        ([string]$_.name).EndsWith('.apk', [System.StringComparison]::OrdinalIgnoreCase)
    })
    if ($apkAssets.Count -ne 1) {
        throw 'Release does not contain exactly one APK asset.'
    }
    $asset = $apkAssets[0]
    if (-not [string]::Equals([string]$asset.name, $apkName, [System.StringComparison]::Ordinal)) {
        throw 'Refetched asset name does not match exactly.'
    }
    if ([long]$asset.id -ne $ExpectedAssetId) {
        throw 'Refetched asset id does not match the upload response.'
    }
    if (-not [string]::Equals([string]$asset.state, 'uploaded', [System.StringComparison]::Ordinal)) {
        throw 'Refetched asset state is not uploaded.'
    }
    if ([long]$asset.size -ne $apkSize) {
        throw 'Refetched asset size does not match the local APK.'
    }
    if (-not [string]::Equals([string]$asset.content_type, $apkContentType, [System.StringComparison]::Ordinal)) {
        throw 'Refetched asset content type does not match.'
    }
    if (-not [string]::Equals([string]$asset.digest, $expectedDigest, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refetched asset digest does not match the local APK.'
    }
    if (-not [string]::Equals([string]$asset.browser_download_url, $expectedAssetUrl, [System.StringComparison]::Ordinal)) {
        throw 'Refetched asset URL does not match the expected repository release URL.'
    }
    return $asset
}

function Assert-FinalPublishedSnapshot {
    param([object]$Release, [long]$ExpectedReleaseId, [long]$ExpectedAssetId)

    if ([long]$Release.id -ne $ExpectedReleaseId) {
        throw 'Final release id does not match.'
    }
    if (-not [string]::Equals([string]$Release.tag_name, $Tag, [System.StringComparison]::Ordinal)) {
        throw 'Final release tag does not match.'
    }
    if (-not [string]::Equals([string]$Release.name, $ReleaseName, [System.StringComparison]::Ordinal)) {
        throw 'Final release name does not match.'
    }
    if ([bool]$Release.draft -or [bool]$Release.prerelease) {
        throw 'Final release is not public and stable.'
    }
    if (-not [string]::Equals([string]$Release.html_url, $expectedReleaseUrl, [System.StringComparison]::Ordinal)) {
        throw 'Final release URL does not match.'
    }
    if (-not ([string]$Release.body).Contains($ownershipMarker)) {
        throw 'Final release ownership marker does not match.'
    }
    $null = Assert-RefetchedAsset -Release $Release -ExpectedAssetId $ExpectedAssetId
}

function Get-FinalPublishedRelease {
    param([long]$ExpectedReleaseId, [long]$ExpectedAssetId)

    $lastFailure = 'Final release could not be verified.'
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        try {
            $final = Invoke-RestMethod `
                -Uri "https://api.github.com/repos/$owner/$repo/releases/$ExpectedReleaseId" `
                -Method Get `
                -Headers $headers
            Assert-FinalPublishedSnapshot `
                -Release $final `
                -ExpectedReleaseId $ExpectedReleaseId `
                -ExpectedAssetId $ExpectedAssetId
            return $final
        } catch {
            $lastFailure = $_.Exception.Message
        }
        if ($attempt -lt 3) {
            Start-Sleep -Seconds 2
        }
    }
    throw $lastFailure
}

$lockAcquired = $false
$publishConfirmed = $false
$releaseId = [long]0
$uploadedAssetId = [long]0
$finalRelease = $null
$releaseLockObjectSha = New-ReleaseLock
$lockAcquired = $true
try {
    if ($existingReleaseId -gt 0) {
        # Only a draft carrying the previous invocation's publisher marker may
        # be resumed. Verify it immediately before rotating to this run's nonce.
        $resumeCandidate = Get-ResumableDraftRelease `
            -ExpectedReleaseId $existingReleaseId `
            -ExpectedOwnershipMarker $previousOwnershipMarker
        $resumeBody = @{
            name       = $ReleaseName
            body       = $ownedReleaseNotes
            draft      = $true
            prerelease = $false
        } | ConvertTo-Json -Depth 4
        $null = Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/releases/$existingReleaseId" `
            -Method Patch `
            -Headers $headers `
            -Body $resumeBody `
            -ContentType 'application/json; charset=utf-8'
        $releaseId = $existingReleaseId
    } else {
        # A release appearing after the preflight belongs to another actor. The
        # server lock prevents a conforming publisher race, so never adopt it.
        if ($null -ne (Get-ReleaseByTag -ReleaseTag $Tag -Headers $headers)) {
            throw "Release $Tag appeared after lock acquisition; refusing to mutate it."
        }
        $createBody = @{
            tag_name        = $Tag
            target_commitish = $remoteTagCommit
            name            = $ReleaseName
            body            = $ownedReleaseNotes
            draft           = $true
            prerelease      = $false
        } | ConvertTo-Json -Depth 4
        try {
            $createdRelease = Invoke-RestMethod `
                -Uri "https://api.github.com/repos/$owner/$repo/releases" `
                -Method Post `
                -Headers $headers `
                -Body $createBody `
                -ContentType 'application/json; charset=utf-8'
        } catch {
            $statusCode = Get-StatusCode -ErrorRecord $_
            if ($statusCode -in @(409, 422)) {
                throw "Release $Tag was created concurrently; refusing to take over or mutate it."
            }
            throw "Unable to create owned draft release $Tag (HTTP $statusCode)."
        }
        $releaseId = [long]$createdRelease.id
        if ($releaseId -le 0) {
            throw 'Created release id is invalid.'
        }
    }
    $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId

    # Remove every APK from this invocation's owned draft. Re-fetch ownership
    # immediately before each DELETE so a changed object is never mutated.
    while ($true) {
        $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId
        $apkAssets = @($current.assets | Where-Object {
            ([string]$_.name).EndsWith('.apk', [System.StringComparison]::OrdinalIgnoreCase)
        })
        if ($apkAssets.Count -eq 0) {
            break
        }
        $assetIdToDelete = [long]$apkAssets[0].id
        $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId
        $assetStillOwned = @($current.assets | Where-Object { [long]$_.id -eq $assetIdToDelete })
        if ($assetStillOwned.Count -ne 1) {
            throw 'Draft asset set changed before deletion.'
        }
        Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$assetIdToDelete" `
            -Method Delete `
            -Headers $headers
    }

    $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId
    $uploadResult = Upload-ReleaseAsset -Release $current
    Assert-UploadedAsset -Asset $uploadResult
    $uploadedAssetId = [long]$uploadResult.id

    $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId
    $null = Assert-RefetchedAsset -Release $current -ExpectedAssetId $uploadedAssetId

    # Validate ownership immediately before the publication mutation. A lost or
    # timed-out PATCH is intentionally resolved only by the authoritative GET.
    $current = Get-OwnedDraftRelease -ExpectedReleaseId $releaseId
    $publishBody = @{
        name       = $ReleaseName
        body       = $ownedReleaseNotes
        draft      = $false
        prerelease = $false
    } | ConvertTo-Json -Depth 4
    $publishPatchError = $null
    $publishPatchResponse = $null
    try {
        $publishPatchResponse = Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/releases/$releaseId" `
            -Method Patch `
            -Headers $headers `
            -Body $publishBody `
            -ContentType 'application/json; charset=utf-8'
        Assert-FinalPublishedSnapshot `
            -Release $publishPatchResponse `
            -ExpectedReleaseId $releaseId `
            -ExpectedAssetId $uploadedAssetId
    } catch {
        $publishPatchError = 'Publish PATCH response was unavailable; verifying authoritative final state.'
    }

    $finalRelease = Get-FinalPublishedRelease `
        -ExpectedReleaseId $releaseId `
        -ExpectedAssetId $uploadedAssetId
    $publishConfirmed = $true
    if ($null -ne $publishPatchError) {
        Write-Warning $publishPatchError
    }
} finally {
    if ($lockAcquired) {
        try {
            Remove-ReleaseLock -ExpectedObjectSha $releaseLockObjectSha
        } catch {
            if ($publishConfirmed) {
                Write-Warning "Release was verified as published, but lock cleanup failed. Manually delete $lockRef."
            } else {
                Write-Warning "Publication failed and lock cleanup also failed. The residual lock safely blocks retries; manually inspect $lockRef."
            }
        }
    }
}

Write-Host "Published verified release $Tag." -ForegroundColor Green
Write-Host $finalRelease.html_url -ForegroundColor Cyan
