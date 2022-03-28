@echo off

set BINDIR="%~dp0"
call %BINDIR%/version.bat

pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
java %JAVA-SWITCHES% -cp ./meta-common/build/libs/teclient-%TE_VERSION%.jar  com.hoddmimes.te.testclient.TestClient configuration/ClientTestScript.json
popd