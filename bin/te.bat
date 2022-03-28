@echo off

set BINDIR="%~dp0"
call %BINDIR%/version.bat


pushd "%cd%"
rem -- set default to the Modum working dir
cd %BINDIR%/..
set BASEDIR=%BINDIR%/..
set BASEDIR=%BASEDIR:\=/%

SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-engine/build/libs/te-%TE_VERSION%.jar com.hoddmimes.te.TradingEngine file:///%BASEDIR%/configuration/TeConfiguration.json
popd