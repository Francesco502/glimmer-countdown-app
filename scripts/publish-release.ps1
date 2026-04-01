# Publish the direct APK to GitHub Release.
# Usage:
#   .\scripts\publish-release.ps1
#   .\scripts\publish-release.ps1 -Tag v3.7 -ReleaseName v3.7

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

function Get-VersionName {
    param([string]$Path)

    $fallback = '3.7'
    if (-not (Test-Path $Path)) {
        return $fallback
    }

    $line = Get-Content $Path | Where-Object {
        $_ -match '^\s*VERSION_NAME\s*=\s*(.+)$'
    } | Select-Object -First 1

    if ($line -match 'VERSION_NAME\s*=\s*(.+)') {
        return $Matches[1].Trim()
    }

    return $fallback
}

function Get-ReleaseNotes {
    param(
        [string]$Path,
        [string]$VersionName
    )

    $fallback = 'Glimmer Android countdown app.'
    if (-not (Test-Path $Path)) {
        return $fallback
    }

    $content = Get-Content $Path -Raw -Encoding UTF8
    $escapedVersion = [regex]::Escape($VersionName)
    $pattern = "(?ms)^##\s+\[$escapedVersion\][^\r\n]*\r?\n(.*?)(?=\r?\n##\s+\[|\z)"

    if ($content -match $pattern) {
        return $Matches[1].Trim()
    }

    return $fallback
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

$versionName = Get-VersionName -Path $gradleProps
if (-not $Tag) {
    $Tag = "v$versionName"
}
if (-not $ReleaseName) {
    $ReleaseName = "v$versionName"
}

$releaseNotes = Get-ReleaseNotes -Path $changelogPath -VersionName $versionName
$apkName = 'glimmer-countdown-' + ($versionName -replace '\.', '-') + '.apk'
$apkPath = Join-Path $rootDir ("app/build/outputs/apk/direct/release/$apkName")

$token = Get-AuthToken
if (-not $token) {
    Write-Host 'ERROR: GITHUB_TOKEN not found and gh auth token is unavailable.' -ForegroundColor Red
    Write-Host '$env:GITHUB_TOKEN = "your_token"; .\scripts\publish-release.ps1' -ForegroundColor Cyan
    exit 1
}

if (-not (Test-Path $apkPath)) {
    Write-Host 'ERROR: APK not found. Run .\gradlew assembleDirectRelease first.' -ForegroundColor Red
    Write-Host $apkPath -ForegroundColor Yellow
    exit 1
}

$headers = @{
    Authorization = "token $token"
    Accept        = 'application/vnd.github.v3+json'
    'User-Agent'  = 'glimmer-countdown-release-script'
}

$releaseBody = @{
    tag_name   = $Tag
    name       = $ReleaseName
    body       = $releaseNotes
    draft      = $false
    prerelease = $false
} | ConvertTo-Json -Depth 4

Write-Host ("Create or update Release $Tag ...") -ForegroundColor Cyan

try {
    $release = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases" `
        -Method Post `
        -Headers $headers `
        -Body $releaseBody `
        -ContentType 'application/json; charset=utf-8'
} catch {
    $statusCode = Get-StatusCode -ErrorRecord $_
    if ($statusCode -ne 422) {
        throw
    }

    Write-Host ("Release $Tag already exists, updating notes and assets...") -ForegroundColor Yellow
    $release = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/$Tag" `
        -Method Get `
        -Headers $headers

    $release = Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/$($release.id)" `
        -Method Patch `
        -Headers $headers `
        -Body $releaseBody `
        -ContentType 'application/json; charset=utf-8'
}

$existingAsset = $release.assets | Where-Object { $_.name -eq $apkName } | Select-Object -First 1
if ($existingAsset) {
    Write-Host ("Delete existing asset $apkName ...") -ForegroundColor Yellow
    Invoke-RestMethod `
        -Uri "https://api.github.com/repos/$owner/$repo/releases/assets/$($existingAsset.id)" `
        -Method Delete `
        -Headers $headers
}

$encodedApkName = [uri]::EscapeDataString($apkName)
$uploadUrl = $release.upload_url -replace '\{\?name,label\}$', "?name=$encodedApkName"

Write-Host 'Upload APK ...' -ForegroundColor Cyan

$apkBytes = [System.IO.File]::ReadAllBytes($apkPath)
$uploadHeaders = @{
    Authorization  = "token $token"
    Accept         = 'application/vnd.github.v3+json'
    'Content-Type' = 'application/vnd.android.package-archive'
    'User-Agent'   = 'glimmer-countdown-release-script'
}

$null = Invoke-RestMethod `
    -Uri $uploadUrl `
    -Method Post `
    -Headers $uploadHeaders `
    -Body $apkBytes

Write-Host 'Done.' -ForegroundColor Green
Write-Host $release.html_url -ForegroundColor Cyan
