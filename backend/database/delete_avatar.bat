@echo off
REM ========= 基本配置，根据需要修改 =========
set MYSQL_HOST=<YOUR_MYSQL_HOST>
set MYSQL_PORT=20644
set MYSQL_USER=root
set MYSQL_PWD=<YOUR_MYSQL_PASSWORD>
set DB_NAME=water_app

REM 把下面的值替换成微信邮件附件 CSV 里的那个 OpenID
set OPENID=oxeEy3VFidrzCo5i3fsUeepNTO0Y 

REM ========= 执行 SQL：清空该用户的头像 URL =========
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PWD% %DB_NAME% ^
  -e "UPDATE user SET avatar_url = NULL WHERE openid = '%OPENID%'; SELECT id, openid, avatar_url FROM user WHERE openid = '%OPENID%';"

REM 可选：查询确认一下结果
mysql -h%MYSQL_HOST% -P%MYSQL_PORT% -u%MYSQL_USER% -p%MYSQL_PWD% %DB_NAME% ^
  -e "SELECT id, openid, avatar_url FROM user WHERE openid = '%OPENID%';"

pause