@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM Standalone Windows cmd runner for demosdk (no .ps1 file needed).
REM Linux / macOS / Git Bash: use run-local.sh
REM
REM demosdk is a library (no main class), so there is no start/stop: the commands
REM build, test, measure coverage and install the JAR into the local Maven repo.
REM
REM   run-local.bat init | test | coverage | install | javadoc | deps | sonar | clean | all
REM Optional: MAVEN_REPO (default %USERPROFILE%\.m2\repository)  MVN_ARGS (extra Maven args)
REM           SONAR_TOKEN SONAR_ORG (sonar)

set "MODULE_DIR=%~dp0"
if "%MODULE_DIR:~-1%"=="\" set "MODULE_DIR=%MODULE_DIR:~0,-1%"

set "LOCAL_DIR=%MODULE_DIR%\.local"
set "LOG_DIR=%LOCAL_DIR%\logs"
set "MODULE=demosdk"
set "POM=%MODULE_DIR%\pom.xml"
set "VERSION="
set "KERNEL_CORE_VERSION="
for /f "usebackq delims=" %%V in (`powershell -NoProfile -Command "([xml](Get-Content -Raw '%POM%')).project.version"`) do set "VERSION=%%V"
for /f "usebackq delims=" %%V in (`powershell -NoProfile -Command "([xml](Get-Content -Raw '%POM%')).project.properties.'kernel.core.version'"`) do set "KERNEL_CORE_VERSION=%%V"
set "JACOCO_CSV=%MODULE_DIR%\target\site\jacoco\jacoco.csv"
set "JACOCO_HTML=%MODULE_DIR%\target\site\jacoco\index.html"
set "COVERAGE_MIN=90"
set "MVN_SKIP=-Dgpg.skip=true -Dmaven.javadoc.skip=true"

set "REPO_ARG="
if defined MAVEN_REPO set "REPO_ARG=-Dmaven.repo.local=%MAVEN_REPO%"
if not defined MAVEN_REPO set "MAVEN_REPO=%USERPROFILE%\.m2\repository"

set "CMD=%~1"
if "%CMD%"=="" goto :usage
if /I "%CMD%"=="-h" goto :usage
if /I "%CMD%"=="--help" goto :usage
if /I "%CMD%"=="help" goto :usage
if /I "%CMD%"=="init" goto :init
if /I "%CMD%"=="test" goto :test
if /I "%CMD%"=="coverage" goto :coverage
if /I "%CMD%"=="install" goto :install
if /I "%CMD%"=="javadoc" goto :javadoc
if /I "%CMD%"=="deps" goto :deps
if /I "%CMD%"=="sonar" goto :sonar
if /I "%CMD%"=="clean" goto :clean
if /I "%CMD%"=="all" goto :all

echo error: unknown command '%CMD%'
goto :usage

:usage
echo Local %MODULE% %VERSION% ^(library - build / test / coverage / install^)
echo.
echo   run-local.bat init      package the JAR ^(skip tests^)
echo   run-local.bat test      run unit tests
echo   run-local.bat coverage  tests + JaCoCo report + %COVERAGE_MIN%%% gate, prints summary
echo   run-local.bat install   install JAR into %MAVEN_REPO%
echo   run-local.bat javadoc   generate API docs ^(target\reports\apidocs^)
echo   run-local.bat deps      dependency tree -^> .local\logs\deps.txt
echo   run-local.bat sonar     mvn verify -Psonar ^(needs SONAR_TOKEN, SONAR_ORG^)
echo   run-local.bat clean     remove target\ and .local\
echo   run-local.bat all       init + coverage + install
echo.
echo Optional: MAVEN_REPO MVN_ARGS SONAR_TOKEN SONAR_ORG
call :print_artifacts
exit /b 1

:ensure_dirs
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
exit /b 0

:print_artifacts
echo.
echo %MODULE%  version=%VERSION%  kernel-core=%KERNEL_CORE_VERSION%
echo   jar         target\%MODULE%-%VERSION%.jar
echo   fat jar     target\%MODULE%-%VERSION%-jar-with-dependencies.jar
echo   coverage    target\site\jacoco\index.html
echo   javadoc     target\reports\apidocs\index.html
echo   maven       io.mosip.demosdk:%MODULE%:%VERSION%
echo.
exit /b 0

:check_prereqs
where java >nul 2>&1
if errorlevel 1 (
  echo error: java is required on PATH
  exit /b 1
)
where mvn >nul 2>&1
if errorlevel 1 (
  echo error: mvn is required on PATH
  exit /b 1
)
for /f "tokens=*" %%V in ('java -version 2^>^&1 ^| findstr /I "version"') do set "JAVA_VER=%%V"
echo java: !JAVA_VER!
echo !JAVA_VER! | findstr /R /C:"\"21[\. \"]" >nul
if errorlevel 1 echo warn: JDK 21 is required. Continuing anyway.
if not exist "%MAVEN_REPO%\io\mosip\kernel\kernel-core\%KERNEL_CORE_VERSION%" (
  echo warn: kernel-core %KERNEL_CORE_VERSION% not in %MAVEN_REPO%.
  echo       Maven will try Central snapshots; or build it first:
  echo       cd ..\..\commons\kernel\kernel-core ^&^& mvn clean install -Dgpg.skip=true -DskipTests
)
exit /b 0

