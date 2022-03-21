/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoddmimes.te.client;

import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.messages.SID;
import com.hoddmimes.te.messages.generated.Market;
import com.hoddmimes.te.messages.generated.Symbol;
import org.bitcoinj.core.Coin;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class CryptoScaling
{
	Connector mConnector;

	public CryptoScaling( Connector pConnector ) {
		mConnector = pConnector;
	}

	private Market getCryptoMarket() {
		for( Market m : mConnector.getMarkets()) {
			if (m.getIsCryptoMarket().get()) {
				return m;
			}
		}
		throw new RuntimeException(" Crypto market not defined / found ");
	}

	private Symbol getCryptoSymbol( Crypto.CoinType pCoinType ) {
		SID tSID = new SID(getCryptoMarket().getId().get(), pCoinType.name() );
		Symbol tSymbol = mConnector.getInstrument( tSID.toString() );
		if (tSymbol == null) {
			throw new RuntimeException(" Crypto symbol \"" + pCoinType + "\" not defined / found ");
		}
		return tSymbol;
	}


	public long coinToNominator( Crypto.CoinType pCoinType, double pCoinAmount) {
		if (pCoinType == Crypto.CoinType.BTC) {
			return BTCToSatoshi(pCoinAmount);
		}
		if (pCoinType == Crypto.CoinType.ETH) {
			return ETHToWei(pCoinAmount);
		}
		throw new RuntimeException(" Undefined coin type: " + pCoinType.name());
	}

	private long BTCToSatoshi( double pBTCAmount ) {
		Coin tCoin = Coin.ofBtc( new BigDecimal( pBTCAmount ));
		return tCoin.longValue();
	}

	private long ETHToWei( double pETHAmount ) {
		return Convert.toWei(new BigDecimal( pETHAmount), Convert.Unit.ETHER ).longValue();
	}



	public long scaleFromOutsideNotation(Crypto.CoinType pCoinType, long pOutSidePosition )
	{
		Market tCryptoMarket = getCryptoMarket();
		Symbol tCryptoSymbol = getCryptoSymbol( pCoinType );
		long tScalingFactor = (tCryptoSymbol.getScaleFactor().isPresent()) ? tCryptoSymbol.getScaleFactor().get() : tCryptoMarket.getScaleFactor().get();
		return ( pOutSidePosition / tScalingFactor);
	}

	public long scaleToOutsideNotation( Crypto.CoinType pCoinType,   long pTeInternalPosition ) {
		Market tCryptoMarket = getCryptoMarket();
		Symbol tCryptoSymbol = getCryptoSymbol( pCoinType );
		long tScalingFactor = (tCryptoSymbol.getScaleFactor().isPresent()) ? tCryptoSymbol.getScaleFactor().get() : tCryptoMarket.getScaleFactor().get();
		return ( pTeInternalPosition  * tScalingFactor);
	}


	public String coinToFriendlyString( Crypto.CoinType pCoinType, long pOutsideAmount ) {
		if (pCoinType == Crypto.CoinType.BTC) {
			return BTCToFriendlyString(pOutsideAmount);
		}
		if (pCoinType == Crypto.CoinType.ETH) {
			return ETHToFriendlyString(pOutsideAmount);
		}
		throw new RuntimeException(" Undefined coin type: " + pCoinType.name());
	}

	private String BTCToFriendlyString( long pSatoshiAmount ) {
		Coin tCoin = Coin.valueOf(pSatoshiAmount);
		return tCoin.toFriendlyString();
	}

	private String  ETHToFriendlyString( long pWeiAmount ) {
			BigDecimal bigdec = new BigDecimal( pWeiAmount );
			return Convert.fromWei(bigdec, Convert.Unit.ETHER ).toString()  + " Eth";
	}



}
