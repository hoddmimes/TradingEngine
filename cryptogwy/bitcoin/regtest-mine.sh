#!/bin/bash

if [ -z $1 ]; then
  export BLOCKS=1
else
  export BLOCKS=$1
fi

bitcoin-cli -regtest -rpcuser=rpc -rpcpassword=rpc -rpcwallet=default -generate $BLOCKS
