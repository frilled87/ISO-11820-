@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Windows
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set MAVEN_WRAPPER_JAR="%CD%\.mvn\wrapper\maven-wrapper.jar"
set MAVEN_PROJECTBASEDIR=%CD%
if not exist %MAVEN_WRAPPER_JAR% (
    echo Maven wrapper jar not found, attempting to install Maven...
    goto :run
)
java -jar %MAVEN_WRAPPER_JAR% %*
goto :end
:run
echo Running Maven via system installation...
mvn %*
:end
endlocal
