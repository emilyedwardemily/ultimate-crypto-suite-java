<#
=====================================================================
 UltimateCryptoSuite - self-contained Windows installer pipeline
=====================================================================

 Produces a Windows installer that BUNDLES a trimmed Java runtime
(JRE 17+ / matching JDK 25) plus the JavaFX modules, so the app runs
on a fresh Windows machine with NO Java installed.

 Pipeline:
   1. Build the thin obfuscated jar        (mvn -Pnative -Pobfuscate)
   2. Inject the anti-tamper hash           (scripts/hash-and-sign.py)
   3. Fetch the JavaFX jmods for windows-x64 (cached in target/jmods)
   4. Stage the jar for jpackage
   5. jpackage -> app-image (bundled runtime) + .msi installer
      with Desktop + Start Menu shortcuts

 Usage:
   powershell -ExecutionPolicy Bypass -File scripts\build-windows.ps1
   powershell -ExecutionPolicy Bypass -File scripts\build-windows.ps1 -AppImageOnly

 Output (all under target\):
   target\UltimateCryptoSuite\                 (app image w/ bundled runtime)
   target\UltimateCryptoSuite-1.0.0.msi        (the installer)

 Prereqs:
   * JDK 25 with jpackage on JAVA_HOME (or -JavaHome <path>)
   * WiX Toolset v3.x (candle.exe / light.exe) on PATH  -- required by jpackage for .msi
   * Maven on PATH  * Python 3 on PATH  * Internet (first run, to fetch JavaFX jmods)
=====================================================================
#>
[CmdletBinding()]
param(
    [string] $JavaHome = $env:JAVA_HOME,
    [switch] $AppImageOnly
)

$ErrorActionPreference = 'Stop'
$ProgressPreference    = 'SilentlyContinue'

$ProjectDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $ProjectDir

# ── application metadata (must mirror pom.xml <properties>) ───────────
$AppName       = 'UltimateCryptoSuite'
$AppVendor     = 'emilyedward'
$AppVersion    = '1.0.0'
$MainClass     = 'app.Launcher'
$JavaFxVersion = '25.0.3'
$Classifier    = 'windows-x64'
$Icon          = 'packaging/icons/windows/ucs.ico'
$LicenseFile   = 'TERMS.txt'

$Jar        = "target/$AppName-1.0-SNAPSHOT-obfuscated.jar"
$JmodsDir   = 'target/jmods'
$JmodsRoot  = "$JmodsDir/javafx-jmods-$JavaFxVersion"
$InputDir   = 'target/jpackage-input'
$OutDir     = 'target'
$AppImage   = "$OutDir/$AppName"

# JDK + JavaFX modules baked into the bundled runtime image.
$AppModules = @(
    'java.base', 'java.logging', 'java.management', 'java.desktop', 'java.xml',
    'java.net.http', 'java.naming', 'java.security.jgss', 'jdk.crypto.ec', 'jdk.unsupported',
    'javafx.base', 'javafx.controls', 'javafx.fxml', 'javafx.graphics', 'javafx.media'
)

# JVM options for the packaged launcher.
$JvmOpts = @(
    '--enable-native-access=javafx.graphics',
    '--add-exports=javafx.graphics/com.sun.glass.ui=ALL-UNNAMED',
    '-Xms128m', '-Xmx1g'
)

function Write-Step([string] $msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Fail([string] $msg)       { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }

# ── 0. locate a usable JDK / jpackage ─────────────────────────────────
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/jpackage.exe'))) {
    $candidates = @(
        $env:JAVA_HOME,
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft\jdk-25*'
    ) | Where-Object { $_ }
    foreach ($c in $candidates) {
        $jp = Join-Path $c 'bin/jpackage.exe'
        if (Test-Path $jp) { $JavaHome = $c; break }
        $found = Get-ChildItem -Path $c -Filter 'jpackage.exe' -Recurse -ErrorAction SilentlyContinue |
                 Select-Object -First 1
        if ($found) { $JavaHome = Split-Path -Parent (Split-Path -Parent $found.FullName); break }
    }
}
if (-not $JavaHome -or -not (Test-Path (Join-Path $JavaHome 'bin/jpackage.exe'))) {
    Fail "jpackage not found. Install JDK 25 and set JAVA_HOME (or pass -JavaHome <path>)."
}
$env:JAVA_HOME = $JavaHome
$JPackage = Join-Path $JavaHome 'bin/jpackage.exe'
Write-Step "Using JDK: $JavaHome"

