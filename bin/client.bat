rem -- get the bin location where this script is located
set BINDIR="%~dp0"
rem -- push current location
pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
start /B java %JAVA-SWITCHES% -cp ./meta-common/build/libs/teclient-1.1.jar  com.hoddmimes.te.testclient.TestClient configuration/ClientTestScript.json
popd