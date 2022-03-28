@echo off

set BINDIR="%~dp0"
call %BINDIR%/version.bat

rem -- get the bin location where this script is located

pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-client/build/libs/teguitestapp-%TE_VERSION%.jar  com.hoddmimes.te.client.TeGuiTestApp -account TEST -password test
popd