# ── 0b. ensure the JDK ships jmods ────────────────────────────────────
# Adoptium's Windows JDK archive omits jmods/, which both jlink (invoked by
# jpackage to build the bundled runtime) and the -Pnative ProGuard step need.
# Provision them from the matching Adoptium jmods asset when absent.
$JdkJmods = Join-Path $JavaHome 'jmods'
if (-not (Test-Path (Join-Path $JdkJmods 'java.base.jmod'))) {
    Write-Step 'JDK has no jmods/ - provisioning from Adoptium'
    $jdkCache = Join-Path 'target/jdk'
    New-Item -ItemType Directory -Force -Path $jdkCache | Out-Null
    $url = 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jmods/hotspot/normal/eclipse?project=jdk'
    $zip = Join-Path $jdkCache 'jdk-jmods.zip'
    Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
    $tmp = Join-Path $jdkCache 'jmods-tmp'
    if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    New-Item -ItemType Directory -Force -Path $JdkJmods | Out-Null
    Copy-Item (Join-Path $inner.FullName '*.jmod') $JdkJmods -Force
    Remove-Item -Recurse -Force $tmp
    Remove-Item -Force $zip
}
if (-not (Test-Path (Join-Path $JdkJmods 'java.base.jmod'))) {
    Fail "Could not provision jmods into $JdkJmods"
}

# ── 1. WiX Toolset (jpackage needs candle.exe + light.exe for .msi) ───
function Find-Wix {
    foreach ($n in @('candle.exe', 'light.exe')) {
        if (-not (Get-Command $n -ErrorAction SilentlyContinue)) { return $false }
    }
    return $true
}
if (-not (Find-Wix)) {
    $wixRoots = @(
        'C:\Program Files (x86)\WiX Toolset v3.14\bin',
        'C:\Program Files (x86)\WiX Toolset v3.11\bin',
        'C:\Program Files (x86)\WiX Toolset v3.10\bin'
    )
    foreach ($w in $wixRoots) {
        if (Test-Path (Join-Path $w 'candle.exe')) { $env:Path = "$w;$env:Path"; break }
    }
}
if (-not (Find-Wix)) {
    Write-Host 'WARNING: WiX Toolset not found on PATH.' -ForegroundColor Yellow
    Write-Host '         The .msi step will fail. Install it with:' -ForegroundColor Yellow
    Write-Host '           winget install WiXToolset.WiXToolset' -ForegroundColor Yellow
    Write-Host '         (or: choco install wixtoolset)' -ForegroundColor Yellow
    if (-not $AppImageOnly) { Fail 'WiX is required to build the .msi installer.' }
}

# ── 2. fetch the JavaFX jmods for windows-x64 (needed by jlink + ProGuard) ──
Write-Step "[1/5] Ensuring JavaFX jmods ($JavaFxVersion, $Classifier)"
if (-not (Test-Path (Join-Path $JmodsRoot 'javafx.base.jmod'))) {
    New-Item -ItemType Directory -Force -Path $JmodsDir | Out-Null
    $zipName = "openjfx-${JavaFxVersion}_${Classifier}_bin-jmods.zip"
    $zipPath = Join-Path $JmodsDir $zipName
    $url     = "https://download2.gluonhq.com/openjfx/${JavaFxVersion}/${zipName}"
    Write-Host "    downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
    Expand-Archive -Path $zipPath -DestinationPath $JmodsDir -Force
    Remove-Item $zipPath -Force
}
if (-not (Test-Path (Join-Path $JmodsRoot 'javafx.base.jmod'))) {
    Fail "JavaFX jmods missing at $JmodsRoot"
}

