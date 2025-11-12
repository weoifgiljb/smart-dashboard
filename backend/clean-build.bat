@echo off
echo Cleaning Maven project...
cd /d %~dp0
call mvn clean
echo.
echo Removing local repository cache for Spring Boot dependencies...
echo This may take a while...
call mvn dependency:purge-local-repository -DmanualInclude="org.springframework.boot:spring-boot-maven-plugin,org.springframework.boot:spring-boot-buildpack-platform,org.springframework.boot:spring-boot-loader-tools"
echo.
echo Downloading dependencies...
call mvn dependency:resolve
echo.
echo Building project...
call mvn compile
echo.
echo Done! You can now run: mvn spring-boot:run
pause

