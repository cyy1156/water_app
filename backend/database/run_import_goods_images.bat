@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==============================
echo 导入自我奖励商品图片
echo ==============================
echo.

cd /d "%~dp0"

set "PROJECT_ROOT=%~dp0..\.."
set "SOURCE_DIR=%PROJECT_ROOT%\images\goods\self-reward"
set "TARGET_DIR=%PROJECT_ROOT%\backend\uploads\goods"
set "JAR_RES_DIR=%PROJECT_ROOT%\backend\src\main\resources\static\uploads\goods"

if not exist "%SOURCE_DIR%" (
  echo 未找到源目录: %SOURCE_DIR%
  echo 请先在 images\goods\self-reward\ 中放入图片，参考该目录下的 README.md
  pause
  exit /b 1
)

if not exist "%TARGET_DIR%" mkdir "%TARGET_DIR%"
if not exist "%JAR_RES_DIR%" mkdir "%JAR_RES_DIR%"

echo 源目录: %SOURCE_DIR%
echo 目标1(本地): %TARGET_DIR%
echo 目标2(打包进jar/云托管): %JAR_RES_DIR%
echo.

set "COPY_COUNT=0"
for %%f in (reward_1 reward_2 reward_3 reward_4 reward_5 reward_6) do (
  if exist "%SOURCE_DIR%\%%f.png" (
    copy /Y "%SOURCE_DIR%\%%f.png" "%TARGET_DIR%\%%f.png" >nul 2>&1
    copy /Y "%SOURCE_DIR%\%%f.png" "%JAR_RES_DIR%\%%f.png" >nul 2>&1
    if !errorlevel! equ 0 (
      echo 已复制: %%f.png
      set /a COPY_COUNT+=1
    )
  )
  if exist "%SOURCE_DIR%\%%f.jpg" (
    copy /Y "%SOURCE_DIR%\%%f.jpg" "%TARGET_DIR%\%%f.jpg" >nul 2>&1
    copy /Y "%SOURCE_DIR%\%%f.jpg" "%JAR_RES_DIR%\%%f.jpg" >nul 2>&1
    if !errorlevel! equ 0 (
      echo 已复制: %%f.jpg
      set /a COPY_COUNT+=1
    )
  )
)

if %COPY_COUNT% equ 0 (
  echo 未找到 images\goods\self-reward\ 中的图片，尝试从 uploads\goods 同步到 jar 资源...
  for %%f in (reward_1 reward_2 reward_3 reward_4 reward_5 reward_6) do (
    if exist "%TARGET_DIR%\%%f.png" (
      copy /Y "%TARGET_DIR%\%%f.png" "%JAR_RES_DIR%\%%f.png" >nul 2>&1
      echo 已同步: %%f.png
    )
    if exist "%TARGET_DIR%\%%f.jpg" (
      copy /Y "%TARGET_DIR%\%%f.jpg" "%JAR_RES_DIR%\%%f.jpg" >nul 2>&1
      echo 已同步: %%f.jpg
    )
  )
) else (
  echo 共复制 %COPY_COUNT% 个文件到本地和 jar 资源目录
)
echo.

REM 生成临时 SQL 并执行（优先使用 png，否则 jpg）
set "SQL_FILE=%TEMP%\update_goods_images_%RANDOM%.sql"
(
  echo USE `water_app`;
  echo.
  for %%f in (1 2 3 4 5 6) do (
    if exist "%TARGET_DIR%\reward_%%f.png" echo UPDATE `goods` SET `image_url` = '/uploads/goods/reward_%%f.png' WHERE `id` = %%f;
    if not exist "%TARGET_DIR%\reward_%%f.png" if exist "%TARGET_DIR%\reward_%%f.jpg" echo UPDATE `goods` SET `image_url` = '/uploads/goods/reward_%%f.jpg' WHERE `id` = %%f;
  )
) > "%SQL_FILE%"

if "%MYSQL_ADDRESS%"=="" set "MYSQL_ADDRESS=localhost:3306"
if "%MYSQL_USERNAME%"=="" set "MYSQL_USERNAME=root"
if "%MYSQL_DATABASE%"=="" set "MYSQL_DATABASE=water_app"
for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do set "MYSQL_HOST=%%a" & set "MYSQL_PORT=%%b"
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"

echo 正在更新数据库...
if not "%MYSQL_PASSWORD%"=="" (
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" -D "%MYSQL_DATABASE%" < "%SQL_FILE%"
) else (
  echo 请输入 MySQL 密码:
  mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p -D "%MYSQL_DATABASE%" < "%SQL_FILE%"
)

del "%SQL_FILE%" 2>nul

if %errorlevel% equ 0 (
  echo 数据库更新完成
) else (
  echo 数据库更新失败，请检查 MySQL 连接
)
pause
