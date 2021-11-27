rem -- get the bin location where this script is located
set BINDIR="%~dp0"
rem -- push current location
pushd "%cd%"
rem -- set default to the TE working dir
cd %BINDIR%/..
SET JAVA-SWITCHES=-Dlog4j.configurationFile=./configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true
start /B java %JAVA-SWITCHES% -cp ./meta-engine/build/libs/te-1.0.jar  com.hoddmimes.te.management.gui.Account -accountdb ./configuration/AccountDefinitions.json
popd