param(
    [ValidateSet("auto", "mysql", "h2")]
    [string]$Database = "auto",

    [string]$DbHost = "localhost",
    [int]$DbPort = 3306,
    [string]$DbName = "mlops_platform",
    [string]$DbUser = "root",
    [string]$DbPassword = "123456",

    [switch]$SkipInstall,
    [switch]$NoBuild,
    [switch]$PrepareOnly,
    [switch]$HiddenWindows
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Split-Path -Parent $ScriptDir
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$RunDir = Join-Path $RootDir ".run"
$OutputRoot = Join-Path $RootDir "output"
$PythonVenv = Join-Path $RootDir ".venv"
$VenvPython = Join-Path $PythonVenv "Scripts\python.exe"
$MockScript = Join-Path $RootDir "scripts\mock_process.py"
$Requirements = Join-Path $RootDir "scripts\requirements.txt"
$InitSql = Join-Path $RootDir "database\init.sql"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Resolve-Tool {
    param(
        [string[]]$Names,
        [string[]]$Fallbacks = @(),
        [switch]$Required
    )

    foreach ($name in $Names) {
        $command = Get-Command $name -ErrorAction SilentlyContinue
        if ($command) {
            return $command.Source
        }
    }

    foreach ($path in $Fallbacks) {
        if ($path -and (Test-Path $path)) {
            return $path
        }
    }

    if ($Required) {
        throw "Required tool not found: $($Names -join ', ')"
    }
    return $null
}

function Invoke-Native {
    param(
        [string]$FilePath,
        [string[]]$Arguments = @()
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: $FilePath $($Arguments -join ' ')"
    }
}

function Test-MysqlConnection {
    param([string]$MysqlPath)
    if (-not $MysqlPath) {
        return $false
    }

    $args = @("-h$DbHost", "-P$DbPort", "-u$DbUser")
    if ($DbPassword.Length -gt 0) {
        $args += "-p$DbPassword"
    }
    $args += @("-e", "SELECT 1")

    try {
        & $MysqlPath @args *> $null
        return $LASTEXITCODE -eq 0
    } catch {
        return $false
    }
}

function Initialize-MysqlDatabase {
    param([string]$MysqlPath)

    Write-Step "Initializing MySQL database $DbName"
    $args = @("-h$DbHost", "-P$DbPort", "-u$DbUser", "--default-character-set=utf8mb4")
    if ($DbPassword.Length -gt 0) {
        $args += "-p$DbPassword"
    }

    Get-Content -Raw $InitSql | & $MysqlPath @args
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL initialization failed. Check DbHost/DbPort/DbUser/DbPassword."
    }
}

function Escape-SingleQuoted {
    param([string]$Value)
    return $Value.Replace("'", "''")
}

Write-Host "MLOps platform bootstrap" -ForegroundColor Green
Write-Host "Repository: $RootDir"

$python = Resolve-Tool -Names @("python", "python.exe") -Required
$node = Resolve-Tool -Names @("node", "node.exe") -Required
$npm = Resolve-Tool -Names @("npm.cmd", "npm", "npm.ps1") -Required
$mavenFallbacks = @()
if ($env:USERPROFILE) {
    $mavenFallbacks += Join-Path $env:USERPROFILE "scoop\apps\maven\current\bin\mvn.cmd"
}
if ($env:MAVEN_HOME) {
    $mavenFallbacks += Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
}

