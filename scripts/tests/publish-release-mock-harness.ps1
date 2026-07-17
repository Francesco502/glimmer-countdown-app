[CmdletBinding()]
param(
    [ValidateSet(
        'all',
        'success',
        'lock-contention',
        'owned-draft',
        'failure-cleanup',
        'residual-lock'
    )]
    [string]$Scenario = 'all'
)

$ErrorActionPreference = 'Stop'
$isWindowsHost = $IsWindows -or $env:OS -eq 'Windows_NT'

$scenarioNames = @(
    'success',
    'lock-contention',
    'owned-draft',
    'failure-cleanup',
    'residual-lock'
)

if ($Scenario -eq 'all') {
    $pwshName = if ($isWindowsHost) { 'pwsh.exe' } else { 'pwsh' }
    $pwsh = Join-Path $PSHOME $pwshName
    foreach ($scenarioName in $scenarioNames) {
        & $pwsh -NoProfile -File $PSCommandPath -Scenario $scenarioName
        if ($LASTEXITCODE -ne 0) {
            throw "Publisher mock scenario failed: $scenarioName"
        }
    }
    Write-Host "All publisher mock scenarios passed ($($scenarioNames.Count)/$($scenarioNames.Count))."
    exit 0
}

class MockHttpResponse {
    [int]$StatusCode

    MockHttpResponse([int]$statusCode) {
        $this.StatusCode = $statusCode
    }
}

class MockHttpException : System.Exception {
    [MockHttpResponse]$Response

    MockHttpException([string]$message, [int]$statusCode) : base($message) {
        $this.Response = [MockHttpResponse]::new($statusCode)
    }
}

function Assert-Condition {
    param([bool]$Condition, [string]$Message)

    if (-not $Condition) {
        throw "Harness assertion failed: $Message"
    }
}

function Throw-MockHttpError {
    param([string]$Message, [int]$StatusCode)

    throw [MockHttpException]::new($Message, $StatusCode)
}

$sourceRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$fixtureRoot = Join-Path (
    [System.IO.Path]::GetTempPath()
) "glimmer-publisher-$Scenario-$([guid]::NewGuid().ToString('N'))"
$fixtureScripts = Join-Path $fixtureRoot 'scripts'
$fixtureApkDirectory = Join-Path $fixtureRoot 'app/build/outputs/apk/direct/release'
$fixtureSdk = Join-Path $fixtureRoot 'android-sdk'
$fixtureBuildTools = Join-Path $fixtureSdk 'build-tools/37.0.0'
$publisherPath = Join-Path $fixtureScripts 'publish-release.ps1'
$apkPath = Join-Path $fixtureApkDirectory 'glimmer-countdown-4-0.apk'
$apksignerName = if ($isWindowsHost) { 'apksigner.bat' } else { 'apksigner' }
$apksignerPath = Join-Path $fixtureBuildTools $apksignerName
$certificateSha256 = '3b7cb426a82664f891c69511cc2505b67128c8503664639f297291da4ea903ca'
$mockCommit = '1111111111111111111111111111111111111111'
$previousOwnershipMarker = '<!-- glimmer-release-owner:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa -->'

$previousAndroidHome = $env:ANDROID_HOME
$previousCertificate = $env:GLIMMER_RELEASE_CERT_SHA256
$previousToken = $env:GITHUB_TOKEN

