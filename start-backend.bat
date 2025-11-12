@echo off
echo Starting Spring Boot Backend...

REM 检查并设置 Java 版本
echo Checking Java version...
java -version 2>nul | findstr /C:"version \"24" >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Java 24 may not be the active version.
    echo Please ensure Java 24 is set in JAVA_HOME or PATH.
    java -version
)

REM 确保 Maven 使用正确的 Java 版本
if defined JAVA_HOME (
    echo Using JAVA_HOME: %JAVA_HOME%
    REM 确保 JAVA_HOME 被正确设置（处理路径中的空格）
    set "JAVA_HOME=%JAVA_HOME%"
) else (
    echo ERROR: JAVA_HOME is not set!
    echo Please set JAVA_HOME to Java 24 installation directory.
    echo Example: set JAVA_HOME=C:\Program Files\Java\jdk-24
    pause
    exit /b 1
)

REM 验证 Java 版本
echo Verifying Java version from JAVA_HOME...
"%JAVA_HOME%\bin\java" -version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Cannot execute Java from JAVA_HOME: %JAVA_HOME%
    pause
    exit /b 1
)

REM 启动前尝试释放 8080 端口
echo Checking and releasing port 8080 if occupied...
REM 优先使用 PowerShell（可处理多条监听记录）
powershell -NoProfile -Command "Try { Get-NetTCPConnection -LocalPort 8080 -State Listen | Select -Expand OwningProcess -Unique ^| ForEach-Object { try { Stop-Process -Id $_ -Force -ErrorAction Stop; Write-Output ('Killed PID ' + $_) } catch {} } } Catch {}"
REM 回退方案：使用 netstat 解析 LISTENING 的 PID 并结束
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo Killing PID %%a occupying 8080 ...
    taskkill /F /PID %%a >nul 2>&1
)
echo Port 8080 check finished.

cd backend

REM 选择可用端口：优先 8080，占用则使用 8081
set "SERVER_PORT=8080"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    set "SERVER_PORT=8081"
)
echo Using server port: %SERVER_PORT%

REM 加载本地环境变量（可选）：backend\env.bat（用户可复制 env.example.bat 重命名为 env.bat 并填入密钥）
if exist "env.bat" (
    echo Loading environment variables from backend\env.bat ...
    call "env.bat"
    if "%OPENAI_API_KEY%"=="" (
        echo WARNING: OPENAI_API_KEY is empty after loading env.bat
    ) else (
        echo OPENAI_API_KEY is set.
    )
) else (
    echo backend\env.bat not found. Skipping local env load.
)

REM 清理旧的编译文件（使用 Java 24 重新编译）
echo Cleaning previous build...
call mvn clean -q

echo Checking if dependencies are downloaded...
if not exist "%USERPROFILE%\.m2\repository\org\springframework\boot\spring-boot-starter-parent\3.3.5" (
    echo Dependencies not found, downloading...
    call mvn dependency:resolve
)

REM 确保 Maven 使用 JAVA_HOME 中的 Java
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM 设置 Maven 选项，确保所有插件使用 Java 24
REM 注意：由于路径包含空格，不能直接在命令行参数中使用引号
REM 通过设置 PATH 和 JAVA_HOME，Maven 会自动使用正确的 Java

REM 先编译项目，确保使用 Java 24 编译
echo Compiling project with Java 24...
REM 不设置 executable 参数，让 Maven 使用 PATH 中的 Java（已设置为 Java 24）
call mvn compile -Dmaven.compiler.fork=true
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo Starting Spring Boot application...
echo Using Maven with Java 24 support...
echo JAVA_HOME is set to: %JAVA_HOME%

REM 使用 Maven exec:java 插件运行应用
REM exec:java 会自动构建正确的 classpath，避免文件读取问题
REM spring.classformat.ignore=true 已在 pom.xml 中配置，以支持 Java 24
echo Starting application with Java 24 using Maven exec plugin...
call mvn exec:java ^
    -Dexec.mainClass="com.selfdiscipline.SelfDisciplineApplication" ^
    -Dexec.classpathScope=runtime ^
    -Dexec.cleanupDaemonThreads=false ^
    -Dexec.args="--server.port=%SERVER_PORT%"
pause

