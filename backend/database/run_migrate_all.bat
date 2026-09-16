@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==============================
echo 粥粥喝水 - 数据库迁移脚本
echo ==============================
echo.

:: 切换到脚本所在目录
cd /d "%~dp0"

:: MySQL 配置（与 application.yml 一致，可通过环境变量覆盖）
if "%MYSQL_ADDRESS%"=="" set "MYSQL_ADDRESS=localhost:3306"
if "%MYSQL_USERNAME%"=="" set "MYSQL_USERNAME=root"
if "%MYSQL_DATABASE%"=="" set "MYSQL_DATABASE=water_app"

for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do (
  set "MYSQL_HOST=%%a"
  set "MYSQL_PORT=%%b"
)
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"

echo 目标数据库: %MYSQL_DATABASE%
echo MySQL 地址: %MYSQL_HOST%:%MYSQL_PORT%
echo 用户名: %MYSQL_USERNAME%
echo.

:: 迁移文件列表（按执行顺序）
set "MIGRATIONS=migrate_growth_v1.sql migrate_m4_reminder.sql migrate_m5.sql"

:: 合并所有 SQL 文件为临时文件，只需输入一次密码
set "TEMP_SQL=%TEMP%\water_app_migrate_%RANDOM%.sql"
type nul > "%TEMP_SQL%"

for %%f in (%MIGRATIONS%) do (
  if exist "%%f" (
    echo -- ========== %%f ========== >> "%TEMP_SQL%"
    type "%%f" >> "%TEMP_SQL%"
    echo. >> "%TEMP_SQL%"
    echo [合并] %%f
  )
)

if not exist "%TEMP_SQL%" (
  echo 未找到任何迁移文件
  pause
  exit /b 1
)

echo.
echo 开始执行迁移（若未设置 MYSQL_PASSWORD 环境变量，将提示输入密码）...
echo.

if not "%MYSQL_PASSWORD%"=="" (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" -D "%MYSQL_DATABASE%" < "%TEMP_SQL%"
) else (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p -D "%MYSQL_DATABASE%" < "%TEMP_SQL%"
)

set "EXIT_CODE=%errorlevel%"
del "%TEMP_SQL%" 2>nul

echo.
echo ==============================
if %EXIT_CODE% equ 0 (
  echo 全部迁移完成！
) else (
  echo 迁移失败！可能原因：
  echo - 部分迁移已执行过（如重复添加列会报错）
  echo - 数据库连接失败或密码错误
  echo - 请检查上方 MySQL 报错信息
)
echo ==============================
pause
exit /b %EXIT_CODE%

