param([int]$Port=8080,[string]$DataDir='data')
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    $javaExecutable='java'
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) { $javaExecutable=Join-Path $env:JAVA_HOME 'bin/java.exe' }
    else {
        $candidateJdk=Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue | Where-Object { $_.Name -match '17' } | Select-Object -First 1 -ExpandProperty FullName
        if ($candidateJdk) { $javaExecutable=Join-Path $candidateJdk 'bin/java.exe' }
    }
    $jar=Join-Path $PSScriptRoot 'target/history-journey-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $jar)) { & (Join-Path $PSScriptRoot 'build.ps1') -Task package }
    Write-Host "Откройте http://127.0.0.1:$Port — остановка сервера: Ctrl+C"
    & $javaExecutable -jar $jar "--server.port=$Port" "--game.data-dir=$DataDir"
    if ($LASTEXITCODE -ne 0) { throw 'Сервер не запущен. Проверьте JDK 17+, порт и папку данных.' }
} finally { Pop-Location }