try {
    $null = New-Item -ItemType Directory -Path $fixtureScripts -Force
    $null = New-Item -ItemType Directory -Path $fixtureApkDirectory -Force
    $null = New-Item -ItemType Directory -Path $fixtureBuildTools -Force
    Copy-Item (Join-Path $sourceRoot 'scripts/publish-release.ps1') $publisherPath
    Copy-Item (Join-Path $sourceRoot 'gradle.properties') (Join-Path $fixtureRoot 'gradle.properties')
    Copy-Item (Join-Path $sourceRoot 'CHANGELOG.md') (Join-Path $fixtureRoot 'CHANGELOG.md')
    [System.IO.File]::WriteAllBytes(
        $apkPath,
        [System.Text.Encoding]::UTF8.GetBytes('mock signed Direct APK')
    )

    $apksignerScript = if ($isWindowsHost) {
        @"
@echo off
echo Signer #1 certificate SHA-256 digest: $certificateSha256
exit /b 0
"@
    } else {
        @"
#!/bin/sh
echo 'Signer #1 certificate SHA-256 digest: $certificateSha256'
exit 0
"@
    }
    Set-Content -LiteralPath $apksignerPath -Value $apksignerScript -Encoding utf8NoBOM
    if (-not $isWindowsHost) {
        & /bin/chmod +x $apksignerPath
        if ($LASTEXITCODE -ne 0) {
            throw 'Unable to make the mock apksigner executable.'
        }
    }

    $env:ANDROID_HOME = $fixtureSdk
    $env:GLIMMER_RELEASE_CERT_SHA256 = $certificateSha256
    $env:GITHUB_TOKEN = 'mock-token'

    $script:Mock = [ordered]@{
        Calls             = [System.Collections.Generic.List[string]]::new()
        Commit            = $mockCommit
        Release           = $null
        ReleaseCreates    = 0
        ResumePatches     = 0
        PublishPatches    = 0
        AssetDeletes      = 0
        UploadAttempts    = 0
        LockPostAttempts  = 0
        LockHeld          = $false
        LockDeleted       = $false
        CleanupReads      = 0
    }

    function New-MockRelease {
        param(
            [string]$Body,
            [bool]$Draft,
            [object[]]$Assets = @()
        )

        return [pscustomobject]@{
            id         = [long]123
            tag_name   = 'v4.0'
            name       = 'v4.0'
            body       = $Body
            draft      = $Draft
            prerelease = $false
            upload_url = 'https://uploads.github.com/repos/Francesco502/glimmer-countdown-app/releases/123/assets{?name,label}'
            html_url   = 'https://github.com/Francesco502/glimmer-countdown-app/releases/tag/v4.0'
            assets     = @($Assets)
        }
    }

    if ($Scenario -eq 'owned-draft') {
        $script:Mock.Release = New-MockRelease `
            -Body "previous notes`n`n$previousOwnershipMarker" `
            -Draft $true `
            -Assets @(
                [pscustomobject]@{ id = [long]11; name = 'old.apk' },
                [pscustomobject]@{ id = [long]12; name = 'old.aab' }
            )
    }

    function git {
        $argumentsText = (@($args) | ForEach-Object { [string]$_ }) -join ' '
        if ($argumentsText -match '\bshow-ref\b') {
            $global:LASTEXITCODE = 0
            return
        }
        if ($argumentsText -match '\brev-parse\b') {
            $global:LASTEXITCODE = 0
            return $script:Mock.Commit
        }
        $global:LASTEXITCODE = 1
    }

    function Invoke-RestMethod {
        param(
            [string]$Uri,
            [string]$Method = 'Get',
            [hashtable]$Headers,
            [object]$Body,
            [string]$ContentType
        )

        $methodName = $Method.ToUpperInvariant()
        $script:Mock.Calls.Add("$methodName $Uri")

        if ($methodName -eq 'GET' -and $Uri -match '/git/ref/tags/v4\.0$') {
            return [pscustomobject]@{
                ref    = 'refs/tags/v4.0'
                object = [pscustomobject]@{
                    type = 'commit'
                    sha  = $script:Mock.Commit
                }
            }
        }

        if ($methodName -eq 'GET' -and $Uri -match '/releases/tags/v4\.0$') {
            if ($Scenario -eq 'owned-draft') {
                return $script:Mock.Release
            }
            Throw-MockHttpError -Message 'release not found' -StatusCode 404
        }

        if ($methodName -eq 'POST' -and $Uri -match '/git/refs$') {
            $script:Mock.LockPostAttempts++
            if ($Scenario -eq 'lock-contention') {
                Throw-MockHttpError -Message 'lock exists' -StatusCode 422
            }
            $script:Mock.LockHeld = $true
            return [pscustomobject]@{
                ref    = 'refs/heads/release-locks/v4.0'
                object = [pscustomobject]@{
                    type = 'commit'
                    sha  = $script:Mock.Commit
                }
            }
        }

        if ($methodName -eq 'GET' -and $Uri -match '/git/ref/heads/release-locks/v4\.0$') {
            $script:Mock.CleanupReads++
            if ($Scenario -eq 'residual-lock') {
                Throw-MockHttpError -Message 'mock cleanup read failure' -StatusCode 500
            }
            return [pscustomobject]@{
                ref    = 'refs/heads/release-locks/v4.0'
                object = [pscustomobject]@{
                    type = 'commit'
                    sha  = $script:Mock.Commit
                }
            }
        }

        if ($methodName -eq 'DELETE' -and $Uri -match '/git/refs/heads/release-locks/v4\.0$') {
            $script:Mock.LockHeld = $false
            $script:Mock.LockDeleted = $true
            return $null
        }

        if ($methodName -eq 'POST' -and $Uri -match '/repos/Francesco502/glimmer-countdown-app/releases$') {
            $create = $Body | ConvertFrom-Json
            $script:Mock.Release = New-MockRelease -Body ([string]$create.body) -Draft $true
            $script:Mock.ReleaseCreates++
            return $script:Mock.Release
        }

        if ($methodName -eq 'DELETE' -and $Uri -match '/releases/assets/(\d+)$') {
            $assetId = [long]$Matches[1]
            $script:Mock.Release.assets = @(
                $script:Mock.Release.assets | Where-Object { [long]$_.id -ne $assetId }
            )
            $script:Mock.AssetDeletes++
            return $null
        }

        if ($methodName -eq 'POST' -and $Uri -match '^https://uploads\.github\.com/') {
            $script:Mock.UploadAttempts++
            if ($Scenario -in @('failure-cleanup', 'residual-lock')) {
                Throw-MockHttpError -Message 'mock upload failure' -StatusCode 500
            }
            $asset = [pscustomobject]@{
                id                   = [long]789
                name                 = $apkName
                state                = 'uploaded'
                content_type         = $apkContentType
                size                 = [long]$apkSize
                digest               = $expectedDigest
                browser_download_url = 'https://github.com/Francesco502/glimmer-countdown-app/releases/download/v4.0/glimmer-countdown-4-0.apk'
            }
            $script:Mock.Release.assets = @($asset)
            return $asset
        }

        if ($Uri -match '/releases/(\d+)$') {
            $releaseId = [long]$Matches[1]
            if ($releaseId -ne 123 -or $null -eq $script:Mock.Release) {
                Throw-MockHttpError -Message 'release not found' -StatusCode 404
            }
            if ($methodName -eq 'GET') {
                return $script:Mock.Release
            }
            if ($methodName -eq 'PATCH') {
                $update = $Body | ConvertFrom-Json
                $script:Mock.Release.name = [string]$update.name
                $script:Mock.Release.body = [string]$update.body
                $script:Mock.Release.draft = [bool]$update.draft
                $script:Mock.Release.prerelease = [bool]$update.prerelease
                if ([bool]$update.draft) {
                    $script:Mock.ResumePatches++
                } else {
                    $script:Mock.PublishPatches++
                }
                return $script:Mock.Release
            }
        }

        throw "Unhandled mock request: $methodName $Uri"
    }

    $publisherFailure = $null
    try {
        . $publisherPath
    } catch {
        $publisherFailure = $_
    }

    switch ($Scenario) {
        'success' {
            Assert-Condition ($null -eq $publisherFailure) 'success scenario unexpectedly failed'
            Assert-Condition ($script:Mock.ReleaseCreates -eq 1) 'new release was not created exactly once'
            Assert-Condition ($script:Mock.PublishPatches -eq 1) 'release was not published exactly once'
            Assert-Condition (-not [bool]$script:Mock.Release.draft) 'release remained a draft'
            Assert-Condition (@($script:Mock.Release.assets).Count -eq 1) 'final release asset set was not singular'
            Assert-Condition $script:Mock.LockDeleted 'success scenario did not delete its lock'
        }
        'lock-contention' {
            Assert-Condition ($null -ne $publisherFailure) 'lock contention unexpectedly succeeded'
            Assert-Condition (
                $publisherFailure.Exception.Message.Contains('Another publisher owns the release lock')
            ) 'lock contention did not fail closed with the expected message'
            Assert-Condition ($script:Mock.LockPostAttempts -eq 1) 'lock was not attempted exactly once'
            Assert-Condition ($script:Mock.ReleaseCreates -eq 0) 'release was created despite lock contention'
            Assert-Condition (-not $script:Mock.LockDeleted) 'an unowned lock was deleted'
        }
        'owned-draft' {
            Assert-Condition ($null -eq $publisherFailure) 'owned draft recovery unexpectedly failed'
            Assert-Condition ($script:Mock.ReleaseCreates -eq 0) 'owned draft recovery created a replacement release'
            Assert-Condition ($script:Mock.ResumePatches -eq 1) 'owned draft marker was not rotated exactly once'
            Assert-Condition ($script:Mock.AssetDeletes -eq 2) 'owned draft assets were not all deleted'
            Assert-Condition ($script:Mock.PublishPatches -eq 1) 'recovered draft was not published'
            Assert-Condition (@($script:Mock.Release.assets).Count -eq 1) 'recovered release asset set was not singular'
            Assert-Condition $script:Mock.LockDeleted 'owned draft recovery did not delete its lock'
        }
        'failure-cleanup' {
            Assert-Condition ($null -ne $publisherFailure) 'upload failure unexpectedly succeeded'
            Assert-Condition (
                $publisherFailure.Exception.Message.Contains('mock upload failure')
            ) 'upload failure was not propagated'
            Assert-Condition $script:Mock.LockDeleted 'failure path did not clean up its lock'
            Assert-Condition ([bool]$script:Mock.Release.draft) 'failed publication exposed a public release'
        }
        'residual-lock' {
            Assert-Condition ($null -ne $publisherFailure) 'residual lock scenario unexpectedly succeeded'
            Assert-Condition (
                $publisherFailure.Exception.Message.Contains('mock upload failure')
            ) 'primary publication failure was not preserved'
            Assert-Condition ($script:Mock.CleanupReads -eq 1) 'lock cleanup was not attempted'
            Assert-Condition $script:Mock.LockHeld 'residual lock was not retained after cleanup failure'
            Assert-Condition (-not $script:Mock.LockDeleted) 'residual lock was incorrectly deleted'
        }
    }

    Write-Host "[PASS] publisher mock scenario: $Scenario"
} finally {
    $env:ANDROID_HOME = $previousAndroidHome
    $env:GLIMMER_RELEASE_CERT_SHA256 = $previousCertificate
    $env:GITHUB_TOKEN = $previousToken
    if (Test-Path $fixtureRoot) {
        Remove-Item -LiteralPath $fixtureRoot -Recurse -Force
    }
}
