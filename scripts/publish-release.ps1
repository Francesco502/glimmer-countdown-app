# Publish the direct APK to GitHub Release through a verified draft.
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
    param(
        [string]$Path,
        [string]$VersionName
    )

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

function Normalize-CertificateFingerprint {
    param(
        [string]$Value,
        [string]$InvalidMessage
    )

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
    $candidate = Get-ChildItem $buildTools -Recurse -File |
        Where-Object { $_.Name -in @('apksigner', 'apksigner.bat') } |
        Sort-Object { [version]$_.Directory.Name } -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if (-not $candidate) {
        throw 'Unable to locate apksigner under ANDROID_HOME/build-tools.'
    }
    return $candidate
}

function Get-ReleaseByTag {
    param(
        [string]$Owner,
        [string]$Repo,
        [string]$ReleaseTag,
        [hashtable]$Headers
    )

    $encodedTag = [uri]::EscapeDataString($ReleaseTag)
    try {
        return Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$Owner/$Repo/releases/tags/$encodedTag" `
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

# Complete every local validation before any POST, PATCH, or DELETE can run.
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
    Authorization        = "Bearer $token"
    Accept               = 'application/vnd.github+json'
    'X-GitHub-Api-Version' = '2022-11-28'
    'User-Agent'         = 'glimmer-countdown-release-script'
}

function Update-DraftRelease {
    param(
        [object]$Release,
        [string]$Name,
        [string]$Notes,
        [hashtable]$Headers,
        [string]$Owner,
        [string]$Repo
    )

    if (-not [bool]$Release.draft) {
        throw "Release $($Release.tag_name) is already published; refusing to mutate."
    }
    $draftBody = @{
        name       = $Name
        body       = $Notes
        draft = $true
        prerelease = $false
    } | ConvertTo-Json -Depth 4
    return Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$Owner/$Repo/releases/$($Release.id)" `
        -Method Patch `
        -Headers $Headers `
        -Body $draftBody `
        -ContentType 'application/json; charset=utf-8'
}

function Upload-ReleaseAsset {
    param(
        [object]$Release,
        [string]$AssetName,
        [string]$AssetPath,
        [string]$ContentType,
        [hashtable]$Headers
    )

    $encodedName = [uri]::EscapeDataString($AssetName)
    $uploadUrl = $Release.upload_url -replace '\{\?name,label\}$', "?name=$encodedName"
    $bytes = [System.IO.File]::ReadAllBytes($AssetPath)
    $uploadHeaders = @{
        Authorization  = $Headers.Authorization
        Accept         = $Headers.Accept
        'Content-Type' = $ContentType
        'User-Agent'   = $Headers.'User-Agent'
    }
    return Invoke-RestMethod `
        -Uri $uploadUrl `
        -Method Post `
        -Headers $uploadHeaders `
        -Body $bytes
}

$release = Get-ReleaseByTag -Owner $owner -Repo $repo -ReleaseTag $Tag -Headers $headers
if ($null -ne $release) {
    if (-not [bool]$release.draft) {
        throw "Release $Tag is already published; refusing to mutate."
    }
    $release = Update-DraftRelease `
        -Release $release -Name $ReleaseName -Notes $releaseNotes `
        -Headers $headers -Owner $owner -Repo $repo
} else {
    $createBody = @{
        tag_name   = $Tag
        name       = $ReleaseName
        body       = $releaseNotes
        draft = $true
        prerelease = $false
    } | ConvertTo-Json -Depth 4
    try {
        $release = Invoke-RestMethod `
            -Uri "https://api.github.com/repos/$owner/$repo/releases" `
            -Method Post `
            -Headers $headers `
            -Body $createBody `
            -ContentType 'application/json; charset=utf-8'
    } catch {
        $statusCode = Get-StatusCode -ErrorRecord $_
        if ($statusCode -ne 422) {
            throw "Unable to create draft release $Tag (HTTP $statusCode)."
        }
        $release = Get-ReleaseByTag -Owner $owner -Repo $repo -ReleaseTag $Tag -Headers $headers
        if ($null -eq $release) {
            throw "Release $Tag returned 422 but could not be found."
        }
        if (-not [bool]$release.draft) {
            throw "Release $Tag is already published; refusing to mutate."
        }
        Write-Host "Release appeared during draft creation; updating the existing draft." -ForegroundColor Yellow
        $release = Update-DraftRelease `
            -Release $release -Name $ReleaseName -Notes $releaseNotes `
            -Headers $headers -Owner $owner -Repo $repo
    }
}

@($release.assets) | Where-Object { $_.name -like '*.apk' } | ForEach-Object {
    Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($_.id)" `
        -Method Delete `
        -Headers $headers
}

$null = Upload-ReleaseAsset `
    -Release $release `
    -AssetName $apkName `
    -AssetPath $apkPath `
    -ContentType $apkContentType `
    -Headers $headers

$verified = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/$owner/$repo/releases/$($release.id)" `
    -Method Get `
    -Headers $headers
if (-not [bool]$verified.draft -or $verified.tag_name -ne $Tag) {
    throw "Verified release is no longer the expected draft; refusing to publish."
}
$apkAssets = @($verified.assets | Where-Object { $_.name -like '*.apk' })
$hasVerifiedAsset = $apkAssets.Count -eq 1 -and `
    $apkAssets[0].name -eq $apkName -and `
    [long]$apkAssets[0].size -gt 0 -and `
    -not [string]::IsNullOrWhiteSpace($apkAssets[0].browser_download_url) -and `
    $apkAssets[0].content_type -eq $apkContentType
if (-not $hasVerifiedAsset) {
    throw "Draft does not contain exactly one verified APK named $apkName."
}

$publishBody = @{
    name       = $ReleaseName
    body       = $releaseNotes
    draft = $false
    prerelease = $false
} | ConvertTo-Json -Depth 4
$published = Invoke-RestMethod `
    -Uri "https://api.github.com/repos/$owner/$repo/releases/$($release.id)" `
    -Method Patch `
    -Headers $headers `
    -Body $publishBody `
    -ContentType 'application/json; charset=utf-8'
if ([bool]$published.draft -or $published.tag_name -ne $Tag) {
    throw "GitHub did not publish the expected release $Tag."
}

Write-Host "Published verified release $Tag." -ForegroundColor Green
Write-Host $published.html_url -ForegroundColor Cyan
