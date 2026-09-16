@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ==============================
echo 运行大升级 M4 迁移：migrate_m4_reminder.sql
echo ==============================
echo.

cd /d "%~dp0"

if "%MYSQL_ADDRESS%"=="" (
  set "MYSQL_ADDRESS=localhost:3306"
)
for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do (
  set "MYSQL_HOST=%%a"
  set "MYSQL_PORT=%%b"
)
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"
if "%MYSQL_USERNAME%"=="" set "MYSQL_USERNAME=root"

set "SQL_FILE=migrate_m4_reminder.sql"
if not exist "%SQL_FILE%" (
  echo ❌ 未找到 %SQL_FILE%
  pause
  exit /b 1
)

echo 目标：%SQL_FILE%
echo MySQL：%MYSQL_HOST%:%MYSQL_PORT%
echo.

if not "%MYSQL_PASSWORD%"=="" (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" < "%SQL_FILE%"
) else (
  echo 请输入 MySQL 密码：
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p < "%SQL_FILE%"
)

if %errorlevel% equ 0 (
  echo ✅ migrate_m4_reminder.sql 执行成功！
) else (
  echo ❌ 执行失败！
)

pause

