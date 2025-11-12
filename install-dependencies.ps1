# Automatic Installation Script for Java 17 and MongoDB
# This script will download and install required dependencies

param(
    [switch]$SkipJava = $false,
    [switch]$SkipMongoDB = $false
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Dependency Installation Script" -ForegroundColor Cyan
Write-Host "  Installing: Java 17 + MongoDB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "[WARN] This script is not running as Administrator" -ForegroundColor Yellow
    Write-Host "       Some installations may require administrator privileges" -ForegroundColor Yellow
    Write-Host ""
    $response = Read-Host "Continue anyway? (Y/N)"
    if ($response -ne "Y" -and $response -ne "y") {
        Write-Host "Installation cancelled. Please run as Administrator." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
}

# Create temp directory
$tempDir = "$env:TEMP\selfdiscipline-install"
if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
}

Write-Host "Temporary directory: $tempDir" -ForegroundColor Gray
Write-Host ""

# ============================================
# 1. Install Java 17
# ============================================
if (-not $SkipJava) {
    Write-Host "[1/2] Installing Java 17..." -ForegroundColor Green
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    # Check if Java 17 is already installed
    $java17Installed = $false
    try {
        $javaVersion = java -version 2>&1 | Select-String "version"
        if ($javaVersion -match "17\.") {
            Write-Host "   [OK] Java 17 is already installed" -ForegroundColor Green
            java -version
            $java17Installed = $true
        }
    } catch {
        # Java not found or error
    }
    
    if (-not $java17Installed) {
        Write-Host "   Downloading OpenJDK 17..." -ForegroundColor Yellow
        
        # Using Adoptium (Eclipse Temurin) - reliable and free
        $jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi"
        $jdkInstaller = "$tempDir\OpenJDK17.msi"
        
        try {
            Write-Host "   Download URL: $jdkUrl" -ForegroundColor Gray
            Write-Host "   This may take a few minutes..." -ForegroundColor Yellow
            
            # Download with progress
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkInstaller -UseBasicParsing
            $ProgressPreference = 'Continue'
            
            Write-Host "   [OK] Download completed" -ForegroundColor Green
            Write-Host "   File size: $([math]::Round((Get-Item $jdkInstaller).Length / 1MB, 2)) MB" -ForegroundColor Gray
            
            # Install Java 17
            Write-Host ""
            Write-Host "   Installing Java 17..." -ForegroundColor Yellow
            Write-Host "   Please wait, this may take a few minutes..." -ForegroundColor Yellow
            
            $installArgs = "/i `"$jdkInstaller`" /quiet /norestart ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJarFileRunWith,FeatureJavaHome"
            Start-Process msiexec.exe -ArgumentList $installArgs -Wait -NoNewWindow
            
            Write-Host "   [OK] Java 17 installation completed" -ForegroundColor Green
            
            # Refresh environment variables
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            
            # Verify installation
            Start-Sleep -Seconds 2
            Write-Host ""
            Write-Host "   Verifying Java installation..." -ForegroundColor Yellow
            
            # Try to find Java 17
            $javaHome = [System.Environment]::GetEnvironmentVariable("JAVA_HOME","Machine")
            if ($javaHome) {
                Write-Host "   JAVA_HOME: $javaHome" -ForegroundColor Gray
            }
            
            try {
                $javaVersionCheck = & "java" -version 2>&1 | Out-String
                Write-Host "   Java version:" -ForegroundColor Gray
                Write-Host $javaVersionCheck -ForegroundColor Gray
                
                if ($javaVersionCheck -match "17\.") {
                    Write-Host "   [OK] Java 17 verified successfully!" -ForegroundColor Green
                } else {
                    Write-Host "   [WARN] Java installed but version might not be 17" -ForegroundColor Yellow
                    Write-Host "   You may need to set JAVA_HOME manually" -ForegroundColor Yellow
                }
            } catch {
                Write-Host "   [WARN] Could not verify Java installation" -ForegroundColor Yellow
                Write-Host "   You may need to restart your terminal or computer" -ForegroundColor Yellow
            }
            
        } catch {
            Write-Host "   [ERROR] Failed to download or install Java 17" -ForegroundColor Red
            Write-Host "   Error: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "   Please install Java 17 manually from:" -ForegroundColor Yellow
            Write-Host "   https://adoptium.net/temurin/releases/?version=17" -ForegroundColor Cyan
        }
    }
    
    Write-Host ""
}

# ============================================
# 2. Install MongoDB
# ============================================
if (-not $SkipMongoDB) {
    Write-Host "[2/2] Installing MongoDB..." -ForegroundColor Green
    Write-Host "----------------------------------------" -ForegroundColor Gray
    
    # Check if MongoDB is already installed
    $mongoInstalled = $false
    try {
        $mongoVersion = mongod --version 2>&1 | Select-String "version"
        if ($mongoVersion) {
            Write-Host "   [OK] MongoDB is already installed" -ForegroundColor Green
            mongod --version | Select-Object -First 1
            $mongoInstalled = $true
        }
    } catch {
        # MongoDB not found
    }
    
    # Also check for MongoDB service
    $mongoService = Get-Service -Name "MongoDB" -ErrorAction SilentlyContinue
    if ($mongoService) {
        Write-Host "   [OK] MongoDB service found" -ForegroundColor Green
        $mongoInstalled = $true
    }
    
    if (-not $mongoInstalled) {
        Write-Host "   Downloading MongoDB Community Edition..." -ForegroundColor Yellow
        
        # MongoDB 7.0 Community Edition for Windows
        $mongoUrl = "https://fastdl.mongodb.org/windows/mongodb-windows-x86_64-7.0.4-signed.msi"
        $mongoInstaller = "$tempDir\MongoDB.msi"
        
        try {
            Write-Host "   Download URL: $mongoUrl" -ForegroundColor Gray
            Write-Host "   This may take several minutes (file is ~300MB)..." -ForegroundColor Yellow
            
            # Download with progress
            $ProgressPreference = 'SilentlyContinue'
            Invoke-WebRequest -Uri $mongoUrl -OutFile $mongoInstaller -UseBasicParsing
            $ProgressPreference = 'Continue'
            
            Write-Host "   [OK] Download completed" -ForegroundColor Green
            Write-Host "   File size: $([math]::Round((Get-Item $mongoInstaller).Length / 1MB, 2)) MB" -ForegroundColor Gray
            
            # Install MongoDB
            Write-Host ""
            Write-Host "   Installing MongoDB..." -ForegroundColor Yellow
            Write-Host "   Please wait, this may take several minutes..." -ForegroundColor Yellow
            Write-Host "   The installer may show a GUI window - please follow the prompts" -ForegroundColor Yellow
            
            # Install with GUI (silent install often fails for MongoDB)
            Start-Process msiexec.exe -ArgumentList "/i `"$mongoInstaller`"" -Wait
            
            Write-Host "   [OK] MongoDB installation completed" -ForegroundColor Green
            
            # Refresh environment variables
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
            
            # Verify installation
            Start-Sleep -Seconds 2
            Write-Host ""
            Write-Host "   Verifying MongoDB installation..." -ForegroundColor Yellow
            
            try {
                $mongoVersionCheck = & "mongod" --version 2>&1 | Select-Object -First 1
                Write-Host "   MongoDB version: $mongoVersionCheck" -ForegroundColor Gray
                Write-Host "   [OK] MongoDB verified successfully!" -ForegroundColor Green
            } catch {
                Write-Host "   [WARN] Could not verify MongoDB installation" -ForegroundColor Yellow
                Write-Host "   You may need to restart your terminal or computer" -ForegroundColor Yellow
            }
            
            # Check for MongoDB service
            $mongoService = Get-Service -Name "MongoDB" -ErrorAction SilentlyContinue
            if ($mongoService) {
                Write-Host "   [OK] MongoDB service installed" -ForegroundColor Green
                Write-Host "   Service status: $($mongoService.Status)" -ForegroundColor Gray
                
                if ($mongoService.Status -ne "Running") {
                    Write-Host "   Starting MongoDB service..." -ForegroundColor Yellow
                    Start-Service -Name "MongoDB"
                    Write-Host "   [OK] MongoDB service started" -ForegroundColor Green
                }
            } else {
                Write-Host "   [INFO] MongoDB service not found" -ForegroundColor Yellow
                Write-Host "   You may need to start MongoDB manually with: mongod" -ForegroundColor Yellow
            }
            
        } catch {
            Write-Host "   [ERROR] Failed to download or install MongoDB" -ForegroundColor Red
            Write-Host "   Error: $_" -ForegroundColor Red
            Write-Host ""
            Write-Host "   Please install MongoDB manually from:" -ForegroundColor Yellow
            Write-Host "   https://www.mongodb.com/try/download/community" -ForegroundColor Cyan
        }
    }
    
    Write-Host ""
}

