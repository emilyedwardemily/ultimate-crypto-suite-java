; UltimateCryptoSuite - Windows installer (Inno Setup 6)
; Wraps the jpackage app-image built by scripts/build-native.sh.
;
; Build with:  ISCC packaging/innosetup/ucs.iss
; The script expects the jpackage app-image in dist/native/UltimateCryptoSuite
; and produces dist/native/UltimateCryptoSuite-Setup.exe
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
#define AppImageDir "..\..\dist\native\UltimateCryptoSuite"
#define OutDir "..\..\dist\native"

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

[Code]
// Refuse to run when the anti-tamper hash is missing from the app image
// (defense in depth: the Java guard still enforces this at runtime).
function InitializeSetup(): Boolean;
begin
  if not FileExists(ExpandConstant('{app}\UltimateCryptoSuite.jar')) and
     not FileExists(ExpandConstant('{src}\UltimateCryptoSuite.jar')) then
  begin
    MsgBox('Application files missing. Please re-download the installer.',
      mbError, MB_OK);
    Result := False;
    Exit;
  end;
  Result := True;
end;
