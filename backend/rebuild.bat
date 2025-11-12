@echo off
echo ========================================
echo Rebuilding Spring Boot Backend Project
echo ========================================
echo.

cd /d %~dp0

echo Using Java 24...
java -version
echo.

echo Setting MAVEN_OPTS for Java 24 compatibility...
set MAVEN_OPTS=--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED --add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
echo.

echo Step 1: Cleaning project...
call mvn clean
if %errorlevel% neq 0 (
    echo Clean failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Downloading dependencies...
call mvn dependency:resolve -U
if %errorlevel% neq 0 (
    echo.
    echo ========================================
    echo Dependency resolution failed!
    echo ========================================
    echo.
    echo Please check your network connection and try again.
    pause
    exit /b 1
)

:compile_step

echo.
echo Step 3: Compiling project (with Lombok annotation processing)...
call mvn clean compile
if %errorlevel% neq 0 (
    echo Compilation failed!
    echo.
    echo If you see Lombok-related errors, try:
    echo   1. Make sure Lombok is installed in your IDE
    echo   2. Run: mvn clean compile -U
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build completed successfully!
echo ========================================
echo.
echo You can now run the application with:
echo   mvn spring-boot:run
echo.
pause

