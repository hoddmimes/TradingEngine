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

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.transport.http.TeHttpClient;
import com.hoddmimes.te.common.transport.http.TeRequestException;
import com.hoddmimes.te.common.transport.http.TeWebsocketClient;
import com.hoddmimes.te.messages.generated.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Connector implements TeWebsocketClient.WssCallback
{
	private final static String DEFULT_HTTP_URI = "https://localhost:8883/te-trading/";
	private final static String DEFULT_WSS_URI = "wss://localhost:8883/te-marketdata/";

	private TeWebsocketClient mWssClient;
	private TeHttpClient      mHttpClient;
	private String            mHttpUri;
	private String            mWssUri;
	private String            mAccount;
	private String            mPassword;

	private List<Market>      mMarkets;
	private List<Symbol>      mInstruments;
	private List<TeBroadcastListener> mBdxListners;
	private MessageFactory      mMessageFactory;


	public Connector() {
		this( null, null, null, null );
	}

	public Connector(String pHttpUri, String pWssUri, String pAccount, String pPassword ) {
		mHttpUri = (pHttpUri == null) ? DEFULT_HTTP_URI : pHttpUri;
		mWssUri = (pWssUri == null) ? DEFULT_WSS_URI : pWssUri;
		mAccount = (pAccount == null) ? "" : pAccount;
		mPassword = (pPassword == null) ? "" : pPassword;

		mBdxListners = new ArrayList<>();
		mMessageFactory = new MessageFactory();

		mHttpClient = new TeHttpClient( mHttpUri, false);

		LoginDialog tLoginDialog = new LoginDialog();
		tLoginDialog.waitForSuccessfullLogin();
		loadInstrumentData();
	}

	@Override
	public void onOpen(javax.websocket.Session session, javax.websocket.EndpointConfig config) {

	}

	@Override
	public void onMessage(String pBdxMsg) {
		MessageInterface tBdxMsg = mMessageFactory.getMessageInstance( pBdxMsg );
		if (tBdxMsg == null) {
			System.out.println("Unknown broadcast: " + pBdxMsg );
			return;
		}
		for( TeBroadcastListener tListener : mBdxListners) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					tListener.onTeBdx( tBdxMsg );
				}
			});
		}
	}

	@Override
	public void onError(javax.websocket.Session session, Throwable throwable) {

	}

	@Override
	public void onClose(javax.websocket.Session session, javax.websocket.CloseReason closeReason) {

	}

	public void addSubription( TeBroadcastListener pBdxListener ) {
		mBdxListners.add( pBdxListener );
	}

	public List<Market> getMarkets() {
		return mMarkets;
	}

	public List<Symbol> getInstruments() {
		return mInstruments;
	}

	public List<String> getSymbolIdentities() {
		return mInstruments.stream().map( s -> s.getSid().get()).toList();
	}

	public Symbol getInstrument( String mSid ) {
		for( Symbol s : mInstruments ) {
			if (s.getSid().get().contentEquals( mSid )) {
				return s;
			}
		}
		return null;
	}


	public JsonObject get( String pEndpoint) throws IOException, TeRequestException {
		return mHttpClient.get( pEndpoint );
	}

	public JsonObject post( String pOrderRequest, String pEndpoint ) throws IOException, TeRequestException {
		return mHttpClient.post( pOrderRequest, pEndpoint );
	}

	public JsonObject post( JsonObject jOrderRequest, String pEndpoint ) throws IOException, TeRequestException {
		return mHttpClient.post( jOrderRequest, pEndpoint );
	}

	private void loadInstrumentData() {
		MessageFactory tMessageFactory = new MessageFactory();

		try {
			// Load Markets
			JsonObject jMktRsp = mHttpClient.get("queryMarkets");
			QueryMarketsResponse tMktRsp = (QueryMarketsResponse) tMessageFactory.getMessageInstance( AuxJson.tagMessageBody(QueryMarketsResponse.NAME, jMktRsp ));
			mMarkets = tMktRsp.getMarkets().get();

			// Load Instruments
			mInstruments = new ArrayList<>();
			for( Market m : mMarkets ) {
				JsonObject jSymolsRsp = mHttpClient.get("querySymbols/" + String.valueOf( m.getId().get()));
				QuerySymbolsResponse tSymbolsRsp = (QuerySymbolsResponse) tMessageFactory.getMessageInstance( AuxJson.tagMessageBody(QuerySymbolsResponse.NAME, jSymolsRsp ));
				mInstruments.addAll( tSymbolsRsp.getSymbols().get());
			}
			Collections.sort( mInstruments, new InstrumentSort());
			System.out.println("loaded " +mInstruments.size() + " instruments");
		} catch (IOException | TeRequestException e) {
			JOptionPane.showMessageDialog(null,
					"Load markets failure, reason: " + e.getMessage(),
					"Load Data Failure",
					JOptionPane.WARNING_MESSAGE);
			System.exit(0);
		}
	}




	class LoginDialog extends JFrame  {
		JButton mLoginBtn;
		JTextField mAccountTxt;
		JPasswordField mPasswordTxt;

		LoginDialog() {
		  this.setTitle("TE Test Client");
		  this.init();
		  this.pack();
		  AuxClt.centeredFrame( this );
		  this.setVisible( true );
		}


		void init() {
			JPanel tRootPanel = new JPanel( new GridBagLayout() );
			tRootPanel.setToolTipText("Beware his is simple test client for making dynamic test a bit easier. A real client implementation is likely to be a web application structured in a completely different way");
			tRootPanel.setBorder( new EtchedBorder( EtchedBorder.RAISED ));
			GridBagConstraints gc = new GridBagConstraints();
			gc.anchor = GridBagConstraints.NORTHWEST;
			gc.gridx = gc.gridy = 0;
			gc.insets = new Insets(20,20, 0, 20 );

			JLabel tUserLbl = AuxClt.makelabel("Account", 65);
			tRootPanel.add( tUserLbl, gc);

			gc.gridx++;
			mAccountTxt = AuxClt.maketxtfld( mAccount, 182);
			tRootPanel.add( mAccountTxt, gc);

			gc.gridx = 0; gc.gridy++;
			JLabel tPwdLbl = AuxClt.makelabel("Password", 65);
			tRootPanel.add( tPwdLbl, gc);

			gc.gridx++;
			mPasswordTxt = AuxClt.makepwdfld( mPassword, 182);
			tRootPanel.add( mPasswordTxt, gc);

			gc.gridwidth = 2;
			gc.gridx = 0; gc.gridy++;
			gc.anchor = GridBagConstraints.CENTER;
			gc.insets.top = 30;
			gc.insets.bottom = 20;
			mLoginBtn = AuxClt.makebutton( "Login", 90);
			tRootPanel.add( mLoginBtn, gc);

			mLoginBtn.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					doLogin();
				}
			});


			this.setContentPane( tRootPanel );
			this.getRootPane().setDefaultButton(mLoginBtn);
		}

		private void doLogin() {
			LogonRequest tRqst = new LogonRequest();
			tRqst.setAccount( mAccountTxt.getText());
			tRqst.setPassword( mPasswordTxt.getText());
			tRqst.setRef("lr");

			JsonObject jRequest = AuxJson.getMessageBody(tRqst.toJson());

			try {
				JsonObject jResponse = mHttpClient.post( jRequest, "logon" );
				if (jResponse.has("sessionAuthId")) {
					String tAuthId = jResponse.get("sessionAuthId").getAsString();
					mWssClient = new TeWebsocketClient( mWssUri, tAuthId, Connector.this );
					setupSubscriptions();
					this.dispose();
					synchronized ( this ) {
						this.notifyAll();
					}
				} else {
					JOptionPane.showMessageDialog(this,
							"Login failure, reason: " + jResponse.toString(),
							"Login failure",
							JOptionPane.WARNING_MESSAGE);
					return;
				}
			}
			catch( IOException ie) {
				JOptionPane.showMessageDialog(this,
						"Failed to connect to TE system, reason: " + ie.getMessage(),
						"Service not avaiable",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
			catch( TeRequestException te) {
				JOptionPane.showMessageDialog(this,
						"Login failed, reason: " + te.getMessage(),
						"Login Failure",
						JOptionPane.WARNING_MESSAGE);
				return;
			}
		}

		void setupSubscriptions() {
			JsonObject jSubscriptonRqst = new JsonObject();
			jSubscriptonRqst.addProperty("command","ADD");

			// Setup subscriptions for BBO
			jSubscriptonRqst.addProperty("topic",  "/BdxBBO/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );

			// Setup subscriptions for BdxOrderbookChange
			jSubscriptonRqst.addProperty("topic",  "/BdxOrderbookChange/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );

			// Setup subscriptions for BdxTrade
			jSubscriptonRqst.addProperty("topic",  "/BdxTrade/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );

			// Setup subscriptions for BdxOwnTrade
			jSubscriptonRqst.addProperty("topic",  "/BdxOwnTrade/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );

			// Setup subscriptions for BdxPriceLevel
			jSubscriptonRqst.addProperty("topic",  "/BdxPriceLevel/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );

			// Setup subscriptions for BdxOwnOrderbookChange
			jSubscriptonRqst.addProperty("topic",  "/BdxOwnOrderbookChange/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );


			// Setup subscriptions for BdxOwnOrderbookChange
			jSubscriptonRqst.addProperty("topic",  "/BdxOwnTrade/...");
			mWssClient.sendMessage( jSubscriptonRqst.toString() );
		}


		void waitForSuccessfullLogin() {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {}
			}
		}


	}

	static class InstrumentSort implements Comparator<Symbol>
	{
		@Override
		public int compare(Symbol sym1, Symbol  sym2) {
			return sym1.getSid().get().compareTo( sym2.getSid().get());
		}
	}
}
