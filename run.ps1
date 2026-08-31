# Build (if needed) and launch the Maze Game. Re-run this as many times as you like.
#   PS>  ./run.ps1            # normal run
#   PS>  ./run.ps1 -Clean     # force a fresh compile first
param([switch]$Clean)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Find-Exe([string[]]$candidates, [string]$onPath) {
    $cmd = Get-Command $onPath -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($c in $candidates) { if (Test-Path $c) { return $c } }
    return $null
}

# --- locate a JDK ---------------------------------------------------------
$javaCandidates = @()
if ($env:JAVA_HOME) { $javaCandidates += "$env:JAVA_HOME\bin\java.exe" }
$javaCandidates += (Get-ChildItem "C:\Program Files\JetBrains\*\jbr\bin\java.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName)
$javaCandidates += (Get-ChildItem "C:\Program Files\Java\*\bin\java.exe" -ErrorAction SilentlyContinue | ForEach-Object FullName)
$java = Find-Exe $javaCandidates "java"
if (-not $java) { throw "Could not find java.exe. Set JAVA_HOME or add java to PATH." }

# --- locate Maven (bundled with IntelliJ is fine) -----------------------
$mvnCandidates = (Get-ChildItem "C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin\mvn.cmd" -ErrorAction SilentlyContinue | ForEach-Object FullName)
$mvn = Find-Exe $mvnCandidates "mvn"
if (-not $mvn) { throw "Could not find Maven. Install it or open this project once in IntelliJ." }

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = Split-Path (Split-Path $java) }

# --- compile + cache the dependency classpath --------------------------
$cpFile = "target\.run-classpath.txt"
if ($Clean -or -not (Test-Path "target\classes\org\example\Main.class")) {
    & $mvn -q compile
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }
}
if ($Clean -or -not (Test-Path $cpFile)) {
    & $mvn -q dependency:build-classpath "-Dmdep.outputFile=$cpFile"
    if ($LASTEXITCODE -ne 0) { throw "Could not resolve dependencies." }
}

$cp = "target\classes;" + (Get-Content $cpFile)
Write-Host "Launching Maze Game..." -ForegroundColor Cyan
& $java -cp $cp org.example.Main
