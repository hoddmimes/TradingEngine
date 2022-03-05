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

package com.hoddmimes.te.cryptogwy;

import com.google.gson.JsonObject;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.Crypto;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.instrumentctl.SymbolX;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bitcoinj.core.Coin;
import org.web3j.crypto.*;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EthereumGwy extends CoinGateway
{
	private static final long GAZ_WEI_MULTIPLIER = 100000000000L;
	private static final long CHANID = 1959;

	private Logger mLog = LogManager.getLogger( EthereumGwy.class);
	private Credentials mWallet;
	private String mDataDir;
	private String mNetHttpService;
	private Web3j mWeb3j;
	private long mGasLimit, mGasPrice, mGasTip;


	public EthereumGwy(JsonObject pTeConfig, TEDB pDb ) {
		super( pDb, Crypto.CoinType.ETH );
		parseConfiguration(AuxJson.navigateObject(pTeConfig,"TeConfiguration/cryptoGateway/ethereum"));
		loadWallet();
		connectToNetwork();
		setupSubscription();

	}

	public GetDepositEntryResponse addDepositEntry(GetDepositEntryRequest pEntryRequest)  {
		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setPaymentType(Crypto.PaymentType.DEPOSIT.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( pEntryRequest.getFromAddress().get() );
		cpe.setConfirmationId( getConfirmationId() );
		cpe.setConfirmed( true );

		mDb.insertDbCryptoPaymentEntry( cpe );

		GetDepositEntryResponse tResponse = new GetDepositEntryResponse();
		tResponse.setAddress( mWallet.getAddress().toString() );
		//tResponse.setConfirmationId( cpe.getConfirmationId().get());
		tResponse.setCoin( pEntryRequest.getCoin().get() );
		return tResponse;
	}


	public SetRedrawEntryResponse addRedrawEntry( SetRedrawEntryRequest pEntryRequest)  {

		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setPaymentType(Crypto.PaymentType.REDRAW.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( pEntryRequest.getAddress().get() );
		cpe.setConfirmationId( getConfirmationId() );
		cpe.setConfirmed( false );
		mDb.insertDbCryptoPaymentEntry( cpe );

		long tNow = System.currentTimeMillis();

		DbConfirmation tConfirmation = new DbConfirmation();
		tConfirmation.setConfirmationId( cpe.getConfirmationId().get());
		tConfirmation.setConfirmationType( Crypto.ConfirmationType.PAYMENT.name() );
		tConfirmation.setTime( SDF.format( tNow));
		tConfirmation.setBinTime(tNow);
		tConfirmation.setAccount(pEntryRequest.getAccountId().get());
		mDb.insertDbConfirmation( tConfirmation );

		//Todo: send mail to user

		SetRedrawEntryResponse tResponse = new SetRedrawEntryResponse();
		tResponse.setAddress( pEntryRequest.getAddress().get() );
		tResponse.setStatusText( "confirmation link will be mailed to your mail account");
		tResponse.setCoin( pEntryRequest.getCoin().get() );
		return tResponse;
	}





	String walletToString() {
		try {

			EthGetBalance tWeiBalance = mWeb3j.ethGetBalance(mWallet.getAddress(), DefaultBlockParameterName.LATEST).send();
			String tEtherBalance = (tWeiBalance.getBalance().compareTo(BigInteger.ZERO) > 0) ?  weiToFriendlyString( tWeiBalance.getBalance().longValue() ) :  new String("0.00");
			JsonObject jWallet = new JsonObject();
			JsonObject jData = new JsonObject();

			jData.addProperty("address", mWallet.getAddress());
			jData.addProperty("pubKey",mWallet.getEcKeyPair().getPublicKey().toString());
			jData.addProperty("wei balance", tWeiBalance.getBalance().longValue());
			jData.addProperty( "eth balance", tEtherBalance);

			jWallet.add("TE Eth Wallet", jData);
			return jWallet.toString();
		}
		catch( Exception e) {
			return e.getMessage();
		}
	}

	private void parseConfiguration(JsonObject pEthereumConfig) {
		mDataDir = AuxJson.navigateString( pEthereumConfig, "dataDir");
		mGasPrice = AuxJson.navigateLong( pEthereumConfig, "gasPrice");
		mGasLimit = AuxJson.navigateLong( pEthereumConfig, "gasLimit");
		mGasTip = AuxJson.navigateLong( pEthereumConfig, "gasTip", 0L);


		mNetHttpService = AuxJson.navigateString( pEthereumConfig, "httpService");
	}

	private void setupSubscription() {
		mWeb3j.transactionFlowable().subscribe(tx -> {

			Request<?, EthTransaction> tTx = mWeb3j.ethGetTransactionByHash(tx.getHash());

			if (tx.getFrom().contentEquals(mWallet.getAddress())) {
				walletPaymentOut( tx );
			}
			if (tx.getTo().contentEquals(mWallet.getAddress())) {
				walletPaymentIn( tx );
			}
		});
	}

	/**
	 * Invoked when a redrawn payment has been confirmed. In normal cases the redrawn should have been accepted
	 * If an error occurred, the amount should not have been sent but the gased has been consumed. So undo the position update
	 * that occurred when the payment was sent and update the position for the gas cost.
	 * @param tx
	 */
	private void walletPaymentOut( Transaction tx ) {
		TransactionReceipt  tTxReceipt = null;
		SymbolX tEthSymbol = TeAppCntx.getInstance().getInstrumentContainer().getCryptoInstrument( Crypto.CoinType.ETH.name());

		// Locale the account for which this payment is
		DbCryptoPaymentEntry tPaymentEntry = (DbCryptoPaymentEntry) TEDB.dbEntryFound(mDb.findPaymentEntryByAddressdPaymentTypeCoinType( tx.getTo(),  Crypto.PaymentType.REDRAW, Crypto.CoinType.ETH));
		if (tPaymentEntry == null) {
			mLog.fatal("A redraw without a defined destination, should never, never, never occur!!!");
			System.exit(0);
		}

		try {
			EthGetTransactionReceipt tEthGetTransactionReceiptResp = mWeb3j.ethGetTransactionReceipt(tx.getHash()).send();
			tTxReceipt = tEthGetTransactionReceiptResp.getTransactionReceipt().get();
			mLog.info("[walletPaymentOut] (txreceipt) \n" + tTxReceipt.toString());
		}
		catch( IOException e) {
			// Not likely to happen. We would not invoke this method if we where not connected to the eth client.
			mLog.error("[walletPaymentOut] could not request tx receipt reason: " + e.getMessage(), e );
			return;
		}


		if (tTxReceipt.isStatusOK()) {
			mLog.info("[walletPaymentOut] account: " + tPaymentEntry.getAccountId().get() + " address: " + tx.toString() +
					" amout: " + weiToFriendlyString(tx.getValue()) + " txid: " + tx.getHash());

			DbCryptoPayment tPayment = mDb.findPaymentEntryByTxid( tx.getHash().toString());
			tPayment.setTime(SDF.format(System.currentTimeMillis()));
			tPayment.setState(Crypto.StateType.CONFIRM.name());
			mDb.updateDbCryptoPayment(tPayment, false);

			// The position and deposit is updated when the payment tx is issued

			long tGasUsed = tEthSymbol.scaleFromOutsideNotation( tTxReceipt.getGasUsed().longValue() * GAZ_WEI_MULTIPLIER) ; // to get in wei
			long tWeiGasedUsed = (tTxReceipt.getGasUsed().longValue() * GAZ_WEI_MULTIPLIER);
			long tCorrectAmountWei = getEstimatedGasCost() - tWeiGasedUsed;
			long tTeCorrectedAmount = tEthSymbol.scaleFromOutsideNotation( tCorrectAmountWei );
			TeAppCntx.getPositionController().updateUpdateCryptoPosition(tPaymentEntry.getAccountId().get(), tEthSymbol.getSid().get(), tTeCorrectedAmount, tx.getHash());

			//todo: update position holding with gased used
		} else {
			mLog.error("redrawn failed, reason: " + tTxReceipt.getStatus());
			long tWeiGasedUsed = (tTxReceipt.getGasUsed().longValue() * GAZ_WEI_MULTIPLIER);
			long tCorrectAmountWei = tx.getValue().longValue() - (tTxReceipt.getGasUsed().longValue() * GAZ_WEI_MULTIPLIER);
			long tTeCorrectedAmount = tEthSymbol.scaleFromOutsideNotation( tCorrectAmountWei );
			TeAppCntx.getPositionController().updateUpdateCryptoPosition(tPaymentEntry.getAccountId().get(), tEthSymbol.getSid().get(), tTeCorrectedAmount, tx.getHash());
		}
	}

	public long getEstimatedGasCost() {
		return GAZ_WEI_MULTIPLIER * (mGasLimit * (mGasPrice + mGasTip));
	}

	public String sendCoins(CryptoRedrawRequest pCryptoRedrawRequest ) throws TeException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream pos = new PrintStream(baos);

		SymbolX tSymbol = TeAppCntx.getInstance().getInstrumentContainer().getCryptoInstrument( Crypto.CoinType.ETH.name());
		long tAmount = tSymbol.scaleToOutsideNotation( pCryptoRedrawRequest.getAmount().get());

		EthGetTransactionCount tEthGetTransactionCount = null;
		try {
			tEthGetTransactionCount = mWeb3j.ethGetTransactionCount(mWallet.getAddress(), DefaultBlockParameterName.LATEST).send();
		} catch (IOException e) {
			mLog.error("failed to send ETH get latest block, reason: " + e.getMessage());
			throw new TeException(StatusMessageBuilder.error("failed to send ETH, reason: " + e.getMessage(), null));
		}
		   BigInteger tNonce = tEthGetTransactionCount.getTransactionCount();

			BigInteger tWeiAmount = BigInteger.valueOf(tAmount);

			BigInteger tGasLimit = BigInteger.valueOf(mGasLimit);
			BigInteger tGasPrice = BigInteger.valueOf(mGasPrice + mGasTip);


			RawTransaction tRawTransaction = RawTransaction.createEtherTransaction(
					tNonce,
					tGasPrice,
					tGasLimit,
					pCryptoRedrawRequest.getAddress().get(),
					tWeiAmount);

			byte[] tSignedMessage = TransactionEncoder.signMessage(tRawTransaction, CHANID, mWallet);
			String tHexMsg = Numeric.toHexString(tSignedMessage);


		EthSendTransaction tEthXtaTx = null;
		try {tEthXtaTx = mWeb3j.ethSendRawTransaction(tHexMsg).send();}
		catch (IOException e) {
			mLog.error("failed to send ETH transaction, reason: " + e.getMessage());
			throw new TeException(StatusMessageBuilder.error("failed to send ETH transaction, reason: " + e.getMessage(), null));
		}

		if (tEthXtaTx.hasError()) {
				throw new TeException(StatusMessageBuilder.error("failed to send ETH coins, reason: " + tEthXtaTx.getError().getMessage(), null));
			}
			String tTxHash = tEthXtaTx.getTransactionHash();

			// Transaction has been received and sent to the ethernet network
		    // but it can still fail due to insufficient gas and maybe other reasons
		    // we would need the tx receipt before we can for certainty say that the payment when through

			DbCryptoPayment tPayment = new DbCryptoPayment();
			tPayment.setAccountId( pCryptoRedrawRequest.getAccountId().get());
			tPayment.setAmount( weiToFriendlyString( tAmount ));
			tPayment.setPaymentType( Crypto.PaymentType.REDRAW.name());
			tPayment.setTxid( tTxHash );
			tPayment.setAddress( pCryptoRedrawRequest.getAddress().get() );
			tPayment.setCoinType( Crypto.CoinType.ETH.name());
			tPayment.setState( Crypto.StateType.PENDING.name());
			tPayment.setTime( SDF.format( System.currentTimeMillis()));


			mLog.info("eth send tx-hash: " + tTxHash.toString() + " amout: " + weiToFriendlyString(tWeiAmount) + " to: " + pCryptoRedrawRequest.getAddress().get());
			return tTxHash;
	}

	@Override
	public long getEstimatedTxFeeNA() {
		long x = GAZ_WEI_MULTIPLIER * (mGasLimit * (mGasPrice + mGasTip));
		return super.mCoinInst.scaleFromOutsideNotation(x);
	}

	/**
	 * Invoked when a (outside) payment is received on the ethereum network. The payment has been confirmed
	 * and the amout should update the DbCryptoDeposit and the position controller holdings
	 *
	 * @param tx Etherum transaction
	 */
	private void walletPaymentIn( Transaction tx ) {

		// If everything is in order there should be a deposit payment entry defined. If not someone sent us coins
		// blindly, should not really happen but coins are received and logged

		DbCryptoPaymentEntry tPaymentEntry = (DbCryptoPaymentEntry) TEDB.dbEntryFound(mDb.findPaymentEntryByAddressdPaymentTypeCoinType(tx.getFrom(), Crypto.PaymentType.DEPOSIT, Crypto.CoinType.ETH));
		String tAccountId = (tPaymentEntry == null) ? null : tPaymentEntry.getAccountId().get();

		mLog.info("[walletPaymentIn] account: " + tAccountId + " address: " + tx.toString() +
				" amout: " + weiToFriendlyString(tx.getValue()) + " txid: " + tx.getHash());

		// Always log the crypto payment
		long tNow = System.currentTimeMillis();
		DbCryptoPayment tPayment = new DbCryptoPayment();
		tPayment.setPaymentType(Crypto.PaymentType.DEPOSIT.name());
		tPayment.setTxid(tx.getHash());
		tPayment.setTime(SDF.format(tNow));
		tPayment.setState(Crypto.StateType.CONFIRM.name());
		tPayment.setAddress(tx.getFrom()); // for ETH we identify the account / payment entry where the payment comes from
		tPayment.setCoinType(Crypto.CoinType.ETH.name());
		tPayment.setAmount(weiToFriendlyString(tx.getValue()));
		tPayment.setAccountId(((tAccountId == null) ? "<null>" : tAccountId));
		mDb.insertDbCryptoPayment(tPayment);

		// Update DbCryptoDeposit and Position Holdings
		SymbolX tSymbol = TeAppCntx.getInstance().getInstrumentContainer().getCryptoInstrument(Crypto.CoinType.ETH.name());
		long tNaDeltaAmmount = tSymbol.scaleFromOutsideNotation(tx.getValue().longValue());
		// Run the update through the Matching engine inorder to synchronize the position update with the orderboo
		if (tAccountId != null) {
			TeAppCntx.getMatchingEngine().updateCryptoPosition(tAccountId, tSymbol.getSid().get(), tNaDeltaAmmount, tx.getHash());
		} else {
			mLog.warn("No account found for received payment ammount: " +weiToFriendlyString( tx.getValue().longValue()) + " txid: " +tx.getHash().toString());
		}
	}


	String weiToFriendlyString( long pWei ) {
		return weiToFriendlyString( BigInteger.valueOf( pWei ));
	}

	String weiToFriendlyString( BigInteger pWeiGigInteger ) {
		BigDecimal bigdec = new BigDecimal( pWeiGigInteger );
		return Convert.fromWei(bigdec, Convert.Unit.ETHER ).toString() + " Eth";
	}


	// For creating infura project
	// Connect and see https://infura.io/dashboard

	private void connectToNetwork() {
		mWeb3j = Web3j.build(new HttpService(mNetHttpService));
	}

	private void createWallet( String pPath) {
		File tWalletFilePath = new File( pPath );
		try {
			WalletUtils.generateNewWalletFile( getWalletPassword(), tWalletFilePath );
		}
		catch( Exception e ) {
			mLog.fatal("failed to create wallet file path (" + tWalletFilePath.getAbsolutePath() +")", e);
			System.exit(0);
		}
	}

	private void loadWallet() {
		String tWalletFilename = getWalletFilenameFromKeystore( mDataDir + "/keystore/"  );
		if (tWalletFilename == null) {
			createWallet( mDataDir + "/keystore/" );
		}

		try {
			tWalletFilename = getWalletFilenameFromKeystore( mDataDir + "/keystore/"  );
			mWallet = WalletUtils.loadCredentials( getWalletPassword(), tWalletFilename );
			mLog.info("successfully loaded wallet file ( " + tWalletFilename + " )");
		}
		catch( Exception e) {
			mLog.fatal("failed to load wallet", e);
			System.exit(0);
		}
	}

	String getWalletFilenameFromKeystore(String pWalletFileDir ) {
		List<String> tAccountFiles = new ArrayList<String>();
		File[] tFiles = new File(pWalletFileDir).listFiles();
		if (tFiles != null) {
			for (File f : tFiles) {
				if (f.isFile()) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	String getWalletPassword() {
		return "testtest"; // todo: fix amore secure mechaism.
	}
}
