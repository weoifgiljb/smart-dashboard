@echo off
echo ========================================
echo Running Spring Boot Application Directly
echo ========================================
echo.

cd /d %~dp0

REM 检查 JAVA_HOME
if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME is not set!
    echo Please set JAVA_HOME to Java 24 installation directory.
    pause
    exit /b 1
)

echo Using JAVA_HOME: %JAVA_HOME%
echo.

REM 验证 Java 版本
echo Checking Java version...
"%JAVA_HOME%\bin\java" -version
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Cannot execute Java from JAVA_HOME: %JAVA_HOME%
    pause
    exit /b 1
)
echo.

REM 检查是否已编译
if not exist "target\classes\com\selfdiscipline\SelfDisciplineApplication.class" (
    echo Compiling project first...
    call mvn clean compile -Dmaven.compiler.executable="%JAVA_HOME%\bin\javac" -Dmaven.compiler.fork=true
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Compilation failed!
        pause
        exit /b 1
    )
    echo.
)

REM 构建 classpath
echo Building classpath...
set CLASSPATH=target\classes

REM 添加所有依赖到 classpath
for /r "%USERPROFILE%\.m2\repository" %%f in (*.jar) do (
    set CLASSPATH=!CLASSPATH!;%%f
)

REM 使用 Spring Boot 的类加载器运行
echo Starting Spring Boot application...
echo.

REM 使用 java -cp 直接运行
"%JAVA_HOME%\bin\java" ^
    -cp "%CLASSPATH%" ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
    org.springframework.boot.loader.JarLauncher

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to start application!
    echo Trying alternative method...
    echo.
    
    REM 尝试直接运行主类
    "%JAVA_HOME%\bin\java" ^
        -cp "%CLASSPATH%" ^
        --add-opens=java.base/java.lang=ALL-UNNAMED ^
        --add-opens=java.base/java.util=ALL-UNNAMED ^
        --add-opens=java.base/java.lang.reflect=ALL-UNNAMED ^
        com.selfdiscipline.SelfDisciplineApplication
)

pause









