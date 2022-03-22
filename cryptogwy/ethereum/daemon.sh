#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export ETH_DATA_DIR=$DIR/geth-dev-data

if [-d $ETH_DATA_DIR ]; then
  rm -Rf -p $ETH_DATA_DIR
fi
mkdir -p $ETH_DATA_DIR
cd $ETH_DATA_DIR

echo test>pwd.pwd
geth --datadir $ETH_DATA_DIR  account new --password ./pwd.pwd
geth --datadir $ETH_DATA_DIR  account new --password ./pwd.pwd

export ACCOUNT1=$(geth --datadir ./ account list | gawk 'match($0,/Account #0: {(.+)}/, a) {print a[1]}')
export ACCOUNT2=$(geth --datadir ./ account list | gawk 'match($0,/Account #1: {(.+)}/, a) {print a[1]}')



echo { > genesis.json
echo    \"config\": { >> genesis.json
echo       \"chainId\": 1959, >> genesis.json
echo       \"homesteadBlock\": 0, >> genesis.json
echo       \"eip150Block\": 0, >> genesis.json
echo       \"eip155Block\": 0, >> genesis.json
echo       \"eip158Block\": 0, >> genesis.json
echo       \"byzantiumBlock\": 0, >> genesis.json
echo       \"constantinopleBlock\": 0, >> genesis.json
echo       \"petersburgBlock\": 0, >> genesis.json
echo       \"ethash\": {} >> genesis.json
echo     }, >> genesis.json
echo    \"coinbase\" : \"0x0000000000000000000000000000000000000000\", >> genesis.json
echo    \"mixhash\" : \"0x0000000000000000000000000000000000000000000000000000000000000000\", >> genesis.json
echo    \"parentHash\" : \"0x0000000000000000000000000000000000000000000000000000000000000000\", >> genesis.json
echo    \"difficulty\": \"200\", >> genesis.json
echo    \"gasLimit\": \"9180211\", >> genesis.json
echo    \"alloc\": { >> genesis.json
echo    \"$ACCOUNT1\": { \"balance\": \"333300000000000000000\" }, >> genesis.json
echo    \"$ACCOUNT2\": { \"balance\": \"000000000000000000000\" } >> genesis.json
echo  } >> genesis.json
echo } >> genesis.json



geth init --datadir $ETH_DATA_DIR genesis.json

geth --datadir $ETH_DATA_DIR --networkid 1959 --verbosity 4 --http --nodiscover --rpc.allow-unprotected-txs --allow-insecure-unlock --http.api "admin,eth,debug,miner,net,txpool,personal,web3" --mine --miner.threads 1 --miner.gasprice 1000





