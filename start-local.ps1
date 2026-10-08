# start-local.ps1
# Starts the whole stack with the self-contained toolchain in .\.runtime
# (portable MongoDB + Maven + a project-local Maven repository).
# No system-wide Maven or MongoDB install is required.
#
# Usage:
#   .\start-local.ps1                 # mongod + backend + frontend
#   .\start-local.ps1 -SkipMongo      # reuse a MongoDB that is already running
#   .\start-local.ps1 -SkipBackend
#   .\start-local.ps1 -SkipFrontend
#   .\start-local.ps1 -Rebuild        # force a Maven rebuild of the backend jar

[CmdletBinding()]
param(
    [switch]$SkipMongo,
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

$Root    = $PSScriptRoot
$Rt      = Join-Path $Root '.runtime'
$Logs    = Join-Path $Rt 'logs'
$Mongo   = Join-Path $Rt 'mongodb\mongodb-win32-x86_64-windows-7.0.14\bin\mongod.exe'
$MongoDb = Join-Path $Rt 'mongodata'
$MavenH  = Join-Path $Rt 'maven\apache-maven-3.9.9'
$M2Repo  = Join-Path $Rt 'm2repo'
$Jar     = Join-Path $Root 'backend\target\self-discipline-backend-1.0.0.jar'
$JwtFile = Join-Path $Rt 'jwt.txt'

New-Item -ItemType Directory -Force -Path $Logs, $MongoDb | Out-Null

function Test-Port([int]$Port) {
    # Try IPv4 and IPv6 loopback: Vite binds to ::1 while Spring Boot listens on both.
    foreach ($host_ in @('127.0.0.1', '::1')) {
        $client = New-Object System.Net.Sockets.TcpClient
        try {
            $client.Connect($host_, $Port)
            return $true
        } catch {
        } finally {
            $client.Dispose()
        }
    }
    return $false
}

# JDK 21 是硬性要求。优先用 JAVA_HOME；未设置时依次回退到 PATH 上的 java.exe
# 与常见安装目录，找不到再抛出可照做的错误。
function Resolve-JavaHome {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\java.exe'
        if (Test-Path $candidate) { return $env:JAVA_HOME }
        Write-Warning "[java]     JAVA_HOME 下没有 bin\java.exe：$env:JAVA_HOME"
    }
    $onPath = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($onPath -and $onPath.Source) {
        $home = Split-Path (Split-Path $onPath.Source -Parent) -Parent
        if ($home -and (Test-Path (Join-Path $home 'bin\java.exe'))) { return $home }
    }
    $roots = @(
        (Join-Path $env:ProgramFiles 'Microsoft'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Amazon Corretto'),
        (Join-Path $env:ProgramFiles 'Zulu')
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $found = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'bin\java.exe') } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

function Get-JwtSecret {
    if (Test-Path $JwtFile) {
        $value = ((Get-Content $JwtFile -Raw).Trim() -replace '^JWT_SECRET=', '')
        if ($value.Length -ge 32) { return $value }
    }
    $bytes = New-Object byte[] 48
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    $secret = [Convert]::ToBase64String($bytes)
    "JWT_SECRET=$secret" | Set-Content -Encoding UTF8 $JwtFile
    return $secret
}

# ---------------------------------------------------------------- MongoDB
if (-not $SkipMongo) {
    if (Test-Port 27017) {
        Write-Host '[mongo]    already listening on 127.0.0.1:27017 - reusing it'
    } elseif (-not (Test-Path $Mongo)) {
        Write-Warning "[mongo]    mongod.exe not found at $Mongo - skipping"
    } else {
        Write-Host '[mongo]    starting mongod on 127.0.0.1:27017'
        Start-Process -FilePath $Mongo `
            -ArgumentList '--dbpath', "`"$MongoDb`"", '--port', '27017', '--bind_ip', '127.0.0.1' `
            -RedirectStandardOutput (Join-Path $Logs 'mongod.log') `
            -RedirectStandardError  (Join-Path $Logs 'mongod.err.log') `
            -WindowStyle Hidden
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Milliseconds 500
            if (Test-Port 27017) { break }
        }
        if (Test-Port 27017) { Write-Host '[mongo]    ready' } else { Write-Warning '[mongo]    did not come up; see .runtime\logs\mongod.log' }
    }
}

