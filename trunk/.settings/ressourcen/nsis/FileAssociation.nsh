/*
_____________________________________________________________________________
 
                       File Association
_____________________________________________________________________________
 
 Based on code taken from http://nsis.sourceforge.net/File_Association 
 
 Usage in script:
 1. !include "FileAssociation.nsh"
 2. [Section|Function]
      ${FileAssociationFunction} "Param1" "Param2" "..." $var
    [SectionEnd|FunctionEnd]
 
 FileAssociationFunction=[RegisterExtension|UnRegisterExtension]
 
_____________________________________________________________________________
 
 ${RegisterExtension} "[executable]" "[extension]" "[description]"
 
"[executable]"     ; executable which opens the file format
                   ;
"[extension]"      ; extension, which represents the file format to open
                   ;
"[description]"    ; description for the extension. This will be display in Windows Explorer.
                   ;
 
 
 ${UnRegisterExtension} "[extension]" "[description]"
 
"[extension]"      ; extension, which represents the file format to open
                   ;
"[description]"    ; description for the extension. This will be display in Windows Explorer.
                   ;
 
_____________________________________________________________________________
 
                         Macros
_____________________________________________________________________________
 
 Change log window verbosity (default: 3=no script)
 
 Example:
 !include "FileAssociation.nsh"
 !insertmacro RegisterExtension
 ${FileAssociation_VERBOSE} 4   # all verbosity
 !insertmacro UnRegisterExtension
 ${FileAssociation_VERBOSE} 3   # no script
*/
 
 
!ifndef FileAssociation_INCLUDED
!define FileAssociation_INCLUDED
 
!include Util.nsh
 
!verbose push
!verbose 3
!ifndef _FileAssociation_VERBOSE
  !define _FileAssociation_VERBOSE 3
!endif
!verbose ${_FileAssociation_VERBOSE}
!define FileAssociation_VERBOSE `!insertmacro FileAssociation_VERBOSE`
!verbose pop
 
!macro FileAssociation_VERBOSE _VERBOSE
  !verbose push
  !verbose 3
  !undef _FileAssociation_VERBOSE
  !define _FileAssociation_VERBOSE ${_VERBOSE}
  !verbose pop
!macroend
 
 
 
!macro RegisterExtensionCall _EXECUTABLE _EXTENSION _DESCRIPTION
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
  Push `${_DESCRIPTION}`
  Push `${_EXTENSION}`
  Push `${_EXECUTABLE}`
  ${CallArtificialFunction} RegisterExtension_
  !verbose pop
!macroend
 
!macro UnRegisterExtensionCall _EXTENSION _DESCRIPTION
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
  Push `${_EXTENSION}`
  Push `${_DESCRIPTION}`
  ${CallArtificialFunction} UnRegisterExtension_
  !verbose pop
!macroend
 
 
 
!define RegisterExtension `!insertmacro RegisterExtensionCall`
!define un.RegisterExtension `!insertmacro RegisterExtensionCall`
 
!macro RegisterExtension
!macroend
 
!macro un.RegisterExtension
!macroend

!macro RegisterExtension_
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
 
  Exch $R2 ;exe
  Exch
  Exch $R1 ;ext
  Exch
  Exch 2
  Exch $R0 ;desc
  Exch 2
  Push $0
  Push $1
  
${If} ${UAC_IsAdmin}

  ReadRegStr $1 HKCR $R1 ""  ; read current file association
  StrCmp "$1" "" NoBackupAdmin  ; is it empty
  StrCmp "$1" "$R0" NoBackupAdmin  ; is it our own
    WriteRegStr HKCR $R1 "backup_val" "$1"  ; backup current value
NoBackupAdmin:
  WriteRegStr HKCR $R1 "" "$R0"  ; set our file association
 
  ReadRegStr $0 HKCR $R0 ""
  StrCmp $0 "" 0 SkipAdmin
    WriteRegStr HKCR "$R0" "" "$R0"
    WriteRegStr HKCR "$R0\shell" "" "open"
    WriteRegStr HKCR "$R0\DefaultIcon" "" "$R2,0"
SkipAdmin:
  WriteRegStr HKCR "$R0\shell\open\command" "" '"$R2" "%1"'

${Else}

  ReadRegStr $1 HKCU "Software\Classes\$R1" ""  ; read current file association
  StrCmp "$1" "" NoBackupUser  ; is it empty
  StrCmp "$1" "$R0" NoBackupUser  ; is it our own
    WriteRegStr HKCU "Software\Classes\$R1" "backup_val" "$1"  ; backup current value
NoBackupUser:
  WriteRegStr HKCU "Software\Classes\$R1" "" "$R0"  ; set our file association
 
  ReadRegStr $0 HKCU "Software\Classes\$R0" ""
  StrCmp $0 "" 0 SkipUser
    WriteRegStr HKCU "Software\Classes\$R0" "" "$R0"
    WriteRegStr HKCU "Software\Classes\$R0\shell" "" "open"
    WriteRegStr HKCU "Software\Classes\$R0\DefaultIcon" "" "$R2,0"
SkipUser:
  WriteRegStr HKCU "Software\Classes\$R0\shell\open\command" "" '"$R2" "%1"'

${EndIf}
  
  /* No edit in our mod 
  WriteRegStr HKCR "$R0\shell\edit" "" "Edit $R0"
  WriteRegStr HKCR "$R0\shell\edit\command" "" '"$R2" "%1"'
  */
 
  Pop $1
  Pop $0
  Pop $R2
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
 
 
!define UnRegisterExtension `!insertmacro UnRegisterExtensionCall`
!define un.UnRegisterExtension `!insertmacro UnRegisterExtensionCall`
 
!macro UnRegisterExtension
!macroend
 
!macro un.UnRegisterExtension
!macroend
 
!macro UnRegisterExtension_
  !verbose push
  !verbose ${_FileAssociation_VERBOSE}
 
  Exch $R1 ;desc
  Exch
  Exch $R0 ;ext
  Exch
  Push $0
  Push $1
 
${If} $ADMINATINSTALL > 0
  DeleteRegKey HKCR $R1 ;Delete key with association name settings
  ReadRegStr $1 HKCR $R0 ""
  StrCmp $1 $R1 0 NoOwnAdmin ; only do this if we own it
  ReadRegStr $1 HKCR $R0 "backup_val"
  StrCmp $1 "" 0 RestoreAdmin ; if backup="" then delete the whole key
  DeleteRegKey HKCR $R0
  Goto NoOwnAdmin
 
RestoreAdmin:
  WriteRegStr HKCR $R0 "" $1
  DeleteRegValue HKCR $R0 "backup_val"
 
NoOwnAdmin:
 
${Else}
  DeleteRegKey HKCU "Software\Classes\$R1" ;Delete key with association name settings
  ReadRegStr $1 HKCU "Software\Classes\$R0" ""
  StrCmp $1 $R1 0 NoOwnUser ; only do this if we own it
  ReadRegStr $1 HKCU "Software\Classes\$R0" "backup_val"
  StrCmp $1 "" 0 RestoreUser ; if backup="" then delete the whole key
  DeleteRegKey HKCU "Software\Classes\$R0"
  Goto NoOwnUser
 
RestoreUser:
  WriteRegStr HKCU "Software\Classes\$R0" "" $1
  DeleteRegValue HKCU "Software\Classes\$R0" "backup_val"
 
NoOwnUser:
${EndIf}

  Pop $1
  Pop $0
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
!endif # !FileAssociation_INCLUDED