:mvn_mod
pushd "%MODULE_DIR%"
call mvn -B %* %REPO_ARG% %MVN_ARGS%
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%

:print_coverage
if not exist "%JACOCO_CSV%" (
  echo warn: no JaCoCo report at %JACOCO_CSV%
  exit /b 0
)
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$rows = Import-Csv '%JACOCO_CSV%';" ^
  "function P($c,$m){ if(($c+$m) -eq 0){ 'n/a' } else { '{0}%%' -f [int](100*$c/($c+$m)) } };" ^
  "foreach($r in $rows){ '  {0,-20} line {1,4}  branch {2,4}' -f $r.CLASS,(P ([int]$r.LINE_COVERED) ([int]$r.LINE_MISSED)),(P ([int]$r.BRANCH_COVERED) ([int]$r.BRANCH_MISSED)) };" ^
  "$s = { param($n) ($rows | Measure-Object -Property $n -Sum).Sum };" ^
  "'  {0,-20} line {1,4}  branch {2,4}  instruction {3,4}' -f 'TOTAL',(P (& $s LINE_COVERED) (& $s LINE_MISSED)),(P (& $s BRANCH_COVERED) (& $s BRANCH_MISSED)),(P (& $s INSTRUCTION_COVERED) (& $s INSTRUCTION_MISSED))"
echo   report  %JACOCO_HTML%
exit /b 0

:init
call :check_prereqs
if errorlevel 1 exit /b 1
echo ==^> packaging %MODULE% ^(skip tests^)
call :mvn_mod clean package -DskipTests %MVN_SKIP%
if errorlevel 1 exit /b 1
echo init complete
call :print_artifacts
exit /b 0

:test
call :check_prereqs
if errorlevel 1 exit /b 1
echo ==^> maven tests
call :mvn_mod test %MVN_SKIP%
exit /b %ERRORLEVEL%

:coverage
call :check_prereqs
if errorlevel 1 exit /b 1
echo ==^> tests + JaCoCo ^(gate %COVERAGE_MIN%%%^)
call :mvn_mod clean verify %MVN_SKIP%
set "COV_RC=%ERRORLEVEL%"
echo.
echo coverage ^(excludes **/config/**^)
call :print_coverage
exit /b %COV_RC%

:install
call :check_prereqs
if errorlevel 1 exit /b 1
echo ==^> installing %MODULE% %VERSION% into %MAVEN_REPO%
call :mvn_mod clean install %MVN_SKIP%
if errorlevel 1 exit /b 1
echo installed io.mosip.demosdk:%MODULE%:%VERSION%
exit /b 0

:javadoc
call :check_prereqs
if errorlevel 1 exit /b 1
echo ==^> javadoc
call :mvn_mod javadoc:javadoc
if errorlevel 1 exit /b 1
echo javadoc  %MODULE_DIR%\target\reports\apidocs\index.html
exit /b 0

:deps
call :check_prereqs
if errorlevel 1 exit /b 1
call :ensure_dirs
echo ==^> dependency tree
call :mvn_mod dependency:tree "-DoutputFile=%LOG_DIR%\deps.txt"
if errorlevel 1 exit /b 1
echo deps  %LOG_DIR%\deps.txt
exit /b 0

:sonar
call :check_prereqs
if errorlevel 1 exit /b 1
if not defined SONAR_TOKEN (
  echo error: SONAR_TOKEN is required for sonar
  exit /b 1
)
set "SONAR_ARGS=-Psonar -Dsonar.token=%SONAR_TOKEN%"
if defined SONAR_ORG set "SONAR_ARGS=%SONAR_ARGS% -Dsonar.organization=%SONAR_ORG%"
echo ==^> sonar analysis
call :mvn_mod clean verify %MVN_SKIP% %SONAR_ARGS%
exit /b %ERRORLEVEL%

:clean
echo ==^> cleaning target\ and .local\
if exist "%MODULE_DIR%\target" rmdir /s /q "%MODULE_DIR%\target"
if exist "%LOCAL_DIR%" rmdir /s /q "%LOCAL_DIR%"
echo clean.
exit /b 0

:all
echo ==^> all: init + coverage + install
call :init
if errorlevel 1 exit /b 1
call :coverage
if errorlevel 1 exit /b 1
call :install
exit /b %ERRORLEVEL%
