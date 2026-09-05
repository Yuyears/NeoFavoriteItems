@echo off
setlocal

if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%~dp0.gradle-home"
set "TEMP=%~dp0.gradle-tmp"
set "TMP=%TEMP%"
if not exist "%TEMP%" mkdir "%TEMP%"

if defined GRADLE_HOME (
    set "GRADLE_CMD=%GRADLE_HOME%\bin\gradle.bat"
) else if exist "D:\Tools\Gradle\gradle-8.14.4\bin\gradle.bat" (
    set "GRADLE_CMD=D:\Tools\Gradle\gradle-8.14.4\bin\gradle.bat"
) else (
    set "GRADLE_CMD=%~dp0gradlew.bat"
)

if not exist "%GRADLE_CMD%" (
    echo Gradle command not found: %GRADLE_CMD%
    exit /b 1
)

if "%~1"=="" (
    call "%GRADLE_CMD%" -g "%GRADLE_USER_HOME%" --configure-on-demand --no-daemon --no-build-cache :common:compileJava :fabric:compileJava :forge:compileJava :neoforge:compileJava
) else (
    call "%GRADLE_CMD%" -g "%GRADLE_USER_HOME%" --configure-on-demand --no-daemon --no-build-cache %*
)
