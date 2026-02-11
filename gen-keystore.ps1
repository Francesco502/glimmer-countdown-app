# 在项目根目录生成 timeapk-release.keystore（仅做一次）
# 用法：在 PowerShell 中执行 .\gen-keystore.ps1 ，按提示输入密码与信息

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
$keytoolPaths = @(
    "C:\Program Files\Java\jdk-17.0.2\bin\keytool.exe",
    "$env:JAVA_HOME\bin\keytool.exe"
)
if ($javaCmd) { $keytoolPaths += (Join-Path (Split-Path $javaCmd.Source) "keytool.exe") }
$keytool = $keytoolPaths | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $keytool) {
    Write-Host "未找到 keytool。请安装 JDK 或设置 JAVA_HOME。" -ForegroundColor Red
    exit 1
}
$keystorePath = Join-Path $PSScriptRoot "timeapk-release.keystore"
Write-Host "使用: $keytool" -ForegroundColor Cyan
Write-Host "将生成: $keystorePath" -ForegroundColor Cyan
& $keytool -genkey -v -keystore $keystorePath -alias timeapk -keyalg RSA -keysize 2048 -validity 10000
