package com.hoddmimes.te.sessionctl;


import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.*;
import com.hoddmimes.te.common.interfaces.AuthenticateInterface;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;
import com.hoddmimes.te.engine.MeRqstCntx;
import com.hoddmimes.te.messages.StatusMessageBuilder;
import com.hoddmimes.te.messages.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SessionController implements ConnectorInterface.ConnectorCallbackInterface
{
	private static  final String INTERNAL_SESSION_ID = UUID.randomUUID().toString();

	private Logger mLog = LogManager.getLogger( SessionController.class);

	private AuthenticateInterface mAuthenticator = null;
	private JsonObject mConfiguration = null;
	private HashMap<String, SessionCntxInterface> mSessions = null;
	private SessionCntxInterface mInternalSession;


	private JsonSchemaValidator mSchemaValidator;
	private MessageFactory mMessageFactory;

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

	private void loadConfiguration( JsonObject pTeConfigurationFile ) {
		    mConfiguration = AuxJson.navigateObject( pTeConfigurationFile,"TeConfiguration/sessionControllerConfiguration").getAsJsonObject();
			initializeAuthenticator();
			String tSchemaSource = AuxJson.navigateString( mConfiguration, "schemaDefinitions");
			mSchemaValidator = new JsonSchemaValidator( tSchemaSource );
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


	public SessionCntxInterface logoninternal(String pUsername, String pPassword, String pSessionId) throws TeException{
		if (pSessionId == null) {
			throw new TeException("Logon, user session id object for user \"" + pUsername + "\" must not be null");
		}
		synchronized (SessionController.class) {
			if (mSessions.containsKey(pSessionId)) {
				return mSessions.get(pSessionId); // already signed in
			}
			if (mAuthenticator.logon(pUsername, pPassword)) {
				SessionCntxInterface tSessCntx = new SessionCntx(pUsername, pSessionId);
				mSessions.put(pSessionId, tSessCntx);
				return tSessCntx;
			}
		}
		throw new TeException("Logon failure user \"" + pUsername + "\"");
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
		SessionCntxInterface tSessionCntx = null;
		String tRef = AuxJson.getMessageRef(pJsonRqstMsgStr);
		MeRqstCntx tMeRqstCntx = null;


		// Validate message syntax
		try {
		} catch (Exception e) {
			return StatusMessageBuilder.error("Invalid request syntax", tRef, e);
		}

		synchronized (SessionController.class) {
			tSessionCntx = mSessions.get(pSessionId);
		}
		if (tSessionCntx == null) {
			mLog.warn("session id : " + pSessionId.toString() + " is not valid any more");
			return StatusMessageBuilder.error("session id : " + pSessionId + " is not valid any more", tRef);
		}

		MessageInterface tRqstMsg = mMessageFactory.getMessageInstance(pJsonRqstMsgStr);

		if (tRqstMsg instanceof AddOrderRequest) {
			tMeRqstCntx = TeAppCntx.getInstance().getMatchingEngineFrontend().queue(tSessionCntx, (AddOrderRequest) tRqstMsg);
		}

		if (tRqstMsg == null) {
			return null;
		}

		return tMeRqstCntx.waitForCompleation();
	}

	@Override
	public LogonResponse logon(String pSessionId, String pJsonRqstMsgStr )
	{
		SessionCntxInterface mSessionCntx = null;
		String tRef = AuxJson.getMessageRef( pJsonRqstMsgStr );
		Pattern tUsrPattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]+)\"");
		Matcher m = tUsrPattern.matcher( pJsonRqstMsgStr );
		LogonResponse tRsp = new LogonResponse();

		String tUsrStr = (m.find()) ? m.group(1) : "null";

		try {mSchemaValidator.validate( pJsonRqstMsgStr );}
		catch( Exception e) {
			mLog.warn("Logon invalid message syntax: " + tUsrStr + " reason: " + e.getMessage());
			return tRsp.setIsOk(false).setRef(tRef).setStatusMessage("Invalid message syntax").setExceptionMessage( e.getMessage());
		}
		try {
			LogonRequest tLoginMsg = new LogonRequest( pJsonRqstMsgStr );
			mSessionCntx = this.logoninternal( tLoginMsg.getUsername().get(), tLoginMsg.getPassword().get(), pSessionId );
			mLog.info("successfull logon: " + tUsrStr + " session id: " + pSessionId);
			return tRsp.setIsOk(true).setRef(tRef).setStatusMessage("Successfull signed on").setSessionAuthId( mSessionCntx.getApiAuthId());

		}
		catch(Exception e) {
			mLog.warn("Logon unauthorized user: " + tUsrStr + " reason: " + e.getMessage());
			return tRsp.setIsOk(false).setRef(tRef).setStatusMessage("Logon not authorized").setExceptionMessage( e.getMessage());
		}
	}
}
