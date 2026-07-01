@REM Maven Wrapper Script for Windows
@REM
@REM This script downloads and runs the Maven wrapper if not already present

@echo off

set MAVEN_WRAPPER_VERSION=3.9.6
set MAVEN_WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper-distribution/%MAVEN_WRAPPER_VERSION%/maven-wrapper-distribution-%MAVEN_WRAPPER_VERSION%-bin.zip
set WRAPPER_JAR=.mvn/wrapper/maven-wrapper.jar
set WRAPPER_PROPERTIES=.mvn/wrapper/maven-wrapper.properties

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_WRAPPER_URL%' -OutFile 'maven-wrapper.zip'"
    powershell -Command "Expand-Archive -Path 'maven-wrapper.zip' -DestinationPath '.mvn' -Force"
    powershell -Command "Move-Item -Path '.mvn/maven-wrapper-distribution-%MAVEN_WRAPPER_VERSION%/*' -Destination '.mvn' -Force"
    powershell -Command "Remove-Item 'maven-wrapper.zip' -Force"
    powershell -Command "Remove-Item '.mvn/maven-wrapper-distribution-%MAVEN_WRAPPER_VERSION%' -Recurse -Force"
)

if not exist "%WRAPPER_PROPERTIES%" (
    echo distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip > "%WRAPPER_PROPERTIES%"
    echo wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar >> "%WRAPPER_PROPERTIES%"
)

java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install JDK 21 or higher
    pause
    exit /b 1
)

java -jar "%WRAPPER_JAR%" %*
