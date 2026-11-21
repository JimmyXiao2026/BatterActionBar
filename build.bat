@echo off
chcp 65001 >nul
set JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
call gradlew.bat build --offline
pause
