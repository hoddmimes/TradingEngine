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

package com.hoddmimes.te.connector;

import com.google.gson.JsonObject;
import com.hoddmimes.te.TeAppCntx;
import com.hoddmimes.te.TeCoreService;
import com.hoddmimes.te.common.AuxJson;
import com.hoddmimes.te.common.interfaces.ConnectorInterface;
import com.hoddmimes.te.common.ipc.IpcService;

public abstract class ConnectorBase  implements ConnectorInterface
{
	protected ConnectorInterface.ConnectorCallbackInterface mCallback;
	protected JsonObject mConnectorConfig;


	protected ConnectorBase(JsonObject pTeConfiguration, ConnectorInterface.ConnectorCallbackInterface pCallback ) {
		mConnectorConfig = AuxJson.navigateObject(TeAppCntx.getInstance().getTeConfiguration(), "TeConfiguration/connectorConfiguration");
		mCallback = pCallback;
	}



}
