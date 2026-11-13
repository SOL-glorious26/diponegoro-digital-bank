# Menjalankan backend di laptop tanpa Docker.
#
#   .\run-local.ps1            -> PostgreSQL lokal (butuh database "ddb", lihat README bagian "Siapkan PostgreSQL")
#   .\run-local.ps1 -H2        -> database file H2 di .\data (cepat, tanpa PostgreSQL; hanya untuk mencoba)
#
# Setelah jalan: http://localhost:8080/swagger-ui.html
# Dari HP di Wi-Fi yang sama: http://<IP-laptop>:8080  (lihat IP dengan: ipconfig)
param(
    [switch]$H2,
    [string]$DbPassword = $env:DB_PASSWORD
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $jbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $jbr) { $env:JAVA_HOME = $jbr }
}

if ($H2) {
    $profile = "h2"
} else {
    $profile = "dev"
    if (-not $DbPassword) {
        $DbPassword = Read-Host "Password user database 'ddb'"
    }
    $env:DB_URL = "jdbc:postgresql://localhost:5432/ddb"
    $env:DB_USER = "ddb"
    $env:DB_PASSWORD = $DbPassword
}

Write-Host "Membangun backend..." -ForegroundColor Cyan
& .\mvnw.cmd -q -B package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Build gagal" }

$ip = (Get-NetIPAddress -AddressFamily IPv4 -PrefixOrigin Dhcp -ErrorAction SilentlyContinue | Select-Object -First 1).IPAddress
Write-Host "Backend jalan di http://localhost:8080  (HP di Wi-Fi yang sama: http://$ip`:8080)" -ForegroundColor Green
Write-Host "Akun demo: demo / Demo@1234, PIN 123456. Tekan Ctrl+C untuk berhenti." -ForegroundColor Green
$jar = Get-ChildItem target\ddb-backend-*.jar | Select-Object -First 1
& "$env:JAVA_HOME\bin\java.exe" -jar $jar.FullName --spring.profiles.active=$profile
