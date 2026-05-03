<#
  N11 E-Commerce Marketplace - Tek Komutla Tum Servisleri Baslat
  
  Kullanim:
    .\start-all.ps1          -> Tum servisleri baslatir
    .\start-all.ps1 -Stop    -> Tum servisleri durdurur
#>

param(
    [switch]$Stop
)

$ErrorActionPreference = "Continue"
$ROOT = Split-Path -Parent $PSScriptRoot
$BACKEND = Join-Path $ROOT "backend"

# Servis tanimlari: [isim, modul, port]
$INFRA_SERVICES = @(
    @("Config Server",    "config-server",    8888),
    @("Discovery Server", "discovery-server", 8761)
)

$APP_SERVICES = @(
    @("API Gateway",           "api-gateway",           8000),
    @("Product Service",       "product-service",       8082),
    @("Basket Service",        "basket-service",        8083),
    @("Order Service",         "order-service",         8084),
    @("Payment Service",       "payment-service",       8085),
    @("Inventory Service",     "inventory-service",     8086),
    @("Notification Service",  "notification-service",  8087)
)

# Opsiyonel servisler (AI API key vs. gerektirebilir)
$OPTIONAL_SERVICES = @(
    ,@("MCP AI Server",         "mcp-ai-server",         8090)
)

$ALL_SERVICES = $INFRA_SERVICES + $APP_SERVICES + $OPTIONAL_SERVICES

# --- STOP MODU ---
if ($Stop) {
    Write-Host ""
    Write-Host "DURDURMA ISLEMI BASLIYOR..." -ForegroundColor Yellow
    foreach ($svc in $ALL_SERVICES) {
        $port = $svc[2]
        $proc = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | 
                Select-Object -ExpandProperty OwningProcess -First 1
        if ($proc) {
            Stop-Process -Id $proc -Force -ErrorAction SilentlyContinue
            Write-Host "  [OK] $($svc[0]) (port $port) durduruldu" -ForegroundColor Green
        } else {
            Write-Host "  [--] $($svc[0]) (port $port) zaten kapali" -ForegroundColor DarkGray
        }
    }
    Write-Host ""
    Write-Host "Tum servisler durduruldu!" -ForegroundColor Green
    Write-Host ""
    exit 0
}

# --- YARDIMCI FONKSIYONLAR ---

function Wait-ForPort {
    param([int]$Port, [int]$TimeoutSeconds = 60)
    $elapsed = 0
    while ($elapsed -lt $TimeoutSeconds) {
        $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
        if ($conn) { return $true }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "." -NoNewline -ForegroundColor DarkGray
    }
    return $false
}

function Start-SpringService {
    param([string]$Name, [string]$Module, [int]$Port)
    
    # Zaten calisiyorsa atla
    $existing = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host "  [SKIP] $Name (port $Port) zaten calisiyor" -ForegroundColor DarkGray
        return
    }

    $jarPattern = Join-Path $BACKEND "$Module\target\*.jar"
    $jar = Get-ChildItem $jarPattern -ErrorAction SilentlyContinue | 
           Where-Object { $_.Name -notmatch "original" } | 
           Select-Object -First 1
    
    if ($jar) {
        Start-Process -FilePath "java" -ArgumentList "-jar", $jar.FullName `
            -WindowStyle Minimized -PassThru | Out-Null
        Write-Host "  [START] $Name (port $Port) -> JAR ile baslatildi" -ForegroundColor Cyan
    } else {
        Write-Host "  [HATA] $Name icin JAR bulunamadi! Once 'mvn clean package -DskipTests' calistirin." -ForegroundColor Red
    }
}

# --- ANA AKIS ---

Write-Host ""
Write-Host "======================================================" -ForegroundColor Magenta
Write-Host "     N11 E-Commerce Marketplace Launcher               " -ForegroundColor Magenta
Write-Host "======================================================" -ForegroundColor Magenta
Write-Host ""

# 1. Docker Compose kontrolu
Write-Host "[1/4] Docker altyapisi kontrol ediliyor..." -ForegroundColor Yellow
$dockerNames = docker ps --format "{{.Names}}" 2>&1
if ($dockerNames -notmatch "postgres-product") {
    Write-Host "  Docker container'lari bulunamadi. Baslatiliyor..." -ForegroundColor Yellow
    $infraDir = Join-Path $ROOT "infra"
    Start-Process -FilePath "docker" -ArgumentList "compose", "up", "-d" `
        -WorkingDirectory $infraDir -Wait -NoNewWindow
    Write-Host "  [OK] Docker altyapisi baslatildi" -ForegroundColor Green
    Write-Host "  Servislerin hazir olmasi bekleniyor (15sn)..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 15
} else {
    Write-Host "  [OK] Docker altyapisi zaten calisiyor" -ForegroundColor Green
}

