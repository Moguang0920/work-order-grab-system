@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  nameless 一键启动脚本
rem
rem  为什么要先 clean compile：
rem  VSCode 的 Java 语言服务器用 Eclipse 编译器 (ECJ) 处理 Lombok 时，
rem  会生成无效字节码（ClassFormatError: Invalid method Code length 0），
rem  覆盖掉 Maven 编好的 target/classes。表现就是"昨天还能跑，今天启动报
rem  Role / User / WorkOrder 之类的类格式错误"。
rem  这里每次都用 Maven 重新编一遍，保证运行的 class 一定是好的。
rem ============================================================

cd /d "%~dp0nameless"

echo.
echo [1/3] 检查 MySQL 与 Redis ...
netstat -ano | findstr ":3306" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo     [警告] MySQL 3306 未监听，应用启动后连库会失败
) else (
    echo     MySQL OK
)
netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
if errorlevel 1 (
    echo     [提示] Redis 6379 未监听，正在尝试拉起 Docker Desktop ...
    rem Redis 跑在 Docker 容器里（容器名 redis7），Docker Desktop 一关它就没了。
    rem 应用启动阶段不会报错（Lettuce 是懒连接），但一登录就 500 —— 就是这个原因。
    call docker desktop start >nul 2>&1
    timeout /t 12 /nobreak >nul
    call docker start redis7 >nul 2>&1
    timeout /t 5 /nobreak >nul
    netstat -ano | findstr ":6379" | findstr "LISTENING" >nul
    if errorlevel 1 (
        echo     [警告] Redis 仍未就绪，登录会返回 500。请手动打开 Docker Desktop
        echo            或执行: docker start redis7
    ) else (
        echo     Redis 已拉起
    )
) else (
    echo     Redis OK
)

echo.
echo [2/3] 用 Maven 重新编译（覆盖 IDE 可能产生的无效字节码）...
call mvn -q clean compile -DskipTests
if errorlevel 1 (
    echo.
    echo [失败] 编译不通过，请检查上面的报错
    pause
    exit /b 1
)
echo     编译成功

echo.
echo [3/3] 启动应用 ...
echo     接口: http://localhost:8080/user
echo     文档: http://localhost:8080/swagger-ui.html
echo.
call mvn spring-boot:run

echo.
echo 应用已退出
pause
