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

