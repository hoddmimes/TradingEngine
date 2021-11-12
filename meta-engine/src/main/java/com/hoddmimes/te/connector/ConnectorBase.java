package com.hoddmimes.te.connector;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.JsonSchemaValidator;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;

public abstract class ConnectorBase implements ConnectorInterface
{
	protected ConnectorInterface.ConnectorCallbackInterface mCallback;
	protected JsonObject mConfiguration;


	protected ConnectorBase(JsonObject pTeConfiguration, ConnectorInterface.ConnectorCallbackInterface pCallback ) {
		mConfiguration = AuxJson.navigateObject(TeAppCntx.getInstance().getTeConfiguration(), "TeConfiguration/connectorConfiguration");
		mCallback = pCallback;
	}



}
