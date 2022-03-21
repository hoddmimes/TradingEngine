@echo off
:test
if "%TE_VERSION%" NEQ "" (goto go)
set /p TE_VERSION="Enter TE version (x.y.z): "
goto test

:go
@echo off
rem -- get the bin location where this script is located
set BINDIR="%~dp0"
rem -- push current location

pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-client/build/libs/teguitestapp-%TE_VERSION%.jar  com.hoddmimes.te.client.TeGuiTestApp -account TEST -password test
popd