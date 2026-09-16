@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ==============================
echo 运行大升级迁移：migrate_growth_v1.sql
echo ==============================
echo.

:: 切到脚本所在目录，确保重定向 < 能找到 sql 文件
cd /d "%~dp0"

:: 如果你的 mysql 不在 PATH，可取消注释并改成你的安装路径：
:: set "PATH=C:\Program Files\MySQL\MySQL Server 8.0\bin;%PATH%"

:: 读取环境变量（与后端 application.yml 一致）
:: MYSQL_ADDRESS 形如：localhost:3306
if "%MYSQL_ADDRESS%"=="" (
  set "MYSQL_ADDRESS=localhost:3306"
)
for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do (
  set "MYSQL_HOST=%%a"
  set "MYSQL_PORT=%%b"
)
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"

if "%MYSQL_USERNAME%"=="" (
  set "MYSQL_USERNAME=root"
)

set "SQL_FILE=migrate_growth_v1.sql"
if not exist "%SQL_FILE%" (
  echo ❌ 未找到 %SQL_FILE%
  echo 当前目录：%cd%
  pause
  exit /b 1
)

echo 目标：%SQL_FILE%
echo MySQL：%MYSQL_HOST%:%MYSQL_PORT%
echo 用户：%MYSQL_USERNAME%
echo.

:: 优先使用 MYSQL_PASSWORD（如果设置了就不再提示输入）
if not "%MYSQL_PASSWORD%"=="" (
  echo 使用环境变量 MYSQL_PASSWORD 执行（不会提示输入密码）
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" < "%SQL_FILE%"
) else (
  echo 请输入 MySQL 密码（不会回显），然后回车：
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p < "%SQL_FILE%"
)

if %errorlevel% equ 0 (
  echo.
  echo ✅ migrate_growth_v1.sql 执行成功！
) else (
  echo.
  echo ❌ migrate_growth_v1.sql 执行失败！
  echo - 请确认 mysql 命令可用（PATH 是否配置）
  echo - 请确认账号/密码正确
  echo - 请确认目标库 water_app 已存在（或在脚本中已 CREATE DATABASE）
)

echo.
pause


