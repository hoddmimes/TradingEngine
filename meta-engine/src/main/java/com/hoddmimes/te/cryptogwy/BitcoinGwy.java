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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.bitcoinj.core.*;
import org.bitcoinj.core.listeners.OnTransactionBroadcastListener;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.core.listeners.PeerDataEventListener;
import org.bitcoinj.core.listeners.TransactionConfidenceEventListener;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.UnreadableWalletException;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletTransaction;
import org.bitcoinj.wallet.listeners.KeyChainEventListener;
import org.bitcoinj.wallet.listeners.WalletChangeEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;
import org.bitcoinj.wallet.listeners.WalletCoinsSentEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


public class BitcoinGwy extends CoinGateway implements  PeerDataEventListener,
									PeerConnectedEventListener,
									OnTransactionBroadcastListener,
									WalletChangeEventListener,
									WalletCoinsReceivedEventListener,
									KeyChainEventListener,
									WalletCoinsSentEventListener,
									TransactionConfidenceEventListener
{

	private enum NetworkEnvironment {REGTEST, TEST3, MAIN};
	private Logger mLog = LogManager.getLogger( BitcoinGwy.class);
	private NetworkParameters mNetParams;
	private Wallet  mWallet;
	private boolean mVerbose;
	private NetworkEnvironment mNetWrkEnv;
	private String mWalletFilename;

	private PeerGroup mPeerGroup;
	private BlockStore mBlockStore;
	private BlockChain mChain;
	private String  mDataDir;
	private long mLastWalletSame;
	private volatile boolean mDepositSettingInitialized;
	private long mTxFeePerKb;

	//private Timer mTimerTask;

	public BitcoinGwy(JsonObject pTeConfig,  TEDB pDb ) {
		super( pDb, Crypto.CoinType.BTC );
		mDepositSettingInitialized = false;
		parseConfiguration(AuxJson.navigateObject(pTeConfig,"TeConfiguration/cryptoGateway/bitcoin"));
		setupLogging();
		loadWallet();
		setupPeerGroup();
	}


	private void parseConfiguration( JsonObject pCryptoGwyConfig) {
		mVerbose = AuxJson.navigateBoolean( pCryptoGwyConfig,"verbose");
		mTxFeePerKb = AuxJson.navigateLong( pCryptoGwyConfig,"txFeePerKb",  6000L );


		mNetWrkEnv = NetworkEnvironment.valueOf( AuxJson.navigateString( pCryptoGwyConfig, "network"));
		if (mNetWrkEnv == null) {
			mLog.fatal("Invalid Bitcoin network (" + AuxJson.navigateString( pCryptoGwyConfig, "network") + ")");
			System.exit(0);
		}


		switch( mNetWrkEnv ) {
			case REGTEST:
				mNetParams = RegTestParams.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"dataDir") + "/regtest/";
				break;
			case TEST3:
				mNetParams = TestNet3Params.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"dataDir") + "/test/";
				break;
			case MAIN:
				mNetParams = MainNetParams.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"dataDir") + "/main/";
				break;
		}

		mLog.info("loading network parameters: " + mNetWrkEnv.name());
		mWalletFilename =  mDataDir + AuxJson.navigateString( pCryptoGwyConfig, "wallet");
	}

	/**
	 ** =====================================================================================================
	 **   Coin Gateway Interface
	 ** =====================================================================================================
	 */

	public long getEstimatedTxFeeNA() {
		return 0L; // transaction fee is paid by the receiver
	}






	/**
	 *
	 * Wallet Callback Lisners
	 */

	@Override
	public void onTransactionConfidenceChanged(Wallet pWallet, Transaction tx) {

		mLog.info("[onTransactionConfidenceChanged] \n" +
				" confidence-depth: " +tx.getConfidence().getDepthInBlocks()+ "\n" +
				walletToString( pWallet, true ));
	}

	@Override
	public void onWalletChanged(Wallet pWallet) {
		if (mVerbose) {
			mLog.info("[onWalletChanged] " + "\n " + walletToString(pWallet, true));
		}

		if ((mLastWalletSame + 300000L) < System.currentTimeMillis()) {
			saveWallet();
		}
	}

	private DbCryptoPaymentEntry findCryptoPaymentEntry( Transaction tx ) {
		String tDestAddress = getToAddressFromTx( tx );
		return  (DbCryptoPaymentEntry) TEDB.dbEntryFound(mDb.findPaymentEntryByAddressAndCoinTypeAndPaymentType(tDestAddress , Crypto.CoinType.BTC, Crypto.PaymentType.DEPOSIT));
	}

	@Override
	public void onCoinsReceived(Wallet pWallet, Transaction tx, Coin prevBalance, Coin newBalance) {
		DbCryptoPaymentEntry tCryptoPaymentEntry = findCryptoPaymentEntry( tx );
		EventBitcoinOnCoinsReceived tEvent = eventOnCoinsReceived( pWallet, tx, prevBalance, newBalance );

		// Check if we have received coind designated to a know destination account or not.
		// If no designated account is found just save the wallet and go on
		if (tCryptoPaymentEntry == null) {
			mLog.info("[onCoinsReceived] for no payment entry,  address: " + tEvent.getEventData().get().getToAddress() +
						" prev-balance: " + prevBalance.toFriendlyString() + " new-balance: " + newBalance.toString() + " txid: " + tx.getTxId().toString());
			saveWallet();
			return;
		}
		mLog.info( prettyJson( tEvent.toJson() ));
		bitcoinOnCoinsReceivedToDB( tEvent );
		saveWallet();

		// Wait for  the TX to appear in at least one block i.e. confirmed
		OnCoinConfirmationListner tOnCoinReceivedConfirmationListner = new OnCoinConfirmationListner(mWallet, tx );
		ListenableFuture<TransactionConfidence> tFuture = tx.getConfidence().getDepthFuture(1);
		tFuture.addListener(tOnCoinReceivedConfirmationListner, tOnCoinReceivedConfirmationListner);
	}


	@Override
	public void onKeysAdded(List<ECKey> keys) {
		StringBuilder sb = new StringBuilder();

		for( ECKey k : keys ) {
			sb.append("    " + k.getPublicKeyAsHex() + "\n");
		}
		mLog.info("[onKeysAdded] " + "\n" + sb.toString());
	}

	@Override
	public void onCoinsSent(Wallet pWallet, Transaction tx, Coin prevBalance, Coin newBalance) {
		EventBitcoinOnCoinsSent tEvent = eventOnCoinsSent( pWallet, tx, prevBalance, newBalance);
		mLog.info( prettyJson( tEvent.toJson() ));
		saveWallet();
	}

	private void saveWallet() {
		File tWalletFile = new File(mWalletFilename);
		try {
			TeWalletExtension tTeWalletExtension = (TeWalletExtension) mWallet.getExtensions().get( TeWalletExtension.WALLET_EXTENSION );
			tTeWalletExtension.incrementsSaves();
			tTeWalletExtension.setSaveTime( System.currentTimeMillis());
			mWallet.saveToFile( tWalletFile );
			mLastWalletSame = System.currentTimeMillis();
		} catch (IOException e) {
			mLog.fatal("Failed to save wallet (" + mWalletFilename + ")", e);
			System.exit(0);
		}
	}


	public String walletToString() {
		return walletToString( mWallet, true );
	}

	String walletToString( Wallet w, boolean pPretty ) {
		EventBitcoinWallet bw = eventToBitcoinWallet( w );
		if (mVerbose || pPretty) {
			return prettyJson(bw.toJson());
		}
		return bw.toJson().toString();
	}




	private String getToAddressFromTx( Transaction pTx ) {
		List<TransactionOutput> tTxOuts = pTx.getOutputs();
		if (tTxOuts == null) {
			return null;
		}
		for( TransactionOutput tTx : tTxOuts) {
			if (tTx.isMine( mWallet )) {
				return tTx.getScriptPubKey().getToAddress(mNetParams).toString();
			}
		}
		return null;
	}

	private void setupPeerGroup() {
		try {
			File tBlockStoreFile = new File( mDataDir +  "spvchain.blockstore");
			mBlockStore = new SPVBlockStore(mNetParams, tBlockStoreFile );
			mChain = new BlockChain(mNetParams, mBlockStore);
		}
		catch( BlockStoreException be) {
			be.printStackTrace();
		}

		TeWalletExtension tTeWalletExtension = (TeWalletExtension) mWallet.getExtensions().get(TeWalletExtension.WALLET_EXTENSION);


		long tEarliestTime = (mWallet.getLastBlockSeenTime() == null) ? tTeWalletExtension.getCreationTime() : mWallet.getLastBlockSeenTime().getTime();


		mPeerGroup = new PeerGroup(mNetParams, mChain );
		mPeerGroup.setUserAgent("TEWallet", "1.0" );
		mPeerGroup.setBloomFilteringEnabled( true );
		mPeerGroup.addWallet(mWallet);
		mChain.addWallet( mWallet );
		if (mVerbose) {
			mPeerGroup.addBlocksDownloadedEventListener(this);
			mPeerGroup.startBlockChainDownload( this );
			mPeerGroup.addOnTransactionBroadcastListener(this);
		}
		mPeerGroup.addConnectedEventListener( this );
		mPeerGroup.setBloomFilteringEnabled(true);
		mPeerGroup.setFastCatchupTimeSecs( tEarliestTime );
		mPeerGroup.start();
		mLog.info("Starting PeerGroup, earliest catchup time: " + SDF.format( tEarliestTime ));

		if (mNetWrkEnv == NetworkEnvironment.REGTEST) {
			mPeerGroup.connectToLocalHost(); // I.e. the regtest daemon
		} else {
			mPeerGroup.addPeerDiscovery(new DnsDiscovery(mNetParams));
		}
		mPeerGroup.startBlockChainDownload( this );
	}

	// ToDo: change the mechanism for how to retreive the password to the exchange wallet :-)
	// The password must be rtreived from a remote password server using certficates but for the simplicity and
	// and the time being we just include the password in the configuration.
	private String getWalletPassword() {
		return "testtest";
	}



	private void loadWallet() {
		createWalletIfDoesNotExists(); //Todo: it should always be required that the wallet exists

		File tWalletFile = new File( mWalletFilename );
		if (!tWalletFile.exists()) {
			mLog.fatal("Bitcoin exchange wallet file not found (" + mWalletFilename + ")");
			System.exit(0);
		}

		try {mWallet = Wallet.loadFromFile(tWalletFile, new TeWalletExtension() ); }
		catch( UnreadableWalletException e) {
			mLog.fatal("Could not read/load wallet (" + mWalletFilename + ")", e);
			System.exit(0);
		}
		//mWallet.decrypt( getWalletPassword());

		mLastWalletSame = (mWallet.getLastBlockSeenTime() == null) ? System.currentTimeMillis() : mWallet.getLastBlockSeenTime().getTime();

		mWallet.addChangeEventListener( this );
		mWallet.addCoinsReceivedEventListener( this );
		mWallet.addKeyChainEventListener( this);
		mWallet.addCoinsSentEventListener( this );
		mWallet.addTransactionConfidenceEventListener( this );
		mLog.info("Successfully loaded Bitcoin wallet \n" + walletToString( mWallet, true ));

		listWatchedAddresses();

		setupConfirmListener();
	}


	private void listWatchedAddresses() {
		List<Address> tAddresses = mWallet.getWatchedAddresses();
		if (tAddresses == null) {
			mLog.warn("NO receive addresses are watched!");
		}
		StringBuilder sb = new StringBuilder();
		for( Address tAddr : tAddresses) {
			sb.append("   address: " + tAddr + "\n");
		}
		if (tAddresses.size() > 0) {
			mLog.info("Addresses watched\n " + sb.toString());
		}
	}


	private void setupConfirmListener() {
		if (mWallet.getTransactionPool(WalletTransaction.Pool.PENDING).size() == 0) {
			mLog.info("Bitcoin has no pending transactions no pending transactions");
		} else {
			Map<Sha256Hash, Transaction> tPendingTxsMap =  mWallet.getTransactionPool(WalletTransaction.Pool.PENDING);
			for( Transaction tx : tPendingTxsMap.values()) {
				OnCoinConfirmationListner tOnCoinReceivedConfirmationListner = new OnCoinConfirmationListner(mWallet, tx );
				ListenableFuture<TransactionConfidence> tFuture = tx.getConfidence().getDepthFuture(1);
				tFuture.addListener(tOnCoinReceivedConfirmationListner, tOnCoinReceivedConfirmationListner);
			}
		}
	}

	private void createWalletIfDoesNotExists() {
		File tWalletFile = new File(mWalletFilename);
		if (!tWalletFile.exists()) {
			Context tContext = new Context(mNetParams);
			mWallet = Wallet.createDeterministic( tContext, Script.ScriptType.P2PKH );
			mWallet.freshReceiveAddress();
			mWallet.setDescription("TE Wallet");
			mWallet.encrypt( getWalletPassword());
			mWallet.addExtension( new TeWalletExtension());
			saveWallet();
		}
	}



	private void setupLogging() {
		String tFilename =  mDataDir + "bitcoin-event.log";

		if (!mVerbose) {
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.core.Peer","OFF");
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.core.PeerGroup", "OFF");
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.core.PeerSocketHandler", "OFF");
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.net.ConnectionHandler","OFF");
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.core.PeerSocketHandler","OFF");
			System.setProperty("org.slf4j.simpleLogger.log.org.bitcoinj.net.NioClientManager","OFF");
		}
	}

	@Override
	public void onPeerConnected(Peer peer, int peerCount) {
		mLog.info("[onPeerConnected] peer: " + peer.toString() + " connected: " + peerCount);
	}

	@Override
	public void onChainDownloadStarted(Peer peer, int blocksLeft) {
		mLog.info("[onChainDownloadStarted] peer: " + peer.toString() + " block-left: " + blocksLeft);
		if ((blocksLeft == 0) && (!mDepositSettingInitialized)) {
			setHoldingsAfterBitcoinChainSync();
		}
	}

	@Nullable
	@Override
	public List<Message> getData(Peer peer, GetDataMessage m) {
		mLog.info("[onChainDownloadStarted] peer: " + peer.toString() + "\n  (GetDataMesssage): " + m.toString());
		return null;
	}

	@Override
	public Message onPreMessageReceived(Peer peer, Message m) {
		if (mVerbose) {
			mLog.info("[onPreMessageReceived] peer: " + peer.toString());
		}
		return m;
	}


	@Override
	public void onBlocksDownloaded(Peer peer, Block block, @Nullable FilteredBlock filteredBlock, int blocksLeft) {
		if (mVerbose) {
			StringBuilder sb = new StringBuilder();
			sb.append("[onBlocksDownloaded] [peer]: " + peer.toString() + " block-time: " + SDF.format(block.getTime().getTime()) + " [block]: " + block.toString() + " [block-left]: " + blocksLeft + "\n");
			List<Transaction> tTxs = block.getTransactions();
			if (tTxs != null) {
				for (Transaction tx : tTxs) {
					sb.append("   [tx]: " + tx.toString() + "\n");
				}
			}
			mLog.info(sb.toString());
		}

		if ((blocksLeft == 0) && (!mDepositSettingInitialized)) {
			/**
			 * Bitcoin chain is in sync. Update the position controller with current holdings
			 */
			mLog.info("Bitcoin block chain in now in sync");
			setHoldingsAfterBitcoinChainSync();
		}

	}

	@Override
	public void onTransaction(Peer peer, Transaction t) {
		mLog.info("[onTransaction] peer: " + peer.toString() +
				"\n   transaction-txid: " + t.getTxId().toString() +
				"\n   tx: " + t.toString());
	}



	private void setHoldingsAfterBitcoinChainSync() {
		mDepositSettingInitialized = true;
		//Todo: fix startup sequence
	}

	private void bitcoinOnCoinsReceivedToDB(EventBitcoinOnCoinsReceived pEvent ) {

			DbCryptoPaymentEntry tCryptoPaymentEntry = (DbCryptoPaymentEntry) TEDB.dbEntryFound(
					mDb.findPaymentEntryByAddressAndCoinTypeAndPaymentType(pEvent.getEventData().get().getToAddress().get() ,
																			Crypto.CoinType.BTC, Crypto.PaymentType.DEPOSIT));

			String  tAccountId = (tCryptoPaymentEntry != null) ? tCryptoPaymentEntry.getAccountId().get() : null;


			if (tAccountId == null) {
				// This should  never happen. Someone sent us bitcoins without having a payment entry defined
				mLog.error("Failed to locate bitcoin payment entry (address: " + pEvent.getEventData().get().getToAddress().get() + ") coin: " + Crypto.CoinType.BTC.name());
			}
			// Insert entry into database
			dbCreatePaymentEntry( Crypto.CoinType.BTC,
									tAccountId,
									pEvent.getTxid().get(),
					                pEvent.getEventData().get().getToAddress().get(),
								    pEvent.getEventData().get().getAmount().get(),
									Crypto.StateType.PENDING,
									Crypto.PaymentType.DEPOSIT );
	}

	private void bitcoinOnReceivedConfirmation( EventBitcoinOnCoinsConfirm tConfirmationEvent) {
		SymbolX tSymbol = TeAppCntx.getInstance().getInstrumentContainer().getCryptoInstrument( Crypto.CoinType.BTC.name());
		long tTeInternalAmount = tSymbol.scaleFromOutsideNotation( tConfirmationEvent.getAmount().get() );
		TeAppCntx.getMatchingEngine().updateCryptoPosition( tConfirmationEvent.getAccountId().get(),  tSymbol.getSid().get(), tTeInternalAmount, tConfirmationEvent.getTxid().get());

		// Log deposit payment entry
		dbCreatePaymentEntry( Crypto.CoinType.BTC,
				tConfirmationEvent.getAccountId().get(),
				tConfirmationEvent.getTxid().get(),
				tConfirmationEvent.getAddress().get().trim(),
				tConfirmationEvent.getAmountFriendly().get(),
				Crypto.StateType.CONFIRM,
				Crypto.PaymentType.DEPOSIT );
	}


	private void bitcoinOnSentConfirmation( EventBitcoinOnCoinsConfirm tConfirmationEvent ) {
		// Log deposit payment entry
		dbCreatePaymentEntry( Crypto.CoinType.BTC,
				tConfirmationEvent.getAccountId().get(),
				tConfirmationEvent.getTxid().get(),
				tConfirmationEvent.getAddress().get().trim(),
				tConfirmationEvent.getAmountFriendly().get(),
				Crypto.StateType.CONFIRM,
				Crypto.PaymentType.REDRAW );
	}


	public String sendCoins(CryptoRedrawRequest pCryptoRedrawRequest) throws TeException {

		Coin tCoins = Coin.valueOf(super.mCoinInst.scaleToOutsideNotation( pCryptoRedrawRequest.getAmount().get()));
		SendRequest xtaTx;
		Transaction tTransaction = null;

		Address tDstAddr = Address.fromString(mNetParams, pCryptoRedrawRequest.getAddress().get().trim());

		xtaTx = SendRequest.to(tDstAddr, tCoins);
		xtaTx.recipientsPayFees = true;
		xtaTx.feePerKb = Coin.valueOf(mTxFeePerKb);
		xtaTx.memo = Crypto.PaymentType.REDRAW.name();


		try {
			mWallet.decrypt( this.getWalletPassword());
			tTransaction = mWallet.sendCoins(mPeerGroup.getConnectedPeers().get(0), xtaTx);
			mWallet.encrypt( this.getWalletPassword());
		}
		catch( InsufficientMoneyException e) {
			throw new TeException(StatusMessageBuilder.error("insufficient holdings, reason: " + e.getMessage(), null));
		}
		saveWallet();

		DbCryptoPayment tPayment = dbCreatePaymentEntry( Crypto.CoinType.BTC,
										   pCryptoRedrawRequest.getAccountId().get(),
				                           tTransaction.getTxId().toString(),
										   tDstAddr.toString(),
										   tCoins.toFriendlyString(),
										   Crypto.StateType.PENDING,
				                           Crypto.PaymentType.REDRAW );

		mLog.info( "[sendCoins] \n" + prettyJson( tPayment.toJson() ));

		// Wait for  the TX to appear in at least one block i.e. confirmed
		OnCoinConfirmationListner tOnCoinReceivedConfirmationListner = new OnCoinConfirmationListner(mWallet, tTransaction );
		ListenableFuture<TransactionConfidence> tFuture = tTransaction.getConfidence().getDepthFuture(1);
		tFuture.addListener(tOnCoinReceivedConfirmationListner, tOnCoinReceivedConfirmationListner);
		return tTransaction.getTxId().toString();
	}

	public GetDepositEntryResponse addDepositEntry( GetDepositEntryRequest pEntryRequest)  {
		String tNewReceiveAddress = mWallet.freshReceiveAddress().toString();
		saveWallet();


		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setPaymentType(Crypto.PaymentType.DEPOSIT.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( tNewReceiveAddress );
		cpe.setConfirmationId( getConfirmationId() );
		cpe.setConfirmed( true );

		mDb.insertDbCryptoPaymentEntry( cpe );

		GetDepositEntryResponse tResponse = new GetDepositEntryResponse();
		tResponse.setAddress( tNewReceiveAddress );
		//tResponse.setConfirmationId( cpe.getConfirmationId().get());
		tResponse.setCoin( pEntryRequest.getCoin().get() );
		return tResponse;
	}

	public SetRedrawEntryResponse addRedrawEntry( SetRedrawEntryRequest pEntryRequest, boolean pConfirmEntry)  {

		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setPaymentType(Crypto.PaymentType.REDRAW.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( pEntryRequest.getAddress().get().trim() );
		cpe.setConfirmationId( getConfirmationId() );
		cpe.setConfirmed( pConfirmEntry );
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
		tResponse.setAddress( pEntryRequest.getAddress().get().trim() );
		tResponse.setStatusText( "confirmation link will be mailed to your mail account");
		tResponse.setCoin( pEntryRequest.getCoin().get() );
		return tResponse;
	}

	/**
	 * ================================================================================================================
	 * To JSON Messages / Structures
	 * ================================================================================================================
	 */

	EventBitcoinOnCoinsSent eventOnCoinsSent( Wallet pWallet, Transaction pTx, Coin pPrevBalance, Coin pNewBalance )
	{
		EventBitcoinOnCoinsSent tEvent = new EventBitcoinOnCoinsSent();
		tEvent.setCoin(Crypto.CoinType.BTC.name());
		tEvent.setTxid( pTx.getTxId().toString());
		tEvent.setWalletId( pWallet.getDescription());
		tEvent.setWallet( eventToBitcoinWallet( pWallet));

		EventBitcoinOnCoinSentData tEventData = new EventBitcoinOnCoinSentData();
		tEventData.setNewBalance( pNewBalance.toFriendlyString());
		tEventData.setPrevBalance( pPrevBalance.toFriendlyString());
		tEventData.setToAddress( getToAddressFromTx( pTx ));

		tEvent.setEventData(tEventData);
		return tEvent;
	}

	EventBitcoinOnCoinsReceived eventOnCoinsReceived( Wallet pWallet, Transaction pTx, Coin pPrevBalance, Coin pNewBalance )
	{
		EventBitcoinOnCoinsReceived tEvent = new EventBitcoinOnCoinsReceived();
		tEvent.setCoin(Crypto.CoinType.BTC.name());
		tEvent.setTxid( pTx.getTxId().toString());
		tEvent.setWalletId( pWallet.getDescription());
		tEvent.setWallet( eventToBitcoinWallet( pWallet));

		EventBitcoinOnCoinReceivedData tEventData = new EventBitcoinOnCoinReceivedData();
		tEventData.setNewBalance( pNewBalance.toFriendlyString());
		tEventData.setPrevBalance( pPrevBalance.toFriendlyString());
		tEventData.setToAddress( getToAddressFromTx( pTx ));
		tEventData.setAmount( pTx.getValueSentToMe( pWallet).toFriendlyString());

		tEvent.setEventData(tEventData);
		return tEvent;
	}

	EventBitcoinOnCoinsConfirm eventOnCoinsConfirmed( Wallet pWallet, Transaction pTx, DbCryptoPayment pPreviousDbCryptoPayment ) {
		EventBitcoinOnCoinsConfirm tEvent = new EventBitcoinOnCoinsConfirm();
		tEvent.setAccountId( pPreviousDbCryptoPayment.getAccountId().get());
		tEvent.setCoin( Crypto.CoinType.BTC.name());
		tEvent.setTxid( pTx.getTxId().toString());
		tEvent.setAddress( getToAddressFromTx(pTx));
		tEvent.setAmount( pTx.getValueSentToMe( pWallet ).getValue());
		tEvent.setAmountFriendly(  pTx.getValueSentToMe( pWallet ).toFriendlyString());
		tEvent.setPaymentType( pPreviousDbCryptoPayment.getPaymentType().get() );
		return tEvent;
	}


	EventBitcoinWallet eventToBitcoinWallet(Wallet pWallet ) {
		TeWalletExtension tExtension = (TeWalletExtension) pWallet.getExtensions().get( TeWalletExtension.WALLET_EXTENSION );
		String tLstBlkTim = (pWallet.getLastBlockSeenTime() == null) ? "<null>" : SDF.format( pWallet.getLastBlockSeenTime().getTime());
		String tLstSavTim = (tExtension.getSaveTimeString() == null) ? "<null>" : tExtension.getSaveTimeString();

		EventBitcoinWallet bw = new EventBitcoinWallet();
		bw.setDescription( pWallet.getDescription());
		bw.setCurrRcvAddr( pWallet.currentReceiveAddress().toString());
		bw.setLastBlkTime( tLstBlkTim );
		bw.setLastSaveTime( tLstSavTim );
		bw.setSaveCnt( tExtension.getFileSavesCount() );

		EventBitcoinBalance tBalance = new EventBitcoinBalance();
		tBalance.setAvailable( pWallet.getBalance(Wallet.BalanceType.AVAILABLE).toFriendlyString());
		tBalance.setEstimated( pWallet.getBalance(Wallet.BalanceType.ESTIMATED).toFriendlyString());
		tBalance.setAvailableSpendable( pWallet.getBalance(Wallet.BalanceType.AVAILABLE_SPENDABLE).toFriendlyString());
		tBalance.setEstimatedSpendable( pWallet.getBalance(Wallet.BalanceType.ESTIMATED_SPENDABLE).toFriendlyString());
		bw.setBalance( tBalance );

		EventBitcoinTransaction tTransactions = new EventBitcoinTransaction();
		tTransactions.setDead(pWallet.getTransactionPool(WalletTransaction.Pool.DEAD).size());
		tTransactions.setPending(pWallet.getTransactionPool(WalletTransaction.Pool.PENDING).size());
		tTransactions.setSpent(pWallet.getTransactionPool(WalletTransaction.Pool.SPENT).size());
		tTransactions.setUnspent(pWallet.getTransactionPool(WalletTransaction.Pool.UNSPENT).size());
		bw.setTransaction( tTransactions );

		return bw;
	}


	public String prettyJson(JsonObject pObject ) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(pObject);
	}

	/**
	 * ================================================================================================================
	 * Confirmation Listeners used both for receiving and sending coins
	 * ================================================================================================================
	 */
	class OnCoinConfirmationListner implements Executor, Runnable
	{
		Transaction mTx;
		Wallet      mWallet;

		public OnCoinConfirmationListner(Wallet pWallet, Transaction pTx  )
		{
			mWallet = pWallet;
			mTx = pTx;
		}

		private boolean stateIsPending( List<DbCryptoPayment> pCryptoPayments ) {
			if (pCryptoPayments == null) {
				return false;
			}
			if ((pCryptoPayments.size() == 1) && (pCryptoPayments.get(0).getState().get().contentEquals(Crypto.StateType.PENDING.name()))) {
				return true;
			}
			for (DbCryptoPayment tPayment : pCryptoPayments) {
				if (!tPayment.getState().get().contentEquals(Crypto.StateType.PENDING.name()))
					return false;
			}
			return true;
		}

		private boolean stateIsConfirmed( List<DbCryptoPayment> pCryptoPayments ) {
			if (pCryptoPayments == null) {
				return false;
			}
			for (DbCryptoPayment tPayment : pCryptoPayments) {
				if (tPayment.getState().get().contentEquals(Crypto.StateType.CONFIRM.name()))
					return true;
			}
			return false;
		}

		@Override
		public void run() {
			// If we get here it's the result of of a pending transaction being confirmed
			// There should be a DBCrypto payment entry in the DB with status "PENDING"
			// Based on that entry we will know whetever this is a confirmation for a DEPOSIT or REDRAW
			List<DbCryptoPayment> tDbCryptoPayments = mDb.findPaymentsByTxid( mTx.getTxId().toString());

			if ((tDbCryptoPayments == null) || (tDbCryptoPayments.size() == 0)) {
				// Wow no entry found
				mLog.warn("[OnCoinConfirmationListner] no previous payment entry found for tx:" + mTx.toString());
				return;
			}

			if (stateIsConfirmed(tDbCryptoPayments)) {
				// Wow transaction already confirmed how did we end up here multiple times ?
				mLog.warn("[OnCoinConfirmationListner] transaction already being confirmed tx:" + mTx.toString());
				return;
			}

			DbCryptoPayment tPreviousDbCryptoPayment = tDbCryptoPayments.get(0);
			Crypto.PaymentType tPaymentType = Crypto.PaymentType.valueOf(tDbCryptoPayments.get(0).getPaymentType().get());

			try {
				EventBitcoinOnCoinsConfirm tEvent = eventOnCoinsConfirmed(mWallet, mTx, tPreviousDbCryptoPayment );
				mLog.info(prettyJson(tEvent.toJson()));
				if (tPaymentType == Crypto.PaymentType.DEPOSIT) {
					bitcoinOnReceivedConfirmation( tEvent );
				} else {
					bitcoinOnSentConfirmation(tEvent);
				}
				saveWallet();
			}
			catch( Throwable t) {
				mLog.fatal("failed to process OnCoinReceivedConfirmationListner event", t);
			}
		}

		@Override
		public void execute(@NotNull Runnable command) {
			command.run();
		}
	}

}
