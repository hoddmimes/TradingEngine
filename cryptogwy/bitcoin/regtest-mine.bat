@ECHO OFF
rem - This command file will mine some coins to the "default" wallet
rem - The "default" wallet should have been created when running the bitcoin-qt program
rem - By default 1 block will be mine 




IF "%~1"=="" (
   set blocks=1
) ELSE (
  set blocks=%1
)

bitcoin-cli -regtest -rpcuser=rpc -rpcpassword=rpc -rpcwallet=default -generate %blocks%


