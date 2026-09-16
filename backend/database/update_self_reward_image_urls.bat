@echo off
chcp 65001 >nul

REM ===== 在这里填你的外网 MySQL 信息 =====
set "MYSQL_ADDRESS=<YOUR_MYSQL_HOST>:3306"
set "MYSQL_USERNAME=root"
set "MYSQL_PASSWORD=<YOUR_MYSQL_PASSWORD>"
set "MYSQL_DATABASE=water_app"
REM =======================================

for /f "tokens=1,2 delims=:" %%a in ("%MYSQL_ADDRESS%") do (
  set "MYSQL_HOST=%%a"
  set "MYSQL_PORT=%%b"
)
if "%MYSQL_PORT%"=="" set "MYSQL_PORT=3306"

set "SQL_FILE=%TEMP%\update_self_reward_image_urls_%RANDOM%.sql"

(
  echo USE `%MYSQL_DATABASE%`;
  echo;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771839332364-6f8d132b-1924-44fd-ade5-6aadf9cd9e22.png' WHERE `id` = 1;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771839731125-5c7cffba-7400-4d6f-942e-3c771d0252e2.png' WHERE `id` = 2;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771839789263-ad0d00ce-ad1d-4e85-a703-7d54e8d1aa5e.png' WHERE `id` = 3;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771840269762-ef228721-83aa-45e4-9812-b991c51b0297.png' WHERE `id` = 4;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771840320292-11c927d7-e21b-4bc5-878d-e71ef6b3edd7.png' WHERE `id` = 5;
  echo UPDATE `goods` SET `image_url` = 'https://image2url.com/r2/default/images/1771840377906-363a75db-fd74-4b22-ae8a-bb8563867649.png' WHERE `id` = 6;
) > "%SQL_FILE%"

echo 正在更新外网數據庫 %MYSQL_HOST%:%MYSQL_PORT% / %MYSQL_DATABASE% ...
mysql -h "%MYSQL_HOST%" -P %MYSQL_PORT% -u "%MYSQL_USERNAME%" -p"%MYSQL_PASSWORD%" "%MYSQL_DATABASE%" < "%SQL_FILE%"

if %errorlevel% equ 0 (
  echo.
  echo 更新成功！
) else (
  echo.
  echo 更新失敗，請檢查 MySQL 連接信息。
)

del "%SQL_FILE%" 2>nul
pause