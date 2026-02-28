# Publish Release APK to GitHub Release. Requires env GITHUB_TOKEN (repo scope).
# Run from project root: .\scripts\publish-release.ps1

$ErrorActionPreference = "Stop"
$owner = "Francesco502"
$repo = "glimmer-countdown-app"
$tag = $null
$releaseName = $null
$releaseNotes = "拾光 (Glimmer) 倒计时 / 纪念日应用。支持多语言、主题切换、桌面小组件、应用内检查更新。"
$rootDir = Split-Path $PSScriptRoot -Parent
# APK 文件名与 build.gradle.kts 一致：glimmer-countdown-2-0.apk（或 gradle.properties 中的 VERSION_NAME）
$versionName = "2.0"
$gradleProps = Join-Path $rootDir "gradle.properties"
if (Test-Path $gradleProps) {
    $line = Get-Content $gradleProps | Where-Object { $_ -match "^\s*VERSION_NAME\s*=\s*(.+)$" } | Select-Object -First 1
    if ($line -match "VERSION_NAME\s*=\s*(.+)") { $versionName = $Matches[1].Trim() }
}
$apkName = "glimmer-countdown-" + ($versionName -replace "\.", "-") + ".apk"
$apkPath = Join-Path $rootDir "app/build/outputs/apk/direct/release/$apkName"

if (-not $tag) {
    $tag = "v$versionName"
}
if (-not $releaseName) {
    $releaseName = "v$versionName"
}

if (-not $env:GITHUB_TOKEN) {
    Write-Host "ERROR: GITHUB_TOKEN not set. Create a PAT at GitHub with repo scope, then:" -ForegroundColor Red
    Write-Host '$env:GITHUB_TOKEN = "your_token"; .\scripts\publish-release.ps1' -ForegroundColor Cyan
    exit 1
}

if (-not (Test-Path $apkPath)) {
    Write-Host "ERROR: APK not found. Run .\gradlew assembleDirectRelease first." -ForegroundColor Red
    Write-Host $apkPath -ForegroundColor Yellow
    exit 1
}

$headers = @{
    "Authorization" = "token $env:GITHUB_TOKEN"
    "Accept"        = "application/vnd.github.v3+json"
}

$body = @{
    tag_name         = $tag
    name             = $releaseName
    body             = $releaseNotes
    draft            = $false
} | ConvertTo-Json

Write-Host "Creating release $tag ..." -ForegroundColor Cyan
try {
    $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases" -Method Post -Headers $headers -Body $body -ContentType "application/json; charset=utf-8"
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 422) {
        Write-Host "Release $tag may already exist, fetching ..." -ForegroundColor Yellow
        $release = Invoke-RestMethod -Uri "https://api.github.com/repos/$owner/$repo/releases/tags/$tag" -Method Get -Headers $headers
    } else {
        throw
    }
}

$uploadUrl = $release.upload_url -replace "\{\?name,label\}", "?name=$apkName"
Write-Host "Uploading APK ..." -ForegroundColor Cyan

$apkBytes = [System.IO.File]::ReadAllBytes($apkPath)
$uploadHeaders = @{
    "Authorization" = "token $env:GITHUB_TOKEN"
    "Accept"        = "application/vnd.github.v3+json"
    "Content-Type"  = "application/vnd.android.package-archive"
}

Invoke-RestMethod -Uri $uploadUrl -Method Post -Headers $uploadHeaders -Body $apkBytes

Write-Host "Done." -ForegroundColor Green
Write-Host $release.html_url -ForegroundColor Cyan
