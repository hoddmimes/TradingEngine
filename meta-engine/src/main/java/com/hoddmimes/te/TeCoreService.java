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

package com.hoddmimes.te;

import com.google.gson.JsonObject;
import com.hoddmimes.te.common.interfaces.TeService;
import com.hoddmimes.te.common.ipc.IpcRequestCallbackInterface;
import com.hoddmimes.te.common.ipc.IpcService;

public abstract class TeCoreService implements IpcRequestCallbackInterface
{
	protected JsonObject mTeConfiguration;
	protected IpcService mIpcService;
	private volatile boolean mSynchronized;

	public TeCoreService(JsonObject pTeConfiguration, IpcService pIpcService) {
		mTeConfiguration = pTeConfiguration;
		mIpcService = pIpcService;
		mSynchronized = false;
	}


	public abstract TeService getServiceId();


	public void waitForService() {
		synchronized( this ) {
			if (mSynchronized) {
				return;
			}
			try {this.wait();}
			catch( InterruptedException e) {}
		}
	}


	public void serviceIsSynchronized() {
		synchronized( this ) {
			mSynchronized = true;
			this.notifyAll();
		}
	}
}
