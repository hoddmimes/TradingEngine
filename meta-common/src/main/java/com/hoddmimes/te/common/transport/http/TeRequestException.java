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

package com.hoddmimes.te.common.transport.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class TeRequestException extends Exception {
	String mReasonPhrase;
	String mBodyReason;
	int mStatusCode;

	public TeRequestException(int pStatus, String pReasonPhrase, String pBodyResponse) {
		super();
		mStatusCode = pStatus;
		mReasonPhrase = pReasonPhrase;
		mBodyReason = pBodyResponse;
	}

	public JsonObject toJson() {
		JsonObject objRoot = new JsonObject();
		JsonObject objbody = new JsonObject();

		objbody.addProperty("statusCode", mStatusCode);
		if (mReasonPhrase != null) {
			objbody.addProperty("reason", mReasonPhrase);
		}
		if (mBodyReason != null) {
			Gson tGson = new Gson();
			try {
				JsonObject jObj = JsonParser.parseString(mBodyReason).getAsJsonObject();
				objbody.add("stsmsg", jObj);
			} catch(JsonSyntaxException ex) {
				objbody.addProperty("stsmsg", mBodyReason);
			}
		}
		objRoot.add("exception", objbody);
		return objRoot;
	}
}