# ── 3. build the thin obfuscated jar (no JavaFX; provided by jmods) ───
Write-Step '[2/5] Building thin obfuscated jar (-Pnative -Pobfuscate)'
& mvn -B -ntp -Pnative -Pobfuscate -DskipTests verify
if ($LASTEXITCODE -ne 0) { Fail 'Maven build failed.' }
if (-not (Test-Path $Jar)) { Fail "$Jar was not produced." }

# ── 4. inject the anti-tamper hash ────────────────────────────────────
Write-Step '[3/5] Injecting anti-tamper hash'
# Pick the first Python 3 that actually RUNS. On Windows `python`/`python3`
# often resolve to the Microsoft Store alias stub, which exists but only
# prints "Python was not found"; probe it before trusting it.
$py = $null; $pyPre = @()
foreach ($spec in @(@('python', @()), @('python3', @()), @('py', @('-3')))) {
    if ($py) { break }
    if (-not (Get-Command $spec[0] -ErrorAction SilentlyContinue)) { continue }
    & $spec[0] @($spec[1]) '--version' 2>$null 1>$null
    if ($LASTEXITCODE -eq 0) { $py = $spec[0]; $pyPre = @($spec[1]) }
}
if (-not $py) { Fail 'A runnable Python 3 is required (scripts/hash-and-sign.py).' }
& $py @pyPre 'scripts/hash-and-sign.py' $Jar
if ($LASTEXITCODE -ne 0) { Fail 'Anti-tamper signing failed.' }

# ── 5. stage the input dir for jpackage ───────────────────────────────
Write-Step '[4/5] Staging jpackage input'
if (Test-Path $InputDir) { Remove-Item -Recurse -Force $InputDir }
New-Item -ItemType Directory -Force -Path $InputDir | Out-Null
Copy-Item $Jar (Join-Path $InputDir 'UltimateCryptoSuite.jar') -Force

# ── 6. jpackage ───────────────────────────────────────────────────────
Write-Step '[5/5] Running jpackage'
if (Test-Path $AppImage) { Remove-Item -Recurse -Force $AppImage }
if (Test-Path "$OutDir/$AppName-$AppVersion.msi") { Remove-Item -Force "$OutDir/$AppName-$AppVersion.msi" }

$common = @(
    '--name', $AppName,
    '--app-version', $AppVersion,
    '--vendor', $AppVendor,
    '--input', $InputDir,
    '--main-jar', 'UltimateCryptoSuite.jar',
    '--main-class', $MainClass,
    '--module-path', $JmodsRoot,
    '--add-modules', ($AppModules -join ','),
    '--dest', $OutDir
)
foreach ($o in $JvmOpts) { $common += @('--java-options', $o) }
if (Test-Path $Icon) { $common += @('--icon', $Icon) }

# App image (bundled runtime) — also the source for the Inno Setup wizard.
# NOTE: --license-file is only valid for installer types, not app-image.
& $JPackage @common --type app-image
if ($LASTEXITCODE -ne 0) { Fail 'jpackage app-image failed.' }
Write-Host "    app-image -> $AppImage"

if ($AppImageOnly) { Write-Step "done (app-image only). Artifacts in $OutDir"; exit 0 }

# MSI installer with Desktop + Start Menu shortcuts.
$msiOpts = @(
    '--type', 'msi',
    '--win-shortcut',
    '--win-menu',
    '--win-menu-group', $AppName,
    '--win-dir-chooser',
    '--win-upgrade-uuid', '8A2F7C1B-5E64-4A3E-9C0D-7B1E2C3D4A5F'
)
if (Test-Path $LicenseFile) { $msiOpts += @('--license-file', $LicenseFile) }
& $JPackage @common @msiOpts
if ($LASTEXITCODE -ne 0) { Fail 'jpackage msi failed.' }

Write-Step "done. Installer artifacts in $OutDir"
Get-ChildItem $OutDir -Filter '*.msi' | ForEach-Object { Write-Host "    msi -> $($_.FullName)" }

# Explicit success exit code: some JDK tools write to stderr, and Windows
# PowerShell's -File exit code can otherwise reflect that instead of 0.
exit 0
