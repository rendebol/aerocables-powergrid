@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

rem Gradle cache and temp files stay inside the project folder (avoids problems with non-latin user names)
set "GRADLE_USER_HOME=%~dp0.gradle-home"
if not exist "%GRADLE_USER_HOME%\tmp" mkdir "%GRADLE_USER_HOME%\tmp"
set "JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=%GRADLE_USER_HOME%\tmp"

echo.
echo === [1/2] Генерация ресурсов мода (runData). Первый раз качается Minecraft и моды, это 10-20 минут ===
call gradlew.bat runData
if errorlevel 1 goto fail

echo.
echo === [2/2] Сборка jar ===
call gradlew.bat build
if errorlevel 1 goto fail

echo.
echo === Готово! Мод лежит здесь: ===
dir /b build\libs\*.jar
start "" "%~dp0build\libs"
pause
exit /b 0

:fail
echo.
echo === Сборка упала. Скопируй сообщение об ошибке выше (или файл build-log.txt, если есть) и отправь Claude. ===
pause
exit /b 1
