@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==============================
echo 运行迁移：migrate_store_achievement.sql
echo 商城与成就改造
echo ==============================
echo.
echo 【重要】执行前请先备份数据库！
echo 备份命令示例：mysqldump -u root -p water_app ^> backup_water_app_%%date:~0,10%%.sql
echo.
set /p confirm="已备份数据库？确认执行迁移请输入 Y: "
if /i not "%confirm%"=="Y" (
  echo 已取消
  pause
  exit /b 0
)
echo.

cd /d "%~dp0"

if "%MYSQL_ADDRESS%"=="" set "MYSQL_ADDRESS=localhost:3306"
if "%MYSQL_USERNAME%"=="" set "MYSQL_USERNAME=root"
if "%MYSQL_DATABASE%"=="" set "MYSQL_DATABASE=water_app"

for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do (
  set "MYSQL_HOST=%%a"
  set "MYSQL_PORT=%%b"
)
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"

set "SQL_FILE=migrate_store_achievement.sql"
if not exist "%SQL_FILE%" (
  echo 未找到 %SQL_FILE%
  pause
  exit /b 1
)

echo 目标: %SQL_FILE%
echo MySQL: %MYSQL_HOST%:%MYSQL_PORT% / %MYSQL_DATABASE%
echo.

if not "%MYSQL_PASSWORD%"=="" (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" -D "%MYSQL_DATABASE%" < "%SQL_FILE%"
) else (
  echo 请输入 MySQL 密码:
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p -D "%MYSQL_DATABASE%" < "%SQL_FILE%"
)

if %errorlevel% equ 0 (
  echo 迁移完成
) else (
  echo 迁移失败，请检查数据库连接和 SQL 文件
)
pause
