@echo off
rem -- get the bin location where this script is located
set BINDIR="%~dp0"
rem -- push current location

pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Djava.net.preferIPv4Stack=true
start /B java %JAVA-SWITCHES% -cp ./meta-client/build/libs/teguitestapp-1.1.0.jar  com.hoddmimes.te.client.TeGuiTestApp
popd