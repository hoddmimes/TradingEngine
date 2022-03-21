
# Crypto Gateway Design

This is a description of the _crypto gateway_, herby called CG design. The overall aim with the gateway acting 
as an gateway to a Bitcoon and/or Ethereum network and possibly other crypto coins. The CG will hold the clients Bitcoin and/or ETH deposits 
that they will be able to trade on the exchange.




## Overall Design.

The CG will have one _omni_ wallet per crypto coin that the clients have deposit to for trading at the exchange.
The _Position Controller_ component is actually the component that holds the real-time position of the coins, the CG would just act as a
depository.

The major reason for having a single _omni_ wallet per crypto currency is;
* to minimize the tranfer fees when moving coins to/from the exchange.
* minimize the latency in the settlement process. When  exchange participants trade with each 
  other we would **not** like settle i.e transfer coins between their wallet (if there had been individual wallets) due to the fee cost and the latecy
  in the Bitcoin and Ethereum networks. 
* Now the _Position Controller_ will maintain the acctual position for each crypto curreny and when a client would like to redraw
  coins the amount is verified agains the _Position Controller_.



Each client that would like to trade crypto currencies will have an entry in the CG (Mongo) databade.
In the database the current deposit amounts will be keept together with a transaction history of all deposit and re-draw 
transactions.


## Deposit Bitcoin 

* Before a client can deposit any bitcoins the client must apply for a deposit _(wallet)_ address. 
Technically that will be handed out by generate a unique address being associated with the Bitcoin _omni_ wallet. However
that does not prevent a user to send coins to the omni bitcoin wallet without having created an _deposit entry_. Such payment
will of cause be received but unidentified.
* A mail is sent to the client with the deposit address. An outstanding question is if a deposit address needs to be confirmed by the user? Currently 
a deposit enrtry gets confirmed when being created. _(Who would deposit coins to someone elses account?)_
* When the client deposit coins to the address being generate for client deposit the coins will show up in the Bitcoin _omni_ wallet defined and used by the CG.
* **_The CG controller will identify the client by the deposit address generated** It will also update the Position Controller
  with the additional holdings.

## Deposit ETH

* Before a client can deposit any bitcoins the client must register for a _source_ address i.e. the wallet address from where the ETH 
  will be sent. The TE ethereum wallet will have **one** unique credential address. Identifying clients by source address is a bit more risky since 
  any logged in client can register any address. Therefore ethereum deposit entries needs to be confirm by mail by the client.
* **_The CG controller will when receiving a payment identify the client by the source address** It will also update the Position Controller
  with the additional holdings.

## Re-drawn

Before as part of a redraw the client must register a wallet / credential address to where the coins should be send. Th request must be confirmed via mail before the 
redrawn will take place.

When the request should be executed amount will be checked against the client position held by the _Position Controller_. This implies also consider what 
is being exposed in the market. I.e current position - sum of a client active sell orders for the client. 


# TE Implementation


_A few words about the TE Position Controller, herby a.k.a PC. The PC is used when when pre-trading is enabled. Buy orders are validated against
the client cash holding, minus the current buy exposure in the market when being submitted. Sell orders are validate against the current holdings, minus the sell exposure in the
market when being submitted._



How the pre-validation should work is likely subject for discussion and adaptation. It to a large extent depends on how/when 
client positions are updated. One a day, periodically in real-time etc.

A simple implemenation would be to update the client _holdings_ once every 24 hrs. The client then trade within these holdings / limits for the next 24 hrs.
This however a have a significant down side. The client is trading _blind_ considering what is happenings outside the trading system. Even if many client depo system
use to work in this way it is associated with some risks. 

A more optimal approach is of cause if the _true holder_ (or the collateral system) updated the TE when there is a change.
In practice the bank would updated the the TE (i.e. PC) when cash was deposit / redrawn or when equities was deposit in 
the TE depo. And when crypto was deposit / redrwan from the TE system.

The current implementation have applied a sort of semi realtime approach;

**For Equities**, holdings are loaded from the file _./configuration/Positions.json_ every time the system is started. 
Any trade for during the current date will applied and update the (loaded) position. I.e more a updated once every 24 hrs.
However the PC interface have methods for externaly updating the holdings in real-time. 


