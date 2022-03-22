#!/bin/bash
#
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if type -p java; then
    export _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    export _java="$JAVA_HOME/bin/java"
else
    echo "java not found in execution path and JAVA_HOME not set"
    exit
fi

if  [ -z $TE_VERSION ]; then
  read -p "TE version (x.y.z) : " TE_VERSION
fi



pushd ./
cd $DIR/..


if [ ! -d ./cryptogwy/bitcoin/regtest ]; then
  mkdir ./cryptogwy/bitcoin/regtest;
fi
if [ ! -d ./cryptogwy/ethereum/geth-dev-data/keystore ]; then
  mkdir  ./cryptogwy/ethereum/geth-dev-data/keystore;
fi


JAVA_SWITCHES="-Dlog4j.configurationFile=$DIR/../configuration/log4j2-te.xml -Djava.net.preferIPv4Stack=true"
$_java $JAVA_SWITCHES -cp $DIR/../meta-engine/build/libs/te-$TE_VERSION.jar com.hoddmimes.te.TradingEngine file:///$DIR/../configuration/TeConfiguration.json
