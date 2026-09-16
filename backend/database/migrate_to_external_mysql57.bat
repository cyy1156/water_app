@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ============================================================
:: 本地 water_app 数据库迁移到外网 MySQL 5.7 脚本
:: 腾讯云 CynosDB MySQL 外网地址
:: ============================================================

echo.
echo ============================================================
echo   本地 water_app -^> 外网 MySQL 5.7 迁移
echo ============================================================
echo.

REM ---------- 本地数据库（源）-----------
set "LOCAL_HOST=localhost"
set "LOCAL_PORT=3306"
set "LOCAL_USER=root"
set "LOCAL_PASS="
if not "%LOCAL_MYSQL_PASS%"=="" set "LOCAL_PASS=%LOCAL_MYSQL_PASS%"

REM ---------- 外网数据库（目标）-----------
set "REMOTE_HOST=<YOUR_MYSQL_HOST>"
set "REMOTE_PORT=20644"
set "REMOTE_USER=root"
set "REMOTE_PASS=<YOUR_MYSQL_PASSWORD>"

REM 可通过环境变量覆盖
if not "%MYSQL_REMOTE_HOST%"=="" set "REMOTE_HOST=%MYSQL_REMOTE_HOST%"
if not "%MYSQL_REMOTE_PORT%"=="" set "REMOTE_PORT=%MYSQL_REMOTE_PORT%"
if not "%MYSQL_REMOTE_USER%"=="" set "REMOTE_USER=%MYSQL_REMOTE_USER%"
if not "%MYSQL_REMOTE_PASS%"=="" set "REMOTE_PASS=%MYSQL_REMOTE_PASS%"

set "SCRIPT_DIR=%~dp0"
set "DUMP_FILE=%SCRIPT_DIR%water_app_dump_temp.sql"

echo [1/3] 从本地导出 water_app ...
if "%LOCAL_PASS%"=="" (
  set "LOCAL_OPTS=-p"
  echo 请输入本地 MySQL root 密码:
) else (
  set "LOCAL_OPTS=-p%LOCAL_PASS%"
)

mysqldump -h %LOCAL_HOST% -P %LOCAL_PORT% -u %LOCAL_USER% %LOCAL_OPTS% ^
  --databases water_app ^
  --single-transaction ^
  --routines ^
  --triggers ^
  --set-gtid-purged=OFF ^
  --default-character-set=utf8mb4 ^
  > "%DUMP_FILE%"

if %ERRORLEVEL% neq 0 (
  echo [失败] 本地导出失败，请检查连接与密码
  del "%DUMP_FILE%" 2>nul
  pause
  exit /b 1
)

echo [成功] 已导出到 %DUMP_FILE%
echo.

echo [1.5/3] 兼容 MySQL 5.7：替换排序规则 utf8mb4_0900_* -^> utf8mb4_unicode_ci ...
powershell -NoProfile -Command "$c = [System.IO.File]::ReadAllText('%DUMP_FILE%', [System.Text.Encoding]::UTF8); $c = $c -replace 'utf8mb4_0900_ai_ci','utf8mb4_unicode_ci' -replace 'utf8mb4_0900_as_cs','utf8mb4_unicode_ci' -replace 'utf8mb4_0900_as_ci','utf8mb4_unicode_ci'; [System.IO.File]::WriteAllText('%DUMP_FILE%', $c, [System.Text.UTF8Encoding]::new($false))"
if %ERRORLEVEL% neq 0 (
  echo [提示] 替换跳过，若导入失败可手动修改 dump 文件中的 collation
) else (
  echo [成功] 排序规则已替换
)
echo.

echo [2/3] 上传到外网 MySQL 5.7 ...

mysql -h %REMOTE_HOST% -P %REMOTE_PORT% -u %REMOTE_USER% -p%REMOTE_PASS% ^
  --default-character-set=utf8mb4 ^
  --ssl-mode=DISABLED ^
  < "%DUMP_FILE%"

if %ERRORLEVEL% neq 0 (
  echo [失败] 外网导入失败
  echo 若提示 SSL 错误，可删除脚本中的 --ssl-mode=DISABLED 重试
  echo 若提示权限错误，请检查外网白名单是否包含本机 IP
  del "%DUMP_FILE%" 2>nul
  pause
  exit /b 1
)

echo [成功] 已导入到外网数据库
echo.

echo [3/3] 清理临时文件 ...
del "%DUMP_FILE%" 2>nul

echo.
echo ============================================================
echo   迁移完成
echo   外网连接: %REMOTE_HOST%:%REMOTE_PORT%
echo   环境变量: MYSQL_ADDRESS=%REMOTE_HOST%:%REMOTE_PORT%
echo            MYSQL_USERNAME=%REMOTE_USER%
echo            MYSQL_PASSWORD=***
echo ============================================================
echo.
pause
