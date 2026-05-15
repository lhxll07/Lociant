@echo off
setlocal

rem Prefer a local Gradle installation/cache, then fall back to Gradle Wrapper.
set "PROJECT_DIR=%~dp0"
set "TASK_ARGS=%*"

if defined GRADLE_HOME (
  if exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo Using Gradle from GRADLE_HOME: %GRADLE_HOME%
    call "%GRADLE_HOME%\bin\gradle.bat" %TASK_ARGS%
    exit /b %ERRORLEVEL%
  )
)

set "DIST_ROOT=%USERPROFILE%\.gradle\wrapper\dists\gradle-8.9-bin"
if exist "%DIST_ROOT%" (
  for /d %%D in ("%DIST_ROOT%\*") do (
    if exist "%%~fD\gradle-8.9\bin\gradle.bat" (
      echo Using cached Gradle: %%~fD\gradle-8.9
      call "%%~fD\gradle-8.9\bin\gradle.bat" %TASK_ARGS%
      exit /b %ERRORLEVEL%
    )
  )
)

where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  echo Using Gradle from PATH
  call gradle %TASK_ARGS%
  exit /b %ERRORLEVEL%
)

echo Using Gradle Wrapper
call "%PROJECT_DIR%gradlew.bat" %TASK_ARGS%
exit /b %ERRORLEVEL%
