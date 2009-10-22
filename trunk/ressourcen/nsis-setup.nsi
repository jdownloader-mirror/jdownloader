# Auto-generated by EclipseNSIS Script Wizard
# 15.06.2008 20:22:26

Name JDownloader

; Definitions for Java 6.0
!define JRE_VERSION "6.0"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=20287"
;!define JRE_VERSION "5.0"
;!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=18675&/jre-1_5_0_15-windows-i586-p.exe"

# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 0.1.475
!define COMPANY JD-Team
!define URL http://www.jdownloader.org

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\orange-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULTFOLDER JDownloader
!define MUI_FINISHPAGE_RUN $INSTDIR\JD-WinLauncher.exe
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\orange-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY ${REGKEY}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage

#Warum fehlen anscheinend einige Sprachen?
#Antwort:
#http://nsis.sourceforge.net/Why_does_the_language_selection_dialog_hide_some_languages

# Included files
!include Sections.nsh
!include MUI.nsh

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\AdvSplash.dll"

# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE German
!insertmacro MUI_LANGUAGE Russian
!insertmacro MUI_LANGUAGE Spanish
!insertmacro MUI_LANGUAGE Turkish
!insertmacro MUI_LANGUAGE Polish
!insertmacro MUI_LANGUAGE Czech
!insertmacro MUI_LANGUAGE Ukrainian
!insertmacro MUI_LANGUAGE French
!insertmacro MUI_LANGUAGE Italian
!insertmacro MUI_LANGUAGE Dutch
!insertmacro MUI_LANGUAGE Bulgarian
!insertmacro MUI_LANGUAGE Danish
!insertmacro MUI_LANGUAGE Finnish
!insertmacro MUI_LANGUAGE Norwegian
!insertmacro MUI_LANGUAGE Portuguese
!insertmacro MUI_LANGUAGE Greek

# Installer attributes
OutFile JDownloader-Install.exe
InstallDir $PROGRAMFILES\JDownloader
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 0.1.475.0
VIAddVersionKey /LANG=${LANG_GERMAN} ProductName JDownloader
VIAddVersionKey /LANG=${LANG_GERMAN} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_GERMAN} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_GERMAN} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_GERMAN} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_GERMAN} FileDescription ""
VIAddVersionKey /LANG=${LANG_GERMAN} LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
!macro CREATE_SMGROUP_SHORTCUT NAME PATH
    Push "${NAME}"
    Push "${PATH}"
    Call CreateSMGroupShortcut
!macroend

Section -JDownloader SEC0000
    Call DetectJRE
    SetOutPath "$INSTDIR"
    SetOverwrite on
    File "C:\JDownloader_01475\JDownloader\JDownloader.jar"
    File "C:\JDownloader_01475\JDownloader\JDownloaderContainer.jar"
    File "C:\JDownloader_01475\JDownloader\JDownloaderPlugins.jar"
    File "C:\JDownloader_01475\JDownloader\JD-WinLauncher.exe"
    SetOutPath "$INSTDIR\libs"
    File "C:\JDownloader_01475\JDownloader\libs\BrowserLauncher2.jar"
    File "C:\JDownloader_01475\JDownloader\libs\jl1.0.jar"
    File "C:\JDownloader_01475\JDownloader\libs\js.jar"
    File "C:\JDownloader_01475\JDownloader\libs\swingx-0.9.2.jar"
    File "C:\JDownloader_01475\JDownloader\libs\swingx.jar"
    !insertmacro CREATE_SMGROUP_SHORTCUT "JDownloader starten" $INSTDIR\JD-WinLauncher.exe
    !insertmacro CREATE_SMGROUP_SHORTCUT "JDownloader Homepage" http://www.jdownloader.org
    !insertmacro CREATE_SMGROUP_SHORTCUT "JDownloader Wiki" http://wiki.jdownloader.org
    SetOutPath $DESKTOP
    CreateShortcut $DESKTOP\JDownloader.lnk $INSTDIR\JD-WinLauncher.exe
    WriteRegStr HKLM "${REGKEY}\Components" JDownloader 1
