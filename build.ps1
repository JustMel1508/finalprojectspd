param([ValidateSet('verify','package','test')][string]$Task='verify')
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    $candidateJdk = $env:JAVA_HOME
    if (-not $candidateJdk -or -not (Test-Path -LiteralPath (Join-Path $candidateJdk 'bin/javac.exe'))) {
        $candidateJdk = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '17' -and (Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe')) } |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if ($candidateJdk) { $env:JAVA_HOME=$candidateJdk }
    & .\mvnw.cmd $Task
    if ($LASTEXITCODE -ne 0) { throw 'Сборка завершилась ошибкой. Нужен JDK 17 или новее и доступ к Maven Central при первом запуске.' }
} finally { Pop-Location }