# 2. Altyapi servisleri (sirayla, biri bitmeden digeri baslamaz)
Write-Host ""
Write-Host "[2/4] Altyapi servisleri baslatiliyor (sirayla)..." -ForegroundColor Yellow

foreach ($svc in $INFRA_SERVICES) {
    Start-SpringService -Name $svc[0] -Module $svc[1] -Port $svc[2]
    Write-Host "  Bekleniyor: $($svc[0])" -NoNewline -ForegroundColor DarkGray
    $ready = Wait-ForPort -Port $svc[2] -TimeoutSeconds 60
    if ($ready) {
        Write-Host " [OK]" -ForegroundColor Green
    } else {
        Write-Host " [HATA] Zaman asimi!" -ForegroundColor Red
        exit 1
    }
}

# 3. Uygulama servisleri (hepsi ayni anda paralel)
Write-Host ""
Write-Host "[3/4] Uygulama servisleri baslatiliyor (paralel)..." -ForegroundColor Yellow

foreach ($svc in $APP_SERVICES) {
    Start-SpringService -Name $svc[0] -Module $svc[1] -Port $svc[2]
}

# Opsiyonel servisleri de dene
foreach ($svc in $OPTIONAL_SERVICES) {
    Start-SpringService -Name $svc[0] -Module $svc[1] -Port $svc[2]
}

# 4. Zorunlu servislerin hazir olmasini bekle
Write-Host ""
Write-Host "[4/4] Servisler kontrol ediliyor..." -ForegroundColor DarkGray
$allReady = $true
foreach ($svc in $APP_SERVICES) {
    Write-Host "  Bekleniyor: $($svc[0]) (port $($svc[2]))" -NoNewline -ForegroundColor DarkGray
    $ready = Wait-ForPort -Port $svc[2] -TimeoutSeconds 60
    if ($ready) {
        Write-Host " [OK]" -ForegroundColor Green
    } else {
        Write-Host " [HATA]" -ForegroundColor Red
        $allReady = $false
    }
}

# Opsiyonel servisleri kontrol et (hata vermez)
foreach ($svc in $OPTIONAL_SERVICES) {
    Write-Host "  Bekleniyor: $($svc[0]) (port $($svc[2]))" -NoNewline -ForegroundColor DarkGray
    $ready = Wait-ForPort -Port $svc[2] -TimeoutSeconds 15
    if ($ready) {
        Write-Host " [OK]" -ForegroundColor Green
    } else {
        Write-Host " [SKIP - opsiyonel]" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host ""

if ($allReady) {
    Write-Host "======================================================" -ForegroundColor Green
    Write-Host "          TUM SERVISLER HAZIR!                         " -ForegroundColor Green
    Write-Host "======================================================" -ForegroundColor Green
    Write-Host "  Frontend:    http://localhost:5173" -ForegroundColor White
    Write-Host "  Keycloak:    http://localhost:8180" -ForegroundColor White
    Write-Host "  Eureka:      http://localhost:8761" -ForegroundColor White
    Write-Host "  RabbitMQ:    http://localhost:15672" -ForegroundColor White
    Write-Host "  Swagger:     http://localhost:8082/swagger-ui.html" -ForegroundColor White
    Write-Host "------------------------------------------------------" -ForegroundColor Green
    Write-Host "  Durdurmak icin:  .\start-all.ps1 -Stop" -ForegroundColor DarkGray
    Write-Host "======================================================" -ForegroundColor Green
} else {
    Write-Host "Bazi servisler baslatila madi. Loglari kontrol edin." -ForegroundColor Yellow
}

Write-Host ""