SectionEnd

Section -post SEC0001
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
!macro DELETE_SMGROUP_SHORTCUT NAME
    Push "${NAME}"
    Call un.DeleteSMGroupShortcut
!macroend

Section /o -un.JDownloader UNSEC0000
    Delete /REBOOTOK $DESKTOP\JDownloader.lnk
    !insertmacro DELETE_SMGROUP_SHORTCUT "JDownloader Wiki"
    !insertmacro DELETE_SMGROUP_SHORTCUT "JDownloader Homepage"
    !insertmacro DELETE_SMGROUP_SHORTCUT "JDownloader starten"
    Delete /REBOOTOK $INSTDIR\libs\\swingx.jar
    Delete /REBOOTOK $INSTDIR\libs\\swingx-0.9.2.jar
    Delete /REBOOTOK $INSTDIR\libs\\js.jar
    Delete /REBOOTOK $INSTDIR\libs\\jl1.0.jar
    Delete /REBOOTOK $INSTDIR\libs\\BrowserLauncher2.jar
    Delete /REBOOTOK $INSTDIR\JD-WinLauncher.exe
    Delete /REBOOTOK $INSTDIR\JDownloaderPlugins.jar
    Delete /REBOOTOK $INSTDIR\JDownloaderContainer.jar
    Delete /REBOOTOK $INSTDIR\JDownloader.jar
    DeleteRegValue HKLM "${REGKEY}\Components" JDownloader
SectionEnd

Section -un.post UNSEC0001
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
    Push $R0
    StrCpy $R0 $StartMenuGroup 1
    StrCmp $R0 ">" no_smgroup
no_smgroup:
    Pop $R0
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    Push $R1
    File /oname=$PLUGINSDIR\spltmp.bmp K:\jDownloader\jd\img\jd_logo_large.bmp
    advsplash::show 1000 600 400 -1 $PLUGINSDIR\spltmp
    Pop $R1
    Pop $R1
    !insertmacro MUI_LANGDLL_DISPLAY
FunctionEnd

Function CreateSMGroupShortcut
    Exch $R0 ;PATH
    Exch
    Exch $R1 ;NAME
    Push $R2
    StrCpy $R2 $StartMenuGroup 1
    StrCmp $R2 ">" no_smgroup
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$R1.lnk" $R0
no_smgroup:
    Pop $R2
    Pop $R1
    Pop $R0
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuGroup
    !insertmacro MUI_UNGETLANGUAGE
    !insertmacro SELECT_UNSECTION JDownloader ${UNSEC0000}
FunctionEnd

Function un.DeleteSMGroupShortcut
    Exch $R1 ;NAME
    Push $R2
    StrCpy $R2 $StartMenuGroup 1
    StrCmp $R2 ">" no_smgroup
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$R1.lnk"
no_smgroup:
    Pop $R2
    Pop $R1
FunctionEnd

# Installer Language Strings
# TODO Update the Language Strings with the appropriate translations.

LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_GERMAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_RUSSIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SPANISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_TURKISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_POLISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_CZECH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_UKRAINIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_FRENCH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ITALIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_DUTCH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BULGARIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_DANISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_FINNISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_NORWEGIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_PORTUGUESE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_GREEK} "Uninstall $(^Name)"

Function GetJRE
        MessageBox MB_OK "JDownloader uses Java ${JRE_VERSION}, it will now \
                         be downloaded and installed"
 
        StrCpy $2 "$TEMP\Java Runtime Environment.exe"
        nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
        Pop $R0 ;Get the return value
                StrCmp $R0 "success" +3
                MessageBox MB_OK "Download failed: $R0"
                Quit
        ExecWait $2
        Delete $2
FunctionEnd
 
 
Function DetectJRE
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" \
             "CurrentVersion"
  StrCmp $2 ${JRE_VERSION} done
 
  Call GetJRE
 
  done:
FunctionEnd
