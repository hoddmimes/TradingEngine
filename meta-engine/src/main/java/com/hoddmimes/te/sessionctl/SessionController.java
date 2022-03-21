/*
 * Copyright (c)  Hoddmimes Solution AB 2021.
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

package com.hoddmimes.te.sessionctl;


import com.google.gson.JsonObject;
import com.hoddmimes.jaux.AuxTimestamp;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.interfaces.*;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;
import com.hoddmimes.te.common.ipc.IpcComponentInterface;
import com.hoddmimes.te.common.ipc.IpcService;
import com.hoddmimes.te.engine.MatchingEngine;
import com.hoddmimes.te.instrumentctl.InstrumentContainer;
import com.hoddmimes.te.messages.*;
import com.hoddmimes.te.messages.generated.*;
import com.hoddmimes.te.trades.TradeContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionController extends TeCoreService implements ConnectorInterface.ConnectorCallbackInterface
{
	public static final String INTERNAL_SESSION_ID = UUID.randomUUID().toString();

	private Logger mLog = LogManager.getLogger( SessionController.class);

	private AuthenticateInterface mAuthenticator = null;
	private JsonObject mConfiguration = null;
	private SessionCntxMapper mSessionMapper = null;
	private SessionCntxInterface mInternalSession;

	private boolean mIsMessageLoggerEnabled;
	private long mMessagerLoggerFlushInterval;
	private MessageLogger.FlushMode mMessageLoggerFlushMode;
	private MessageLogger mMsgLogger;


	private JsonSchemaValidator mSchemaValidator;
	private MessageFactory mMessageFactory;
	private long mTraceExecTimeLimitUsec;
	private boolean mTraceExecTimeVerbose;
	private boolean mTraceExecTimeOff;

	private long mStartTime;
	private AtomicInteger mRequestCount;
	private AtomicInteger mFailureCount;

	public SessionController(JsonObject pTeConfigurationFile, IpcService pIpcService) throws IOException
	{
		super( pTeConfigurationFile, pIpcService );
		mRequestCount = new AtomicInteger(0);
		mFailureCount = new AtomicInteger(0);
		mStartTime = System.currentTimeMillis();
		mSessionMapper = new SessionCntxMapper();
		mInternalSession = new SessionCntx("internal",INTERNAL_SESSION_ID);
		mSessionMapper.add(mInternalSession);
		loadConfiguration( pTeConfigurationFile );
		mMessageFactory = new MessageFactory();
	}

	@Override
	public TeService getServiceId() {
		return TeService.SessionService;
	}

	public SessionCntxInterface getInternalSessionContext()
	{
		return mInternalSession;
	}

	private void loadConfiguration(JsonObject pTeConfigurationFile) {
		mConfiguration = AuxJson.navigateObject(pTeConfigurationFile, "TeConfiguration/sessionControllerConfiguration").getAsJsonObject();

		initializeAuthenticator();

		String tSchemaSource = AuxJson.navigateString(mConfiguration, "schemaDefinitions");

		mSchemaValidator = new JsonSchemaValidator(tSchemaSource);
		mTraceExecTimeLimitUsec = AuxJson.navigateLong(mConfiguration, "traceExecutionTimeLimitUsec", -1L);
		mTraceExecTimeVerbose = AuxJson.navigateBoolean(mConfiguration, "traceExecutionTimeVerbose", false);
		mTraceExecTimeOff = AuxJson.navigateBoolean(mConfiguration, "traceExecutionTimeOff", true);

		if (mTraceExecTimeOff) {
			AuxTimestamp.disable();
		}

		mIsMessageLoggerEnabled = AuxJson.navigateBoolean(mConfiguration,"messageLoggerEnabled", true);
		mMessageLoggerFlushMode = MessageLogger.FlushMode.valueOf( AuxJson.navigateString(mConfiguration,"messageLoggerFlushMode", "NONE"));
		mMessagerLoggerFlushInterval = AuxJson.navigateLong(mConfiguration,"messageLoggerFlushIntervalMs", 15000L);

		if (mIsMessageLoggerEnabled) {
			String tFilename = AuxJson.navigateString(mConfiguration,"messageLoggerFile", "logs/TeMessageLoggger-%datetime%.log");
			mMsgLogger = new MessageLogger( tFilename, mMessageLoggerFlushMode, mMessagerLoggerFlushInterval);
			mLog.info("Create message log file \"" + mMsgLogger.getFilename() + "\"");
		}
	}

	private void initializeAuthenticator() {
		String tAuthClsStr = null;
		try {
			tAuthClsStr = AuxJson.navigateString( mConfiguration,"autheticator/implementation");
			Class[] cArg = new Class[0];
			Class c = Class.forName(tAuthClsStr);
			mAuthenticator = (AuthenticateInterface) c.getDeclaredConstructor( cArg ).newInstance();
		} catch (Exception e) {
			mLog.error("Failed to instansiate Authenticator class \"" + tAuthClsStr +"\" reason: " + e.getMessage());
		}
	}

	public boolean validateAuthId( String pAuthId ) {
		return mSessionMapper.validateApiAuthId( pAuthId );
	}


	public SessionCntxInterface logoninternal(LogonRequest pLogonRequest, String pSessionId) throws TeException
	{
		if (pSessionId == null) {
			mLog.warn(("Logon, account session id object for user " + pLogonRequest.getAccount().get() + " must not be null"));
			throw new TeException( HttpStatus.BAD_REQUEST.value(),
					StatusMessageBuilder.error(("Logon, account session id object for user " + pLogonRequest.getAccount().get() + " must not be null"), pLogonRequest.getRef().get()));
		}

		SessionCntxInterface tSessCntx = mSessionMapper.getById( pSessionId );
		if (tSessCntx != null) {
				return tSessCntx; // already signed in
		}
		Account tAccount = mAuthenticator.logon(pLogonRequest.getAccount().get(), pLogonRequest.getPassword().get());
		if (tAccount != null) {
			tSessCntx = new SessionCntx(tAccount.getAccountId().get(), pSessionId);
			mSessionMapper.add( tSessCntx );
			return tSessCntx;
		}
		mLog.warn("Unauthorized logon, account: " + pLogonRequest.getAccount().get() + " sessionid: " + pSessionId);
		throw new TeException( HttpStatus.UNAUTHORIZED.value(),
				StatusMessageBuilder.error("Unauthorized logon", pLogonRequest.getRef().get()));
	}



	private void validateRequest( String pJsonRqstMsgString ) throws Exception
	{
		mSchemaValidator.validate( pJsonRqstMsgString );
	}

	/**
	 * ====================================================================
	 * Callback endpoints for the Connector implementation
	 * ====================================================================
	 */

	private SessionCntxInterface removeSession( String pSessionId ) {
		return mSessionMapper.removeById( pSessionId );
	}

	@Override
	public SessionCntxInterface connectorDisconnectSession(String pSessionId) {
		return removeSession( pSessionId );
	}

	@Override
	public SessionCntxInterface terminateSession(String pSessionId) {
		return removeSession( pSessionId );
	}

	@Override
	public SessionCntxInterface getSessionContext(String pSessionId) {
		return mSessionMapper.getById(pSessionId);
	}

	public SessionCntxInterface getSessionCntxByAuthId(String pApiAuthId) {
		return mSessionMapper.getSessionCntxByAuthId(pApiAuthId);
	}

	public List<SessionCntxInterface> getSessionContextByAccount(String pAccountId ) {
		return mSessionMapper.getByAccount( pAccountId );
	}

	public MessageInterface validateMessage( String pJsonRqstMsgStr ) throws Exception
	{
		mSchemaValidator.validate( pJsonRqstMsgStr);
		return mMessageFactory.getMessageInstance( pJsonRqstMsgStr );
	}



	@Override
	public MessageInterface connectorMessage(String pSessionId, String pJsonRqstMsgStr) {
		mRequestCount.incrementAndGet();
		String tRef = AuxJson.getMessageRef(pJsonRqstMsgStr);

		// Validate message syntax
		try {
			mSchemaValidator.validate(pJsonRqstMsgStr);
		} catch (Exception e) {
			mFailureCount.incrementAndGet();
			return StatusMessageBuilder.error("Invalid request message syntax", tRef, e);
		}

		MessageInterface tRqstMsg = mMessageFactory.getMessageInstance(pJsonRqstMsgStr);
		if (!(tRqstMsg instanceof RequestMsgInterface)) {
			mFailureCount.incrementAndGet();
			mLog.error("Request \"" + tRqstMsg.getMessageName() + "\" does not implement interface RequestMsgInterface");
			throw new RuntimeException( "Request \"" + tRqstMsg.getMessageName() + "\" does not implement interface RequestMsgInterface");
		}

		MessageInterface tResponseMessage = connectorMessage(pSessionId, (RequestMsgInterface) tRqstMsg );
		if (tResponseMessage instanceof StatusMessage) {
			if (!((StatusMessage) tResponseMessage).getIsOk().get()) {
				mFailureCount.incrementAndGet();
			}
		}

		return tResponseMessage;
	}


	public  MessageInterface connectorMessage(String pSessionId, RequestMsgInterface pRqstMsg) {
		SessionCntxInterface tSessionCntx = null;
		long tTxStartTime = (System.nanoTime() / 1000L);


		if ((pRqstMsg.getRef().isEmpty()) || (pRqstMsg.getRef().get().length() == 0)) {
			mLog.warn("message ref-id is empty, session-id: " + pSessionId);
			return StatusMessageBuilder.error("\"ref\" attribute is not set or empty", null);
		}
		String tRef = pRqstMsg.getRef().get();

		tSessionCntx = mSessionMapper.getById(pSessionId);
		if (tSessionCntx == null) {
			mLog.warn("session id : " + pSessionId.toString() + " is not valid any more");
			return StatusMessageBuilder.error("session id : " + pSessionId + " is not valid any more", tRef);
		}

		RequestContext tRequestContext = new RequestContext(pRqstMsg, tSessionCntx);

		if (mMsgLogger != null) {
			mMsgLogger.logRequestMessage( tRequestContext, pRqstMsg);
		}


		/****
		 * Matchine Engine Messages
		 */
		MatchingEngine tMatchingEngine = (MatchingEngine) TeAppCntx.getInstance().getService( TeService.MatchingService);
		MessageInterface tResponseMessage = null;
		if (pRqstMsg instanceof EngineMsgInterface) {

			if (pRqstMsg instanceof AddOrderRequest) {
				tResponseMessage = tMatchingEngine.executeAddOrder((AddOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof DeleteOrderRequest) {
				tResponseMessage = tMatchingEngine.executeDeleteOrder((DeleteOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof DeleteOrdersRequest) {
				tResponseMessage = tMatchingEngine.executeDeleteOrders((DeleteOrdersRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof AmendOrderRequest) {
				tResponseMessage = tMatchingEngine.executeAmendOrder((AmendOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof InternalPriceLevelRequest) {
				tResponseMessage = tMatchingEngine.executePriceLevel((InternalPriceLevelRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOrderbookRequest) {
				tResponseMessage = tMatchingEngine.executeQueryOrderbook((QueryOrderbookRequest) pRqstMsg, tRequestContext);
			} else {
				mLog.fatal("No execute implementation for ME request \"" + pRqstMsg.getMessageName() + "\"");
				tResponseMessage = StatusMessageBuilder.error(("No execute implementation for ME request \"" + pRqstMsg.getMessageName() + "\""), pRqstMsg.getRef().get());
			}
		} else {
			/*******
			 * Other messages
			 *******/
			if (pRqstMsg instanceof QueryTradesRequest) {
				tResponseMessage = TeAppCntx.getTradeContainer().queryTrades((QueryTradesRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryTradePricesRequest) {
				tResponseMessage = TeAppCntx.getTradeContainer().queryTradePrices((QueryTradePricesRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryPriceLevelsRequest) {
				tResponseMessage = TeAppCntx.getMarketDistributor().queryPriceLevels((QueryPriceLevelsRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOwnTradesRequest) {
				tResponseMessage = TeAppCntx.getTradeContainer().queryOwnTrades((QueryOwnTradesRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOwnOrdersRequest) {
				tResponseMessage = tMatchingEngine.executeQueryOwnOrders((QueryOwnOrdersRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QuerySymbolsRequest) {
				tResponseMessage = TeAppCntx.getInstrumentContainer().querySymbols((QuerySymbolsRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryMarketsRequest) {
				tResponseMessage = TeAppCntx.getInstrumentContainer().queryMarkets((QueryMarketsRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryBBORequest) {
				tResponseMessage = tMatchingEngine.executeQueryBBO((QueryBBORequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryPositionRequest) {
				tResponseMessage = tMatchingEngine.executeQueryPosition((QueryPositionRequest) pRqstMsg, tRequestContext);
			}  else if (pRqstMsg instanceof CryptoRedrawRequest) {
				tResponseMessage = tMatchingEngine.redrawCryptoRequest((CryptoRedrawRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryAddressEntriesRequest) {
				tResponseMessage = TeAppCntx.getPositionController().queryAddressEntries( (QueryAddressEntriesRequest) pRqstMsg, tRequestContext );
			}
			else {
				mLog.fatal("No execute implementation for request \"" + pRqstMsg.getMessageName() + "\"");
				tResponseMessage = StatusMessageBuilder.error(("No execute implementation for request \"" + pRqstMsg.getMessageName() + "\""), pRqstMsg.getRef().get());
			}
		}



		if (tResponseMessage == null) {
			tRequestContext.traceExecTime(mTraceExecTimeLimitUsec, mTraceExecTimeVerbose, mLog);
			mLog.fatal("response message not set in \"connectorMessage\"");
			throw new RuntimeException("response message not set in \"connectorMessage\"");
		}
		if (mMsgLogger != null) {
			if (mTraceExecTimeOff) {
				long tTxExecTime = (System.nanoTime() / 1000L) - tTxStartTime;
				mMsgLogger.logResponseMessage(tRequestContext, tResponseMessage, tTxExecTime);
			} else {
				mMsgLogger.logResponseMessage(tRequestContext, tResponseMessage);
			}
		}
		tRequestContext.timestamp("message to message logger");
		tRequestContext.timestamp((pRqstMsg.getMessageName() + " COMPLETE"));
		if (mTraceExecTimeOff) {
			tRequestContext.traceExecTime(mTraceExecTimeLimitUsec, mTraceExecTimeVerbose, mLog);
		}


		return tResponseMessage;
	}


	@Override
	public LogonResponse logon(String pSessionId, String pJsonRqstMsgStr ) throws TeException
	{
		mRequestCount.incrementAndGet();
		SessionCntxInterface mSessionCntx = null;
		String tRef = AuxJson.getMessageRef( pJsonRqstMsgStr );
		Pattern tAccountPattern = Pattern.compile("\"account\"\\s*:\\s*\"([^\"]+)\"");
		Matcher m = tAccountPattern.matcher( pJsonRqstMsgStr );
		LogonResponse tLogonRsp = new LogonResponse();

		String tUsrStr = (m.find()) ? m.group(1) : "null";

		try {mSchemaValidator.validate( pJsonRqstMsgStr );}
		catch( Exception e) {
			mFailureCount.incrementAndGet();
			mLog.warn("Logon invalid message syntax: " + tUsrStr + " reason: " + e.getMessage());
			throw new TeException(HttpStatus.BAD_REQUEST.value(), StatusMessageBuilder.error("Inavlid message syntax", tRef));
		}

		try {
			LogonRequest tLogonRequest = new LogonRequest(pJsonRqstMsgStr);
			mSessionCntx = this.logoninternal(tLogonRequest, pSessionId);
			mLog.info("successfull logon: " + tUsrStr + " session id: " + pSessionId);
			return tLogonRsp.setRef(tRef).setSessionAuthId(mSessionCntx.getApiAuthId());
		}
		catch( TeException te) {
			mFailureCount.incrementAndGet();
			throw te;
		}
	}

	@Override
	public MessageInterface ipcRequest(MessageInterface pMgmtRequest) {
		if (pMgmtRequest instanceof MgmtGetLogMessagesRequest) {
			return mgmtGetlogMsgData((MgmtGetLogMessagesRequest) pMgmtRequest );
		}
		if (pMgmtRequest instanceof MgmtQueryActiveSessionsRequest) {
			return mgmtGetActiveSessions((MgmtQueryActiveSessionsRequest) pMgmtRequest );
		}
		throw new RuntimeException("mgmt request not supported " + pMgmtRequest.getMessageName());
	}


	private boolean matchName(Path pPath, String pDateStr ) {
		Pattern tNamePatter = (pDateStr == null) ?
				Pattern.compile("TeMessageLog-(.+)\\.json") :
				Pattern.compile("TeMessageLog-" + pDateStr.replace('-','_') + "(.+)\\.json");
		return tNamePatter.matcher(pPath.getFileName().toString()).matches();
	}

	private List<File> listMsgLogFiles( String pDateStr) {
		String tLogDir = "./";
		List<File> tMsgLogFiles = new ArrayList<>();
		try {
			String tFilename = AuxJson.navigateString(mConfiguration,"messageLoggerFile", "logs/TeMessageLoggger-%datetime%.log");
			int tIdx = tFilename.lastIndexOf("/");
			if ( tIdx >=1 ) { tLogDir = tFilename.substring(0, tIdx); }

			Files.walk(Paths.get(tLogDir))
					.filter(Files::isRegularFile)
					.filter( file -> matchName( file , pDateStr ))
					.forEach( file -> tMsgLogFiles.add( file.toFile()));
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return tMsgLogFiles;
	}

	private MgmtQueryActiveSessionsResponse mgmtGetActiveSessions( MgmtQueryActiveSessionsRequest pRequest ) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<MgmtStatEntry> tStatCounter = new ArrayList<>();
		MgmtQueryActiveSessionsResponse tResponse = new MgmtQueryActiveSessionsResponse().setRef( pRequest.getRef().get());

		tStatCounter.add( new MgmtStatEntry().setAttribute("Max active session").setValue( String.valueOf(mSessionMapper.getMaxSessions())));
		tStatCounter.add( new MgmtStatEntry().setAttribute("Request counter").setValue( String.valueOf( mRequestCount.get())));
		tStatCounter.add( new MgmtStatEntry().setAttribute("Request failure counter").setValue( String.valueOf( mFailureCount.get())));

		tResponse.setSysStarTime( mStartTime );
		tResponse.addSessionCounters( tStatCounter );
		tResponse.setSessions( mSessionMapper.getActiveSessions());
		return tResponse;
	}

	private MgmtGetLogMessagesResponse mgmtGetlogMsgData( MgmtGetLogMessagesRequest pRequest )
	{

		List<MsgLogEntry> tLogMessages = new ArrayList<>();
		List<File> tMsgLogFiles = listMsgLogFiles( pRequest.getDateFilter().orElse( null ));
		for( File tFile : tMsgLogFiles) {
			if (!scanMsgLogFile(tFile, pRequest, tLogMessages)) {
				break;
			}
		}

		MgmtGetLogMessagesResponse tResponse = new MgmtGetLogMessagesResponse().setRef( pRequest.getRef().get());
		tResponse.setLogMessages( tLogMessages );
		return tResponse;
	}

	private boolean scanMsgLogFile( File pFile, MgmtGetLogMessagesRequest pRequest, List<MsgLogEntry> pMsgLogEntries ) {
		MgmtMsgLogFilter tFilter = new MgmtMsgLogFilter( pRequest );
		int tMaxLines = pRequest.getMaxLines().get();
		String tLine;


		try {
			BufferedReader fp = new BufferedReader(new FileReader( pFile ));
			while ((tLine = fp.readLine()) != null) {
				MsgLogEntry tLogEntry = tFilter.match(tLine);
				if (tLogEntry != null) {
					pMsgLogEntries.add(tLogEntry);
					if (pMsgLogEntries.size() >= tMaxLines) {
						fp.close();
						return false;
					}
				}
			}
			fp.close();
		}
		catch( IOException e) {
			mLog.error("failed to open TE message log file ", e);
			return false;
		}

		return true;
	}
}
