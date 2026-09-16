@echo off
chcp 65001 >nul
echo ========================================
echo 喝水健康陪伴小程序 - 后端启动脚本
echo ========================================
echo.

cd /d %~dp0

echo [1/4] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到Java环境
    echo 请先安装JDK 1.8或更高版本
    echo 下载地址：https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo ✅ Java环境检查通过
echo.

echo [2/4] 检查Maven环境...
call mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误：未检测到Maven
    echo 请先安装Maven或使用IDE运行项目
    echo.
    echo 使用IDE运行步骤：
    echo 1. 使用IntelliJ IDEA或Eclipse打开backend文件夹
    echo 2. 找到 WaterAppApplication.java
    echo 3. 右键点击 -^> Run 'WaterAppApplication'
    pause
    exit /b 1
)
echo ✅ Maven环境检查通过
echo.

echo [3/4] 检查配置文件...
if not exist "src\main\resources\application.yml" (
    echo ❌ 错误：未找到配置文件 application.yml
    pause
    exit /b 1
)
echo ✅ 配置文件存在
echo.

echo [4/4] 启动SpringBoot应用...
echo.
echo 正在启动，请稍候...
echo 如果看到 "Tomcat started on port(s): 8080" 说明启动成功
echo 按 Ctrl+C 可以停止服务
echo.
echo ========================================
echo.

call mvn spring-boot:run

pause