$maven = Resolve-Tool `
    -Names @("mvn", "mvn.cmd") `
    -Fallbacks $mavenFallbacks `
    -Required

Write-Step "Toolchain"
Write-Host "Python: $python"
Write-Host "Node:   $node"
Write-Host "npm:    $npm"
Write-Host "Maven:  $maven"

New-Item -ItemType Directory -Force -Path $RunDir, $OutputRoot | Out-Null

if (-not $SkipInstall) {
    if (-not (Test-Path $VenvPython)) {
        Write-Step "Creating Python virtual environment"
        Invoke-Native $python @("-m", "venv", $PythonVenv)
    }

    Write-Step "Installing Python dependencies"
    Invoke-Native $VenvPython @("-m", "pip", "install", "-r", $Requirements)

    Write-Step "Installing frontend dependencies"
    Push-Location $FrontendDir
    try {
        Invoke-Native $npm @("install", "--no-audit", "--no-fund")
    } finally {
        Pop-Location
    }
} else {
    Write-Step "Skipping dependency installation"
}

Write-Step "Running Python mock smoke test"
Invoke-Native $VenvPython @($MockScript, "--task_id", "local-smoke", "--target_date", "2026-05-19", "--mask_range=-1,1", "--output_root", $OutputRoot)

if (-not $NoBuild) {
    Write-Step "Building backend"
    Push-Location $BackendDir
    try {
        Invoke-Native $maven @("-DskipTests", "package")
    } finally {
        Pop-Location
    }

    Write-Step "Building frontend"
    Push-Location $FrontendDir
    try {
        Invoke-Native $npm @("run", "build")
    } finally {
        Pop-Location
    }
}

$mysql = Resolve-Tool -Names @("mysql", "mysql.exe")
$selectedDatabase = $Database

if ($Database -eq "auto") {
    if (Test-MysqlConnection -MysqlPath $mysql) {
        $selectedDatabase = "mysql"
    } else {
        $selectedDatabase = "h2"
        Write-Host ""
        Write-Host "MySQL is not reachable. Falling back to embedded H2 demo database." -ForegroundColor Yellow
    }
}

if ($selectedDatabase -eq "mysql") {
    if (-not (Test-MysqlConnection -MysqlPath $mysql)) {
        throw "MySQL is required but not reachable. Start MySQL or run with -Database h2 for a zero-config demo."
    }
    Initialize-MysqlDatabase -MysqlPath $mysql
}

if ($PrepareOnly) {
    Write-Step "Preparation completed"
    Write-Host "Backend and frontend were not started because -PrepareOnly was used."
    exit 0
}

$backendRunner = Join-Path $RunDir "run-backend.ps1"
$frontendRunner = Join-Path $RunDir "run-frontend.ps1"

$springProfile = ""
$dbUrl = "jdbc:mysql://$DbHost`:$DbPort/$DbName`?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
if ($selectedDatabase -eq "h2") {
    $springProfile = "demo"
}

$backendContent = @"
`$ErrorActionPreference = 'Stop'
Set-Location '$(Escape-SingleQuoted $BackendDir)'
`$env:MLOPS_PYTHON = '$(Escape-SingleQuoted $VenvPython)'
`$env:MLOPS_SCRIPT_PATH = '$(Escape-SingleQuoted $MockScript)'
`$env:MLOPS_OUTPUT_ROOT = '$(Escape-SingleQuoted $OutputRoot)'
`$env:DB_URL = '$(Escape-SingleQuoted $dbUrl)'
`$env:DB_USERNAME = '$(Escape-SingleQuoted $DbUser)'
`$env:DB_PASSWORD = '$(Escape-SingleQuoted $DbPassword)'
if ('$(Escape-SingleQuoted $springProfile)'.Length -gt 0) {
    `$env:SPRING_PROFILES_ACTIVE = '$(Escape-SingleQuoted $springProfile)'
}
& '$(Escape-SingleQuoted $maven)' spring-boot:run
"@

$frontendContent = @"
`$ErrorActionPreference = 'Stop'
Set-Location '$(Escape-SingleQuoted $FrontendDir)'
& '$(Escape-SingleQuoted $npm)' run dev
"@

Set-Content -Path $backendRunner -Value $backendContent -Encoding UTF8
Set-Content -Path $frontendRunner -Value $frontendContent -Encoding UTF8

Write-Step "Starting services"
$windowStyle = if ($HiddenWindows) { "Hidden" } else { "Normal" }
Start-Process powershell.exe -WindowStyle $windowStyle -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-File", $backendRunner)
Start-Sleep -Seconds 4
Start-Process powershell.exe -WindowStyle $windowStyle -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-File", $frontendRunner)

Write-Host ""
Write-Host "Started with database mode: $selectedDatabase" -ForegroundColor Green
Write-Host "Frontend: http://localhost:5173"
Write-Host "Backend:  http://localhost:8080"
Write-Host ""
Write-Host "If ports are occupied, close the old process and run this script again."
