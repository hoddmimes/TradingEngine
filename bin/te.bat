@echo off
:test
if "%TE_VERSION%" NEQ "" (goto go)
set /p TE_VERSION="Enter TE version (x.y.z): "
goto test

:go
rem -- get the bin location where this script is located
set BINDIR="%~dp0"
rem -- push current location
pushd "%cd%"
rem -- set default to the Modum working dir
cd %BINDIR%/..
set BASEDIR=%BINDIR%/..
set BASEDIR=%BASEDIR:\=/%

SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-engine/build/libs/te-%TE_VERSION%.jar com.hoddmimes.te.TradingEngine file:///%BASEDIR%/configuration/TeConfiguration.json
popd