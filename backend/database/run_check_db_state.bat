@echo off
chcp 65001 >nul
cd /d "%~dp0"

if "%MYSQL_DATABASE%"=="" set "MYSQL_DATABASE=water_app"
if "%MYSQL_ADDRESS%"=="" set "MYSQL_ADDRESS=localhost:3306"
for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do set "MYSQL_HOST=%%a" & set "MYSQL_PORT=%%b"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"
if "%MYSQL_USERNAME%"=="" set "MYSQL_USERNAME=root"

echo 检查数据库状态...
if not "%MYSQL_PASSWORD%"=="" (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" -D "%MYSQL_DATABASE%" < check_db_state.sql
) else (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p -D "%MYSQL_DATABASE%" < check_db_state.sql
)
pause
