@echo off
setlocal
for %%I in ("%~dp0..") do set "PROJECT=%%~fI"
set "PORTABLE=%PROJECT%\.tools"

if not defined JAVA_HOME if exist "%PORTABLE%\jdk17\bin\java.exe" set "JAVA_HOME=%PORTABLE%\jdk17"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT if exist "%PORTABLE%\android-sdk\platforms\android-35\android.jar" set "ANDROID_HOME=%PORTABLE%\android-sdk"
if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT if exist "%LOCALAPPDATA%\Android\Sdk\platforms\android-35\android.jar" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

set "GRADLE=%PROJECT%\gradlew.bat"
if exist "%PORTABLE%\gradle-8.13\bin\gradle.bat" set "GRADLE=%PORTABLE%\gradle-8.13\bin\gradle.bat"
set "ART="
if /I "%~1"=="--placeholder-art" set "ART=-PplaceholderArt"
call "%GRADLE%" --project-dir "%PROJECT%" --console=plain testDebugUnitTest assembleDebug %ART%
if errorlevel 1 exit /b %errorlevel%
echo "%PROJECT%\app\build\outputs\apk\debug\app-debug.apk"
