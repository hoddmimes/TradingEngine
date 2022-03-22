#!/bin/bash
#
if [ ! -d ./qt-daemon-data ]; then
  mkdir -p ./qt-daemon-data;
fi

bitcoin-qt -regtest -datadir=./qt-daemon-data -server=1 -listen=1 -listenonion=0  -debug -fallbackfee=0.0001 -peerbloomfilters=1 -rpcuser=rpc -rpcpassword=rpc -walletbroadcast