# ---------------------------------------------------------------- Backend
if (-not $SkipBackend) {
    if (Test-Port 8080) {
        Write-Host '[backend]  port 8080 already in use - assuming the backend is already running'
    } else {
        # JDK 21 是硬性要求。JAVA_HOME 缺失时给出一条能照着做的错误，
        # 而不是让 "$env:JAVA_HOME\bin\java" 抛出一句看不懂的路径错误。
        $javaHome = Resolve-JavaHome
        if (-not $javaHome) {
            throw @'
找不到可用的 JDK。请先安装 JDK 21，然后设置 JAVA_HOME，例如：
  setx JAVA_HOME "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
设置后请重开一个终端再运行本脚本。
'@
        }
        $env:JAVA_HOME = $javaHome
        $JavaExe = Join-Path $javaHome 'bin\java.exe'
        Write-Host "[backend]  JAVA_HOME = $javaHome"

        if ($Rebuild -or -not (Test-Path $Jar)) {
            if (-not (Test-Path $MavenH)) { throw "Bundled Maven not found at $MavenH" }
            Write-Host '[backend]  building jar with bundled Maven (this can take a while)'
            & $JavaExe `
                -classpath "$MavenH\boot\plexus-classworlds-2.8.0.jar" `
                "-Dclassworlds.conf=$MavenH\bin\m2.conf" `
                "-Dmaven.home=$MavenH" `
                "-Dmaven.multiModuleProjectDirectory=$Root\backend" `
                "-Dmaven.repo.local=$M2Repo" `
                org.codehaus.plexus.classworlds.launcher.Launcher `
                -B -f "$Root\backend\pom.xml" package -DskipTests
            if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
        }

        if (-not (Test-Path $Jar)) { throw "Backend jar not found at $Jar" }

        $env:JWT_SECRET  = Get-JwtSecret
        $env:MONGODB_URI = 'mongodb://127.0.0.1:27017/self_discipline'

        # Optional local secrets (AI keys) from backend\env.bat
        $envBat = Join-Path $Root 'backend\env.bat'
        if (Test-Path $envBat) {
            Get-Content $envBat | Where-Object { $_ -match '^\s*set\s+"?([A-Z_]+)=(.*?)"?\s*$' } | ForEach-Object {
                if ($_ -match '^\s*set\s+"?([A-Z_]+)=(.*?)"?\s*$') {
                    Set-Item -Path "env:$($Matches[1])" -Value $Matches[2]
                }
            }
            Write-Host '[backend]  loaded extra variables from backend\env.bat'
        }

        Write-Host '[backend]  starting Spring Boot on http://localhost:8080'
        $backendArgs = @('-jar', "`"$Jar`"", '--server.port=8080')
        $proc = Start-Process -FilePath $JavaExe -ArgumentList $backendArgs `
            -RedirectStandardOutput (Join-Path $Logs 'backend.log') `
            -RedirectStandardError  (Join-Path $Logs 'backend.err.log') `
            -WindowStyle Hidden -PassThru
        Write-Host "[backend]  pid $($proc.Id); waiting for port 8080"
        for ($i = 0; $i -lt 120; $i++) {
            Start-Sleep -Seconds 1
            if (Test-Port 8080) { break }
        }
        if (Test-Port 8080) { Write-Host '[backend]  ready' } else { Write-Warning '[backend]  did not come up; see .runtime\logs\backend.log' }
    }
}

# ---------------------------------------------------------------- Frontend
if (-not $SkipFrontend) {
    if (Test-Port 3000) {
        Write-Host '[frontend] port 3000 already in use - assuming Vite is already running'
    } else {
        Write-Host '[frontend] starting Vite on http://localhost:3000'
        $frontendLog = Join-Path $Logs 'frontend.log'
        Start-Process -FilePath 'cmd.exe' `
            -ArgumentList '/c', "npm run dev > `"$frontendLog`" 2>&1" `
            -WorkingDirectory (Join-Path $Root 'frontend') `
            -WindowStyle Hidden
        for ($i = 0; $i -lt 90; $i++) {
            Start-Sleep -Seconds 1
            if (Test-Port 3000) { break }
        }
        if (Test-Port 3000) { Write-Host '[frontend] ready' } else { Write-Warning "[frontend] did not come up; see $frontendLog" }
    }
}

Write-Host ''
Write-Host 'Open:  http://localhost:3000'
Write-Host 'API:   http://localhost:8080'
Write-Host 'Logs:  .runtime\logs\'
