@ECHO OFF

cd  %HOMEPATH%\source\TradingEngine\cryptogwy\ethereum
set ETH_DATADIR=%HOMEPATH%\source\TradingEngine\cryptogwy\ethereum\geth-dev-data

if exist %ETH_DATADIR% (rmdir /S /Q %ETH_DATADIR%)
mkdir %ETH_DATADIR%
cd %ETH_DATADIR%

echo test>pwd.pwd
geth --datadir %ETH_DATADIR%  account new --password .\pwd.pwd


for /f "tokens=2 delims={}" %%a in ('geth --datadir .\ account list') do (set ACCOUNT=%%a)

echo { > genesis.json
echo    ^"config^": { >> genesis.json
echo       ^"chainId^": 1959, >> genesis.json
echo       ^"homesteadBlock^": 0, >> genesis.json
echo       ^"eip150Block^": 0, >> genesis.json
echo       ^"eip155Block^": 0, >> genesis.json
echo       ^"eip158Block^": 0, >> genesis.json
echo       ^"byzantiumBlock^": 0, >> genesis.json
echo       ^"constantinopleBlock^": 0, >> genesis.json
echo       ^"petersburgBlock^": 0, >> genesis.json
echo       ^"ethash^": {} >> genesis.json
echo     }, >> genesis.json
echo    ^"difficulty^": ^"200^", >> genesis.json
echo    ^"gasLimit^": ^"9180211^", >> genesis.json
echo    ^"alloc^": { >> genesis.json
echo    ^"%ACCOUNT%^": { ^"balance^": ^"333300000000000000000^" } >> genesis.json
echo  } >> genesis.json
echo } >> genesis.json

geth init --datadir $ETH_DATADIR genesis.json

geth --datadir $ETH_DATADIR --networkid 1959 --verbosity 4 --http --nodiscover --rpc.allow-unprotected-txs --allow-insecure-unlock --http.api "admin,eth,debug,miner,net,txpool,personal,web3" --mine --miner.threads 1 --miner.gasprice 1000