**Crypto** will work a bit differently it is more of a real-time approach due to the fact that we have implemented a Crypto Gateway.
- The crypto gateway component will when starting, synch wil the crypto network and get the current "wallet" positions.
- The current crpto holdings are then updating the Position Controller as part of the startup.
- Any deposit of crypto asset (updateing the CG) during the day will update PC.
- Any redraw of crypto assets will be validated against the position in PC, considering active crypto selling orders. In case
  crypto is available the redrawn request will be accepted if not sufficient offering is available the request is rejected.




**_The rest is in the code. But as said the the whole pre-validation / position handling is subject for adaptation it all depends on the 
surrounding echo systems_**


## Data Structures

A few words about the some of the essential dat structures used for controlling the cryto flows


* **_AccountPosition_**, holds a real-time view of the asset iventory. Updated as execution takes place and crypto assests are deposit / redrawn. The AccountPosition is for equities loaded when the system is started by loading
the file _./configuration/Positions.json_ Any trades done in any previous session for same day will update the inventory also as part of the startup.

* For crypto assests the current crypto position for the account is updated as part of the startup, after the crypto gateway have synchronized with respective coin network.

* Any deposit / redraw of crypto assets will update the _AccountPosition_ as the happens.
* Any execution taking place in the TE will update the AccountPosition as they happen.


**DBCryptoDepositEntry** is DB structure holding information about deposit / redrawn addresses being defined by clients.
Clients will not benifit from deposits and cannot redraw coins unless there is a matching DBCrytoDepositEntry matching an incoming deposit transaction or 
redrawn request from the client.

**DBCryptoDeposit** holds the crypto inventory for a client/account. Updates when 
* TE execution takes place.
* When the client deposit / redraw coins.

**DBCryptoPayment** used for logging incoming and outcoing crypto payments.


# Setting Up Test Environmemnts 

Currently private crypto network has been used for testing.
The following section describes how to setup and run a private _Bitcoin_ and _Etherreum_ crypto network.

## Bitcoin

Inorder setup and run a Bitcoin network the [Bitcoin core platform](https://bitcoin.org/en/bitcoin-core/) is used.
The _core_ platform contains software to setup and run a one node network. You should download and install the core 
component.

There are essentially two components in  core being of interest.

* The [**_bitcoin-qt_**](https://river.com/learn/terms/b/bitcoin-qt/) application. The application is a GUI app providing a
one node network and an interface for managing wallets. You have to have this application running for having a _test_ network.
* Part of the _core_ platform is also the **_bitcoin-cli_** application. This application is required when simulating mining in the network.
 But via the command interface a [lot of things can be done](https://linuxcommandlibrary.com/man/bitcoin-cli)

The following steps are what is typically done to get the network up and allow the TE crypto gateway to connect to the network.

* Some script are located in the directory _./cryptogwy/bitcoin_ these script will make the setup easier
* the file _regtest-qt.bat_ (win) or _regtest-qt.sh_ (linux) will run the _bitcoin-qt_ application in test mode. The script requires that executable bitcoin-qt is in the PATH.

* Run the script _reqtest-qt.[sh|bat]_.
* In the bitcoin-qt application create three wallets: _default, USER01, USER02_
* Run the script _regtest-mine.[sh|bat]_. with the parameter _101_ It will mine 101 blocks and the mining reward will be placed in the _default_ wallet.
* Start the TE Trading Engine server application _./bin/te.[bat|sh]
* Run the TE management GUI application _./bin/management.[bat|sh]


You can from the _bitcoin-qt_ application send bitcoins to a user account. The _default_ wallet has some coins.
The first thing is to create a deposit entry by issue a TE API rest call **_addDepositEntry_**. Then you can transfer coints from the 
_bitcoin-qt_ application to the address returned in the **_addDepositEntry_** response.

Any transaction issued in the _addDepositEntry_ application must be confirmed i.e. mined. This is accomplished by mining a block, run the script
_regtest-mine.[sh|bat]_ with  the parameter _1_ i.e. mine one block.






