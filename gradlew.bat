@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables
setlocal
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem Set environment variables
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
@rem Resolve any "." and ".." in APP_HOME to make it shorter.
set APP_HOME=%~dp0
@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_EXE=java.exe
goto init
:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
:init
@rem Get command-line arguments, handling Windows variants
if not "%OS%"=="Windows_NT" goto win9xME_args
:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2
:win9xME_args_slurp
if "x%~1" == "x" goto execute
set CMD_LINE_ARGS=%*
:execute
@rem Setup the command line
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
@rem Execute Gradle
"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd
:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
exit /b 1
:mainEnd
if "%OS%"=="Windows_NT" endlocal
:omega
