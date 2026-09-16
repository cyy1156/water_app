@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ============================================================
echo MySQL 5.6/5.7 兼容性测试脚本
echo ============================================================
echo.
echo 请设置以下环境变量（或直接修改下方默认值）：
echo   MYSQL_HOST - 数据库主机（默认 localhost）
echo   MYSQL_PORT - 端口（默认 3306）
echo   MYSQL_USER - 用户名（默认 root）
echo   MYSQL_PASS - 密码
echo   MYSQL_EXTERNAL - 外部数据库连接串（可选，例如 192.168.1.100:3306）
echo.

REM ========== 这里修改为你的连接信息 ==========
set "HOST=<YOUR_MYSQL_HOST>"
set "PORT=20644"
set "USER=root"
set "PASS=<YOUR_MYSQL_PASSWORD>"
REM ============================================

if "%PASS%"=="" (
  set "MYSQL_OPTS=-p"
  set "PASS_PROMPT=请手动输入密码"
) else (
  set "MYSQL_OPTS=-p%PASS%"
)

REM 支持外部连接串：MYSQL_EXTERNAL=host:port
if not "%MYSQL_EXTERNAL%"=="" (
  for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_EXTERNAL%") do (
    set "HOST=%%a"
    set "PORT=%%b"
  )
)

echo 连接目标: %HOST%:%PORT% 用户: %USER%
echo.

set "SCRIPT_DIR=%~dp0"
set "SQL_FILE=%SCRIPT_DIR%test_mysql57_compatibility.sql"

if not exist "%SQL_FILE%" (
  echo 错误: 找不到 %SQL_FILE%
  pause
  exit /b 1
)

REM 使用 mysql 命令行执行测试
mysql -h %HOST% -P %PORT% -u %USER% %MYSQL_OPTS% < "%SQL_FILE%"

if %ERRORLEVEL% equ 0 (
  echo.
  echo [成功] 兼容性测试通过，目标 MySQL 版本支持 water_app 所需语法。
) else (
  echo.
  echo [失败] 执行出错。请检查：
  echo   1. MySQL 客户端是否已安装并在 PATH 中
  echo   2. 主机、端口、用户名、密码是否正确
  echo   3. 防火墙是否放行 20644 端口
  echo   4. 外部库是否允许远程连接
)

echo.
pause