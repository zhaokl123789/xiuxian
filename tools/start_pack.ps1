param([string]$Proxy = "")
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$bundledPython = Join-Path $env:USERPROFILE '.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe'
$pythonCommand = if (Test-Path -LiteralPath $bundledPython) { $bundledPython } else { 'python' }
$installer = Join-Path $PSScriptRoot 'install_pack.py'
$installerArgs = @($installer)
if ($Proxy) { $installerArgs += @('--proxy', $Proxy) }
Push-Location $projectRoot
try {
    & .\gradlew.bat build --console=plain '-PwithPack=false'
    if ($LASTEXITCODE -ne 0) { throw 'Xiuxian build failed.' }
    & $pythonCommand @installerArgs
    if ($LASTEXITCODE -ne 0) { throw 'Pack installation failed.' }
    & .\gradlew.bat runClient --console=plain '-PwithPack=true'
    if ($LASTEXITCODE -ne 0) { throw 'Minecraft exited with an error.' }
} finally {
    Pop-Location
}
