
# Crypto Gateway Design

This is a description of the _crypto gateway_, herby called CG design. The overall aim with the gateway acting 
as an gateway to a Bitcoon and/or Ethereum network. The CG will hold the clients Bitcoin and/or ETH deposits 
that they will be able to trade on the exchange.


Clients are able to deposit or redraw coins from the exchange. When doing so the GC is the components providing the 
deposit logic and doing the interaction with the Bitcoi and Ethereum network.

## Overall Design.

The CG will have one _omni_ wallet for Bitcoin and one for ETH holding all coins that the clients have deposit for trading at the exchange.
The _Position Controller_ component is actually the component that holds the real-time position of the coins, the CG would just act as a
depository.

The major reason for having a single _omni_ wallet per crypto currency is;
* to minimize the tranfer fees when moving coins to/from the exchange.
* is minimize the latency in the settlement process. When to exchange participants trade with each 
  other we would not like settle i.e transfer coins between their wallet due to the fee cost and the latecy
  in the Bitcoin and Ethereum networks. 
* Now the _Position Controller_ will maintain the acctual position for each crypto curreny and when a client would like to redraw
  coins the amount is verified agains the _Position Controller_.



Each client that would like to trade crypto currencies will have an entry in the CG (Mongo) databade.
In the database the current deposit amounts will be keept together with a transaction history of all deposit and re-draw 
transactions.


## Deposit Bitcoin 

* Before a client can deposit any bitcoins the client must apply for a deposit _(wallet)_ address. 
Techically that will be handed out by generate a unique address being associated with the Bitcoin _omni_ wallet. However
that does not prevent a user to send coins to the omni bitcoin wallet without having created an _deposit entry_. Such payment
will of cause be received but unidentified.
* A mail is sent to the client with the deposit address. An outstanding question is if a deposit address needs to be confirmed by the user? Currently 
a deposit enrtry gets confirmed when being created.
* When the client deposit coins to the address being generate for client deposit the coins will show up in the Bitcoin _omni_ wallet defined and used by the CG.
* **_The CG controller will identify the client by the deposit address generated** It will also update the Position Controller
  with the additional holdings.

## Deposit ETH

* Before a client can deposit any bitcoins the client must apply for a register _source_ address i.e. the wallet address from where the ETH 
  will be sent. Each Ethereum wallet will have a unique credential address.
* A mail is sent to the client with the address being registered. Furthermore the client has to confirm that it has received
  the address.
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

A simplicit implemenation would be to update the client _holdings_ once every 24 hrs. The client then trade within these holdings / limits for the next 24 hrs.
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
- Any re-draw of crypto assets will be validated against the position in PC, considering active crypto selling orders. In case
  crypto is available re-drawn will be accepted if not sufficient offering is available the request is rejected.




**_The rest is in the code. But as said the the whole pre-validation / position handling is subject for adaptation it all depends on the 
surrounding echo systems_**





