; UltimateCryptoSuite - Windows installer (Inno Setup 6)
; Wraps the self-contained jpackage app-image (bundled JRE) produced by
; scripts/build-windows.ps1.
;
; Build with:  ISCC packaging/innosetup/ucs.iss
; Expects the app-image in target/UltimateCryptoSuite and writes
; target/UltimateCryptoSuite-Setup.exe. Both locations can be overridden, e.g.
;   ISCC /DAppImageDir=C:\app\UltimateCryptoSuite /DOutDir=C:\out ucs.iss
;
; Features:
;   - Terms & Conditions wizard page (LicenseFile)
;   - Per-machine install (UAC elevation) with optional per-user mode
;   - Desktop + Start Menu shortcuts
;   - Add/Remove Programs entry
;   - Silent install support (/SILENT) for enterprise deployment

#define MyAppName "Ultimate Crypto Suite"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "emilyedward"
#define MyAppExeName "UltimateCryptoSuite.exe"
#define MyAppAssocName "Ultimate Crypto Suite"
#ifndef AppImageDir
  #define AppImageDir "..\..\target\UltimateCryptoSuite"
#endif
#ifndef OutDir
  #define OutDir "..\..\target"
#endif

[Setup]
AppId={{8A2F7C1B-5E64-4A3E-9C0D-ULTIMATECRYPTOSUITE}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\{#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=admin
PrivilegesRequiredOverridesAllowed=dialog
OutputDir={#OutDir}
OutputBaseFilename=UltimateCryptoSuite-Setup
SetupIconFile=..\icons\windows\ucs.ico
UninstallDisplayIcon={app}\{#MyAppExeName}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
LicenseFile=..\..\TERMS.txt

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "swedish"; MessagesFile: "compiler:Languages\Swedish.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "{#AppImageDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: quicklaunchicon

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[Registry]
Root: HKA; Subkey: "Software\Classes\{#MyAppAssocName}\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{app}\{#MyAppExeName}"" ""%1"""; Flags: uninsdeletevalue

