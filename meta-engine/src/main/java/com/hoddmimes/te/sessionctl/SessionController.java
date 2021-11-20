package com.hoddmimes.te.sessionctl;


import com.google.gson.JsonObject;
import com.hoddmimes.jaux.AuxTimestamp;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.TeException;
import com.hoddmimes.te.common.interfaces.AuthenticateInterface;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.messages.EngineMsgInterface;
import com.hoddmimes.te.messages.RequestMsgInterface;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.HTTP;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionController implements ConnectorInterface.ConnectorCallbackInterface
{
	public static final String INTERNAL_SESSION_ID = UUID.randomUUID().toString();

	private Logger mLog = LogManager.getLogger( SessionController.class);

	private AuthenticateInterface mAuthenticator = null;
	private JsonObject mConfiguration = null;
	private HashMap<String, SessionCntxInterface> mSessions = null;
	private SessionCntxInterface mInternalSession;

	private boolean mIsMessageLoggerEnabled;
	private long mMessagerLoggerFlushInterval;
	private MessageLogger.FlushMode mMessageLoggerFlushMode;
	private MessageLogger mMsgLogger;


	private JsonSchemaValidator mSchemaValidator;
	private MessageFactory mMessageFactory;
	private long mTraceExecTimeLimitUsec;
	private boolean mTraceExecTimeVerbose;

	public SessionController( JsonObject pTeConfigurationFile ) throws IOException
	{
		mSessions = new HashMap<>();
		mInternalSession = new SessionCntx("internal",INTERNAL_SESSION_ID);
		mSessions.put(INTERNAL_SESSION_ID, mInternalSession);
		loadConfiguration( pTeConfigurationFile );
		mMessageFactory = new MessageFactory();
		TeAppCntx.getInstance().setSessionController( this );
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
			String tAuthDataStore = AuxJson.navigateString( mConfiguration,"autheticator/dataStore");
			Class[] cArg = new Class[1];
			cArg[0] = String.class;
			Class c = Class.forName(tAuthClsStr);
			mAuthenticator = (AuthenticateInterface) c.getDeclaredConstructor( cArg ).newInstance(tAuthDataStore);
		} catch (Exception e) {
			mLog.error("Failed to instansiate Authenticator class \"" + tAuthClsStr +"\" reason: " + e.getMessage());
		}
	}

	public boolean validateAuthId( String pAuthId ) {
		synchronized( SessionController.class ) {
			return mSessions.values().stream().anyMatch( s -> s.getApiAuthId().contentEquals( pAuthId ));
		}
	}


	public SessionCntxInterface logoninternal(LogonRequest pLogonRequest, String pSessionId) throws TeException
	{
		if (pSessionId == null) {
			mLog.warn(("Logon, user session id object for user " + pLogonRequest.getAccount().get() + " must not be null"));
			throw new TeException( HttpStatus.BAD_REQUEST.value(),
					StatusMessageBuilder.error(("Logon, user session id object for user " + pLogonRequest.getAccount().get() + " must not be null"), pLogonRequest.getRef().get()));
		}

		synchronized (SessionController.class) {
			if (mSessions.containsKey(pSessionId)) {
				return mSessions.get(pSessionId); // already signed in
			}
			if (mAuthenticator.logon(pLogonRequest.getAccount().get(), pLogonRequest.getPassword().get())) {
				SessionCntxInterface tSessCntx = new SessionCntx(pLogonRequest.getAccount().get(), pSessionId);
				mSessions.put(pSessionId, tSessCntx);
				return tSessCntx;
			}
		}
		mLog.warn("Unauthorized logon, account: " + pLogonRequest.getAccount().get() + " sessionid: " + pSessionId);
		throw new TeException( HttpStatus.UNAUTHORIZED.value(),
				StatusMessageBuilder.error("Unauthorized logon", pLogonRequest.getRef().get()));
	}

	public SessionCntxInterface getSessionContext( String pSessionId ) {
		synchronized (SessionController.class) {
			return mSessions.get(pSessionId);
		}
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
		SessionCntxInterface tSessCntx = null;
		synchronized (SessionController.class) {
			tSessCntx = mSessions.remove( pSessionId );
		}
		return tSessCntx;
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
	public SessionCntxInterface getSessionCntx(String pSessionId) {
		synchronized (SessionController.class) {
			return mSessions.get(pSessionId);
		}
	}
	public SessionCntxInterface getSessionCntxByAuthId(String pApiAuthId) {
		synchronized (SessionController.class) {
			for( SessionCntxInterface sc : mSessions.values()) {
				if (pApiAuthId.contentEquals(sc.getApiAuthId())) {
					return sc;
				}
			}
		}
		return null;
	}

	public MessageInterface validateMessage( String pJsonRqstMsgStr ) throws Exception
	{
		mSchemaValidator.validate( pJsonRqstMsgStr);
		return mMessageFactory.getMessageInstance( pJsonRqstMsgStr );
	}



	@Override
	public MessageInterface connectorMessage(String pSessionId, String pJsonRqstMsgStr) {

		String tRef = AuxJson.getMessageRef(pJsonRqstMsgStr);

		// Validate message syntax
		try {
			mSchemaValidator.validate(pJsonRqstMsgStr);
		} catch (Exception e) {
			return StatusMessageBuilder.error("Invalid request message syntax", tRef, e);
		}

		MessageInterface tRqstMsg = mMessageFactory.getMessageInstance(pJsonRqstMsgStr);
		if (!(tRqstMsg instanceof RequestMsgInterface)) {
			mLog.error("Request \"" + tRqstMsg.getMessageName() + "\" does not implement interface RequestMsgInterface");
			throw new RuntimeException( "Request \"" + tRqstMsg.getMessageName() + "\" does not implement interface RequestMsgInterface");
		}

		return connectorMessage(pSessionId, (RequestMsgInterface) tRqstMsg );
	}


	public  MessageInterface connectorMessage(String pSessionId, RequestMsgInterface pRqstMsg) {
		SessionCntxInterface tSessionCntx = null;

		if ((pRqstMsg.getRef().isEmpty()) || (pRqstMsg.getRef().get().length() == 0)) {
			mLog.warn("message ref-id is empty, session-id: " + pSessionId);
			return StatusMessageBuilder.error("\"ref\" attribute is not set or empty", null);
		}
		String tRef = pRqstMsg.getRef().get();

		synchronized (SessionController.class) {
			tSessionCntx = mSessions.get(pSessionId);
		}
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
		MessageInterface tResponseMessage = null;
		if (pRqstMsg instanceof EngineMsgInterface) {

			if (pRqstMsg instanceof AddOrderRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeAddOrder((AddOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof DeleteOrderRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeDeleteOrder((DeleteOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof DeleteOrdersRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeDeleteOrders((DeleteOrdersRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof AmendOrderRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeAmendOrder((AmendOrderRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof InternalPriceLevelRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executePriceLevel((InternalPriceLevelRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOrderbookRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeQueryOrderbook((QueryOrderbookRequest) pRqstMsg, tRequestContext);
			} else {
				mLog.fatal("No execute implementation for ME request \"" + pRqstMsg.getMessageName() + "\"");
				tResponseMessage = StatusMessageBuilder.error(("No execute implementation for ME request \"" + pRqstMsg.getMessageName() + "\""), pRqstMsg.getRef().get());
			}
		} else {
			/*******
			 * Other messages
			 *******/
			if (pRqstMsg instanceof QueryTradePricesRequest) {
				tResponseMessage = TeAppCntx.getInstance().getTradeContainer().queryTradePrices((QueryTradePricesRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryTradePriceRequest) {
				tResponseMessage = TeAppCntx.getInstance().getTradeContainer().queryTradePrice((QueryTradePriceRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryPriceLevelsRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMarketDataDistributor().queryPriceLevels((QueryPriceLevelsRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOwnTradesRequest) {
				tResponseMessage = TeAppCntx.getInstance().getTradeContainer().queryOwnTrades((QueryOwnTradesRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryOwnOrdersRequest) {
				tResponseMessage = TeAppCntx.getInstance().getMatchingEngine().executeQueryOwnOrders((QueryOwnOrdersRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QuerySymbolsRequest) {
				tResponseMessage = TeAppCntx.getInstance().getInstrumentContainer().querySymbols((QuerySymbolsRequest) pRqstMsg, tRequestContext);
			} else if (pRqstMsg instanceof QueryMarketsRequest) {
				tResponseMessage = TeAppCntx.getInstance().getInstrumentContainer().queryMarkets((QueryMarketsRequest) pRqstMsg, tRequestContext);
			}
			else {
				mLog.fatal("No execute implementation for request \"" + pRqstMsg.getMessageName() + "\"");
				tResponseMessage = StatusMessageBuilder.error(("No execute implementation for request \"" + pRqstMsg.getMessageName() + "\""), pRqstMsg.getRef().get());
			}
		}

		tRequestContext.timestamp((pRqstMsg.getMessageName() + " completed"));
		tRequestContext.traceExecTime(mTraceExecTimeLimitUsec, mTraceExecTimeVerbose, mLog);

		if (tResponseMessage == null) {
			mLog.fatal("response message not set in \"connectorMessage\"");
			throw new RuntimeException("response message not set in \"connectorMessage\"");
		}
		if (mMsgLogger != null) {
			mMsgLogger.logResponseMessage( tRequestContext, tResponseMessage);
		}
		return tResponseMessage;
	}


	@Override
	public LogonResponse logon(String pSessionId, String pJsonRqstMsgStr ) throws TeException
	{
		SessionCntxInterface mSessionCntx = null;
		String tRef = AuxJson.getMessageRef( pJsonRqstMsgStr );
		Pattern tUsrPattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
		Matcher m = tUsrPattern.matcher( pJsonRqstMsgStr );
		LogonResponse tLogonRsp = new LogonResponse();

		String tUsrStr = (m.find()) ? m.group(1) : "null";

		try {mSchemaValidator.validate( pJsonRqstMsgStr );}
		catch( Exception e) {
			mLog.warn("Logon invalid message syntax: " + tUsrStr + " reason: " + e.getMessage());
			throw new TeException(HttpStatus.BAD_REQUEST.value(), StatusMessageBuilder.error("Inavlid message syntax", tRef));
		}

		LogonRequest tLogonRequest = new LogonRequest( pJsonRqstMsgStr );
		mSessionCntx = this.logoninternal( tLogonRequest, pSessionId );
		mLog.info("successfull logon: " + tUsrStr + " session id: " + pSessionId);
		return tLogonRsp.setRef(tRef).setSessionAuthId( mSessionCntx.getApiAuthId());
	}
}
