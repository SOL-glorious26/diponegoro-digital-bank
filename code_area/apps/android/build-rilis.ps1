# Membangun APK rilis bertanda tangan (varian demo dan online) dan menyalinnya ke folder rilis\.
#
#   .\build-rilis.ps1                                   -> online untuk emulator (http://10.0.2.2:8080/)
#   .\build-rilis.ps1 -BaseUrl http://192.168.1.10:8080/ -> online untuk HP di Wi-Fi yang sama dengan laptop
#   .\build-rilis.ps1 -BaseUrl https://xxxx.ngrok-free.app/
#
# Butuh keystore.properties + keystore\ddb-release.jks (lihat README bagian "APK rilis").
param(
    [string]$BaseUrl = "http://10.0.2.2:8080/"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Test-Path "keystore.properties")) {
    throw "keystore.properties tidak ditemukan: APK rilis tidak dapat ditandatangani. Lihat README bagian 'APK rilis'."
}
if (-not $BaseUrl.EndsWith("/")) { $BaseUrl = "$BaseUrl/" }
if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}

Write-Host "Membangun APK rilis (online -> $BaseUrl)..." -ForegroundColor Cyan
& .\gradlew.bat :app:assembleDemoRelease :app:assembleOnlineRelease "-Pddb.baseUrl=$BaseUrl" --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "Build gagal" }

$version = (Select-String -Path "app\build.gradle.kts" -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value
$hostTag = ([uri]$BaseUrl).Host -replace '[^A-Za-z0-9.-]', '_'
New-Item -ItemType Directory -Force rilis | Out-Null
$demo = "rilis\DDB-$version-demo.apk"
$online = "rilis\DDB-$version-online-$hostTag.apk"
Copy-Item "app\build\outputs\apk\demo\release\app-demo-release.apk" $demo -Force
Copy-Item "app\build\outputs\apk\online\release\app-online-release.apk" $online -Force

$bt = Get-ChildItem "$env:LOCALAPPDATA\Android\Sdk\build-tools" -Directory | Sort-Object Name | Select-Object -Last 1
# apksigner menulis peringatan JVM ke stderr; yang dinilai hanya kode keluarnya.
$ErrorActionPreference = "Continue"
foreach ($apk in @($demo, $online)) {
    & "$($bt.FullName)\apksigner.bat" verify $apk 2>$null
    if ($LASTEXITCODE -ne 0) { throw "Tanda tangan $apk tidak valid" }
    $hash = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
    Write-Host ("{0}  {1:N1} MB  sha256 {2}" -f $apk, ((Get-Item $apk).Length / 1MB), $hash) -ForegroundColor Green
}
Write-Host "Pasang di HP: adb install -r $online   (copot dulu versi debug bila ada: adb uninstall com.diponegoro.digitalbank)" -ForegroundColor Yellow
