@echo off

set BINDIR="%~dp0"
call %BINDIR%/version.bat


rem -- push current location
pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-engine/build/libs/te-%TE_VERSION%.jar  com.hoddmimes.te.management.gui.Trades -tradeDir ./logs/
popd