# ============================================
# Summary
# ============================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Installation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "Java 17:" -ForegroundColor White
try {
    $javaVer = java -version 2>&1 | Select-String "version" | Out-String
    if ($javaVer -match "17\.") {
        Write-Host "   [OK] Java 17 is installed and active" -ForegroundColor Green
    } else {
        Write-Host "   [WARN] Java is installed but may not be version 17" -ForegroundColor Yellow
        Write-Host "   Current version: $javaVer" -ForegroundColor Gray
    }
} catch {
    Write-Host "   [X] Java not found in PATH" -ForegroundColor Red
    Write-Host "   Please restart your terminal or set JAVA_HOME manually" -ForegroundColor Yellow
}

Write-Host ""

# Check MongoDB
Write-Host "MongoDB:" -ForegroundColor White
$mongoOk = $false
try {
    $mongoVer = mongod --version 2>&1 | Select-Object -First 1
    Write-Host "   [OK] MongoDB is installed" -ForegroundColor Green
    Write-Host "   Version: $mongoVer" -ForegroundColor Gray
    $mongoOk = $true
} catch {
    Write-Host "   [X] MongoDB not found in PATH" -ForegroundColor Red
}

$mongoService = Get-Service -Name "MongoDB" -ErrorAction SilentlyContinue
if ($mongoService) {
    if ($mongoService.Status -eq "Running") {
        Write-Host "   [OK] MongoDB service is running" -ForegroundColor Green
        $mongoOk = $true
    } else {
        Write-Host "   [WARN] MongoDB service exists but is not running" -ForegroundColor Yellow
        Write-Host "   Start it with: Start-Service MongoDB" -ForegroundColor Yellow
    }
}

if (-not $mongoOk) {
    Write-Host "   Please restart your terminal or start MongoDB manually" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Cleanup
Write-Host "Cleaning up temporary files..." -ForegroundColor Gray
try {
    Remove-Item -Path $tempDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "[OK] Cleanup completed" -ForegroundColor Green
} catch {
    Write-Host "[WARN] Could not clean up temp directory: $tempDir" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Close and reopen your terminal to refresh environment variables" -ForegroundColor White
Write-Host "2. Verify installations:" -ForegroundColor White
Write-Host "   java -version    # Should show Java 17" -ForegroundColor Gray
Write-Host "   mongod --version # Should show MongoDB version" -ForegroundColor Gray
Write-Host "3. Start the backend:" -ForegroundColor White
Write-Host "   cd backend" -ForegroundColor Gray
Write-Host "   mvn spring-boot:run" -ForegroundColor Gray
Write-Host ""

