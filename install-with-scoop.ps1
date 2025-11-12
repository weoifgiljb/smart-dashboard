# Quick Installation Script using Scoop Package Manager
# This is faster and more reliable than downloading installers manually

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Quick Install with Scoop" -ForegroundColor Cyan
Write-Host "  Installing: Java 17 + MongoDB" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Scoop is installed
$scoopInstalled = $false
try {
    $scoopVersion = scoop --version
    if ($scoopVersion) {
        Write-Host "[OK] Scoop is already installed" -ForegroundColor Green
        Write-Host "    Version: $scoopVersion" -ForegroundColor Gray
        $scoopInstalled = $true
    }
} catch {
    Write-Host "[INFO] Scoop is not installed" -ForegroundColor Yellow
}

Write-Host ""

# Install Scoop if not present
if (-not $scoopInstalled) {
    Write-Host "Installing Scoop package manager..." -ForegroundColor Yellow
    Write-Host "This will make installing Java and MongoDB much easier!" -ForegroundColor Yellow
    Write-Host ""
    
    try {
        # Set execution policy for current user
        Set-ExecutionPolicy RemoteSigned -Scope CurrentUser -Force
        
        # Install Scoop
        Write-Host "Downloading and installing Scoop..." -ForegroundColor Yellow
        Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression
        
        Write-Host "[OK] Scoop installed successfully!" -ForegroundColor Green
        Write-Host ""
        
    } catch {
        Write-Host "[ERROR] Failed to install Scoop" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install Scoop manually:" -ForegroundColor Yellow
        Write-Host "1. Open PowerShell" -ForegroundColor White
        Write-Host "2. Run: Set-ExecutionPolicy RemoteSigned -Scope CurrentUser" -ForegroundColor Gray
        Write-Host "3. Run: irm get.scoop.sh | iex" -ForegroundColor Gray
        Write-Host ""
        exit 1
    }
}

# Install Java 17
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[1/2] Installing Java 17 with Scoop..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Java 17 is already installed
$java17Installed = $false
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    if ($javaVersion -match "17\.") {
        Write-Host "[OK] Java 17 is already installed" -ForegroundColor Green
        java -version
        $java17Installed = $true
    }
} catch {
    # Java not found
}

if (-not $java17Installed) {
    try {
        # Add java bucket if not already added
        Write-Host "Adding Java bucket to Scoop..." -ForegroundColor Yellow
        scoop bucket add java 2>&1 | Out-Null
        
        Write-Host "Installing OpenJDK 17..." -ForegroundColor Yellow
        Write-Host "This may take a few minutes..." -ForegroundColor Yellow
        Write-Host ""
        
        scoop install openjdk17
        
        Write-Host ""
        Write-Host "[OK] Java 17 installed successfully!" -ForegroundColor Green
        
        # Verify
        Write-Host ""
        Write-Host "Verifying Java installation..." -ForegroundColor Yellow
        java -version
        
    } catch {
        Write-Host "[ERROR] Failed to install Java 17" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "You can try manually:" -ForegroundColor Yellow
        Write-Host "  scoop bucket add java" -ForegroundColor Gray
        Write-Host "  scoop install openjdk17" -ForegroundColor Gray
    }
}

Write-Host ""

# Install MongoDB
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "[2/2] Installing MongoDB with Scoop..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if MongoDB is already installed
$mongoInstalled = $false
try {
    $mongoVersion = mongod --version 2>&1 | Select-String "version"
    if ($mongoVersion) {
        Write-Host "[OK] MongoDB is already installed" -ForegroundColor Green
        mongod --version | Select-Object -First 1
        $mongoInstalled = $true
    }
} catch {
    # MongoDB not found
}

if (-not $mongoInstalled) {
    try {
        Write-Host "Installing MongoDB..." -ForegroundColor Yellow
        Write-Host "This may take a few minutes..." -ForegroundColor Yellow
        Write-Host ""
        
        scoop install mongodb
        
        Write-Host ""
        Write-Host "[OK] MongoDB installed successfully!" -ForegroundColor Green
        
        # Verify
        Write-Host ""
        Write-Host "Verifying MongoDB installation..." -ForegroundColor Yellow
        mongod --version | Select-Object -First 1
        
    } catch {
        Write-Host "[ERROR] Failed to install MongoDB" -ForegroundColor Red
        Write-Host "Error: $_" -ForegroundColor Red
        Write-Host ""
        Write-Host "You can try manually:" -ForegroundColor Yellow
        Write-Host "  scoop install mongodb" -ForegroundColor Gray
    }
}

Write-Host ""

# Summary
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Installation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "Java 17:" -ForegroundColor White
try {
    $javaVer = java -version 2>&1 | Out-String
    if ($javaVer -match "17\.") {
        Write-Host "   [OK] Java 17 is installed and active" -ForegroundColor Green
        Write-Host ""
        java -version
    } else {
        Write-Host "   [WARN] Java is installed but may not be version 17" -ForegroundColor Yellow
        Write-Host "   Current version:" -ForegroundColor Gray
        java -version
    }
} catch {
    Write-Host "   [X] Java not found" -ForegroundColor Red
}

Write-Host ""

# Check MongoDB
Write-Host "MongoDB:" -ForegroundColor White
try {
    $mongoVer = mongod --version 2>&1 | Select-Object -First 1
    Write-Host "   [OK] MongoDB is installed" -ForegroundColor Green
    Write-Host "   $mongoVer" -ForegroundColor Gray
} catch {
    Write-Host "   [X] MongoDB not found" -ForegroundColor Red
}

Write-Host ""

# Instructions for starting MongoDB
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Next Steps" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. Start MongoDB:" -ForegroundColor White
Write-Host "   Open a new terminal and run:" -ForegroundColor Gray
Write-Host "   mongod --dbpath C:\Users\$env:USERNAME\scoop\apps\mongodb\current\data" -ForegroundColor Cyan
Write-Host ""
Write-Host "   Or create a data directory and start:" -ForegroundColor Gray
Write-Host "   mkdir C:\data\db" -ForegroundColor Cyan
Write-Host "   mongod" -ForegroundColor Cyan
Write-Host ""

Write-Host "2. Start the backend:" -ForegroundColor White
Write-Host "   cd backend" -ForegroundColor Cyan
Write-Host "   mvn spring-boot:run" -ForegroundColor Cyan
Write-Host ""

Write-Host "3. Access the application:" -ForegroundColor White
Write-Host "   Frontend: http://localhost:3000" -ForegroundColor Cyan
Write-Host "   Backend:  http://localhost:8080" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Installation completed!" -ForegroundColor Green
Write-Host "If you encounter any issues, check MANUAL_INSTALL_GUIDE.md" -ForegroundColor Yellow
Write-Host ""

