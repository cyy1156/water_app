@echo off
chcp 65001 > nul
echo ==============================
echo 开始初始化 water_app 数据库...
echo ==============================
echo.

:: 检查 MySQL 是否能正常调用（如果提示 mysql 不是命令，取消下面这行注释，替换成你的 MySQL bin 路径）
set PATH=C:\Program Files\MySQL\MySQL Server 9.6\bin;%PATH%

:: 执行第一个初始化脚本 init.sql（数据库/表结构初始化）
::echo 正在执行 init.sql...
::mysql -u root -p water_app < init.sql
::if %errorlevel% equ 0 (
  :: echo ✅ init.sql 执行成功！
::) else (
  :: echo ❌ init.sql 执行失败，请检查密码或文件路径！
    ::pause
    ::exit /b 1
::)

echo.

:: 执行第二个初始化脚本 init_goods.sql（商品数据初始化，已包含TRUNCATE去重）
echo 正在执行 init_goods.sql...
mysql -u root -p water_app < init_goods.sql
if %errorlevel% equ 0 (
    echo ✅ init_goods.sql 执行成功！
) else (
    echo ❌ init_goods.sql 执行失败，请检查密码或文件路径！
    pause
    exit /b 1
)

echo.
echo ==============================
echo  所有脚本执行完成！
echo ==============================
pause