@echo off
setlocal

set "GRADLE_HOME=D:\Tools\Gradle\gradle-8.14.4"
set "GRADLE_USER_HOME=D:\Tools\Gradle\GRADLE_Local_Repository"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo Gradle not found: %GRADLE_HOME%\bin\gradle.bat
    exit /b 1
)

if "%~1"=="" (
    call "%GRADLE_HOME%\bin\gradle.bat" -g "%GRADLE_USER_HOME%" --configure-on-demand --no-daemon --no-build-cache :common:compileJava :fabric:compileJava :forge:compileJava :neoforge:compileJava
) else (
    call "%GRADLE_HOME%\bin\gradle.bat" -g "%GRADLE_USER_HOME%" %*
)
