@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========================================
echo   Starting ClipVault (Fat JAR)...
echo ========================================
echo.

set JAVA_HOME=D:\JAVA
set PATH=%JAVA_HOME%\bin;%PATH%

java -Dfile.encoding=UTF-8 -Dawt.useSystemAAFontSettings=lcd -jar target\clipvault-1.0.0-fat.jar

if errorlevel 1 (
    echo.
    echo ========================================
    echo   An error occurred!
    echo ========================================
    pause
)
