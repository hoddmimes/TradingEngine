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
import com.hoddmimes.te.common.db.TEDB;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.messages.generated.*;
import com.mongodb.client.result.UpdateResult;
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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;


public class BitcoinGwy implements  PeerDataEventListener,
									PeerConnectedEventListener,
									OnTransactionBroadcastListener,
									WalletChangeEventListener,
									WalletCoinsReceivedEventListener,
									KeyChainEventListener,
									WalletCoinsSentEventListener,
									TransactionConfidenceEventListener
{


	public SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


	private enum NetworkEnvironment {REGTEST, TEST3, MAIN};
	private Logger mLog = LogManager.getLogger( BitcoinGwy.class);
	private NetworkParameters mNetParams;
	private Wallet  mWallet;
	private Deposit mDeposit;
	private boolean mVerbose;
	private NetworkEnvironment mNetWrkEnv;
	private String mWalletFilename;

	private PeerGroup mPeerGroup;
	private BlockStore mBlockStore;
	private BlockChain mChain;
	private String  mDataDir;
	private TEDB mDb;
	private long mLastWalletSame;

	//private Timer mTimerTask;

	public BitcoinGwy(JsonObject pCryptoGwyConfig,  TEDB pDb ) {
		mDb = pDb;
		parseConfiguration(pCryptoGwyConfig);
		setupLogging();
		loadWallet();
		setupPeerGroup();
	}


	private void parseConfiguration( JsonObject pCryptoGwyConfig) {
		mVerbose = AuxJson.navigateBoolean( pCryptoGwyConfig,"bitcoin/verbose");

		mNetWrkEnv = NetworkEnvironment.valueOf( AuxJson.navigateString( pCryptoGwyConfig, "bitcoin/network"));
		if (mNetWrkEnv == null) {
			mLog.fatal("Invalid Bitcoin network (" + AuxJson.navigateString( pCryptoGwyConfig, "bitcoin/network") + ")");
			System.exit(0);
		}


		switch( mNetWrkEnv ) {
			case REGTEST:
				mNetParams = RegTestParams.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"bitcoin/dataDir") + "/regtest/";
				break;
			case TEST3:
				mNetParams = TestNet3Params.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"bitcoin/dataDir") + "/test/";
				break;
			case MAIN:
				mNetParams = MainNetParams.get();
				mDataDir = AuxJson.navigateString( pCryptoGwyConfig,"bitcoin/dataDir") + "/main/";
				break;
		}

		mLog.info("loading network parameters: " + mNetWrkEnv.name());
		mWalletFilename =  mDataDir + AuxJson.navigateString( pCryptoGwyConfig, "bitcoin/wallet");
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



	@Override
	public void onCoinsReceived(Wallet pWallet, Transaction tx, Coin prevBalance, Coin newBalance) {
		EventBitcoinOnCoinsReceived tEvent = eventOnCoinsReceived( pWallet, tx, prevBalance, newBalance);
		mLog.info( prettyJson( tEvent.toJson() ));
		bitcoinOnCoinsReceivedToDB( tEvent );
		saveWallet();

		// Wait for  the TX to appear in at least one block i.e. confirmed
		OnCoinReceivedConfirmationListner tOnCoinReceivedConfirmationListner = new OnCoinReceivedConfirmationListner(mWallet, tx );
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
		bitcoinOnCoinsSentToDB( tEvent );
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
				OnCoinReceivedConfirmationListner tOnCoinReceivedConfirmationListner = new OnCoinReceivedConfirmationListner(mWallet, tx );
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

		if (blocksLeft == 0) {
			/**
			 * Bitcoin chain is in sync. Update the position controller with current holdings
			 */
			setHoldingsAfterBitcoinChainSync();
		}

	}

	@Override
	public void onTransaction(Peer peer, Transaction t) {
		mLog.info("[onTransaction] peer: " + peer.toString() +
				"\n   transaction-txid: " + t.getTxId().toString() +
				"\n   tx: " + t.toString());
	}

	private void bitcoinOnCoinsSentToDB(EventBitcoinOnCoinsSent pEvent ) {
		List<DbCryptoPaymentEntry> tCryptoPaymentEntries = mDb.findPaymentEntryByAddressAndCoinType( pEvent.getEventData().get().getToAddress().get(), TEDB.CoinType.BTC.name());
		String  tAccountId = (tCryptoPaymentEntries.size() == 1) ? tCryptoPaymentEntries.get(0).getAccountId().get() : null;


		DbCryptoPayment tPayment = new DbCryptoPayment();
		tPayment.setCoinType( TEDB.CoinType.BTC.name());
		tPayment.setTxid( pEvent.getTxid().get());
		tPayment.setActionType( TEDB.ActionType.REDRAW.name());
		tPayment.setAddress( pEvent.getEventData().get().getToAddress().get());
		tPayment.setTime( SDF.format( System.currentTimeMillis()));
		tPayment.setState(TEDB.StateType.CONFIRM.name());


		if (tAccountId == null) {
			// This should  never happen. Someone redraw bitcoins without having a payment entry defined
			tPayment.setAccountId("null");
			mLog.error("Failed to locate bitcoin payment entry (address: " + pEvent.getEventData().get().getToAddress().get() + ") coin: " + TEDB.CoinType.BTC.name());
		} else {
			tPayment.setAccountId(tAccountId);
		}

		// Insert entry into database
		mDb.insertDbCryptoPayment(tPayment);
	}

	private void setHoldingsAfterBitcoinChainSync() {
		List<DbCryptoDeposit> tHoldings = mDb.findAllDbCryptoDeposit();
		TeAppCntx.getInstance().getPositionController().initialSyncCrypto( tHoldings );
	}

	private void bitcoinOnCoinsReceivedToDB(EventBitcoinOnCoinsReceived pEvent ) {

			List<DbCryptoPaymentEntry> tCryptoPaymentEntries = mDb.findPaymentEntryByAddressAndCoinType(pEvent.getEventData().get().getToAddress().get() , TEDB.CoinType.BTC.name());
			String  tAccountId = (tCryptoPaymentEntries.size() == 1) ? tCryptoPaymentEntries.get(0).getAccountId().get() : null;


			DbCryptoPayment tPayment = new DbCryptoPayment();
			tPayment.setCoinType(TEDB.CoinType.BTC.name());
			tPayment.setActionType( TEDB.ActionType.DEPOSIT.name() );
			tPayment.setTxid( pEvent.getTxid().get());
			tPayment.setAddress( pEvent.getEventData().get().getToAddress().get());
			tPayment.setTime( SDF.format( System.currentTimeMillis()));
			tPayment.setState(TEDB.StateType.PENDING.name());
			tPayment.setAmount( pEvent.getEventData().get().getAmount().get());


			if (tAccountId == null) {
				// This should  never happen. Someone sent us bitcoins without having a payment entry defined
				tPayment.setAccountId("null");
				mLog.error("Failed to locate bitcoin payment entry (address: " + pEvent.getEventData().get().getToAddress().get() + ") coin: " + TEDB.CoinType.BTC.name());
			} else {
				tPayment.setAccountId(tAccountId);
			}

			// Insert entry into database
			mDb.insertDbCryptoPayment(tPayment);
	}

	private void bitcoinOnCoinsReceivedConfirm(EventBitcoinOnCoinsReceivedConfirm pEvent ) {

		List<DbCryptoPaymentEntry> tCryptoPaymentEntries = mDb.findPaymentEntryByAddressAndCoinType( pEvent.getToAddress().get() , TEDB.CoinType.BTC.name());
		String  tAccountId = (tCryptoPaymentEntries.size() == 1) ? tCryptoPaymentEntries.get(0).getAccountId().get() : null;


		DbCryptoPayment tPayment = new DbCryptoPayment();
		tPayment.setCoinType(TEDB.CoinType.BTC.name());
		tPayment.setActionType( TEDB.ActionType.DEPOSIT.name() );
		tPayment.setTxid( pEvent.getTxid().get());
		tPayment.setAddress( pEvent.getToAddress().get());
		tPayment.setTime( SDF.format( System.currentTimeMillis()));
		tPayment.setState(TEDB.StateType.CONFIRM.name());
		tPayment.setAmount( pEvent.getAmountFriendly().get());



		if (tAccountId == null) {
			// This should  never happen. Someone sent us bitcoins without having a payment entry defined
			tPayment.setAccountId("null");
			mLog.error("Failed to locate bitcoin payment entry (txid: " + pEvent.getTxid().get() + ")");
		} else {
			tPayment.setAccountId(tAccountId);
		}

		// Insert entry into database
		mDb.insertDbCryptoPayment(tPayment);
		// Update Account Position
		if (tAccountId != null) {
			UpdateResult tUpdResult = mDb.updateAccountCryptoDeposit( tAccountId,  pEvent.getCoin().get(), pEvent.getAmount().get() );
			TeAppCntx.getInstance().getPositionController().updateHolding( tAccountId, InstrumentContainer.getBitcoinSID().toString(), pEvent.getAmount().get(), 0L);
		}
	}

	public String sendCoins(CryptoReDrawRequest pCryptoReDrawRequest) throws InsufficientMoneyException {

		BigDecimal tAmountBigDecimal = new BigDecimal(pCryptoReDrawRequest.getAmount().get() / TeAppCntx.PRICE_MULTIPLER);
		Coin tCoins = Coin.parseCoin(tAmountBigDecimal.toPlainString());
		SendRequest xtaTx;
		Transaction mTransaction = null;

		Address tDstAddr = Address.fromString(mNetParams, pCryptoReDrawRequest.getAddress().get());

		xtaTx = SendRequest.to(tDstAddr, tCoins);
		mTransaction = mWallet.sendCoins(mPeerGroup.getConnectedPeers().get(0), xtaTx);
		saveWallet();



		DbCryptoPayment tPayment = new DbCryptoPayment();
		tPayment.setAccountId( pCryptoReDrawRequest.getAccountId().get());
		tPayment.setAddress( pCryptoReDrawRequest.getAddress().get() );
		tPayment.setCoinType(TEDB.CoinType.BTC.name());
		tPayment.setState( TEDB.StateType.PENDING.name() );
		tPayment.setTime( SDF.format(System.currentTimeMillis()));
		tPayment.setTxid( mTransaction.getTxId().toString());
		tPayment.setAmount( tCoins.toFriendlyString() );
		tPayment.setActionType( TEDB.ActionType.REDRAW.name());
		mDb.updateDbCryptoPayment( tPayment, true );


		return mTransaction.getTxId().toString();

	}

	public GetDepositEntryResponse getPaymentEntry( GetDepositEntryRequest pEntryRequest)  {
		String tNewReceiveAddress = mWallet.freshReceiveAddress().toString();
		saveWallet();


		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setActionType(TEDB.ActionType.DEPOSIT.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( tNewReceiveAddress );
		cpe.setConfirmationId( CryptoGateway.getConfirmationId() );
		cpe.setConfirmed( true );

		mDb.insertDbCryptoPaymentEntry( cpe );

		GetDepositEntryResponse tResponse = new GetDepositEntryResponse();
		tResponse.setAddress( tNewReceiveAddress );
		//tResponse.setConfirmationId( cpe.getConfirmationId().get());
		tResponse.setCoin( pEntryRequest.getCoin().get() );
		return tResponse;
	}

	public SetReDrawEntryResponse setPaymentEntry( SetReDrawEntryRequest pEntryRequest)  {

		DbCryptoPaymentEntry cpe = new DbCryptoPaymentEntry();
		cpe.setAccountId( pEntryRequest.getAccountId().get() );
		cpe.setActionType(TEDB.ActionType.REDRAW.name());
		cpe.setCoinType( pEntryRequest.getCoin().get() );
		cpe.setAddress( pEntryRequest.getAddress().get() );
		cpe.setConfirmationId( CryptoGateway.getConfirmationId() );
		cpe.setConfirmed( false );

		mDb.insertDbCryptoPaymentEntry( cpe );

		SetReDrawEntryResponse tResponse = new SetReDrawEntryResponse();
		tResponse.setAddress( pEntryRequest.getAddress().get() );
		tResponse.setConfirmationId( cpe.getConfirmationId().get());
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
		tEvent.setCoin(TEDB.CoinType.BTC.name());
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
		tEvent.setCoin(TEDB.CoinType.BTC.name());
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

	EventBitcoinOnCoinsReceivedConfirm eventOnCoinsReceivedConfirmed( Wallet pWallet, Transaction pTx ) {
		EventBitcoinOnCoinsReceivedConfirm tEvent = new EventBitcoinOnCoinsReceivedConfirm();
		tEvent.setWalletId( pWallet.getDescription());
		tEvent.setCoin( TEDB.CoinType.BTC.name());
		tEvent.setTxid( pTx.getTxId().toString());
		tEvent.setToAddress( getToAddressFromTx( pTx));
		tEvent.setWallet( eventToBitcoinWallet( pWallet));
		tEvent.setAmount( pTx.getValueSentToMe( pWallet ).getValue());
		tEvent.setAmountFriendly(  pTx.getValueSentToMe( pWallet ).toFriendlyString());
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
	 * Confirmation Listeners
	 * ================================================================================================================
	 */
	class OnCoinReceivedConfirmationListner implements Executor, Runnable
	{
		Transaction mTx;
		Wallet      mWallet;
		String      mToAddress;



		public OnCoinReceivedConfirmationListner(Wallet pWallet, Transaction pTx)
		{
			mWallet = pWallet;
			mTx = pTx;
		}
		@Override
		public void run() {
			EventBitcoinOnCoinsReceivedConfirm tEvent = eventOnCoinsReceivedConfirmed(mWallet,mTx);
			mLog.info( prettyJson( tEvent.toJson() ));
			bitcoinOnCoinsReceivedConfirm( tEvent );
			saveWallet();
		}

		@Override
		public void execute(@NotNull Runnable command) {
			command.run();
		}
	}

}
