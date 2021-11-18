package com.hoddmimes.te.testclient;

import com.google.gson.JsonObject;

public class TeRequestException extends Exception
{
	String mReasonPhrase;
	String mBodyReason;
	int mStatusCode;

	public TeRequestException(int pStatus, String pReasonPhrase, String pBodyResponse)
	{
		super();
		mStatusCode = pStatus;
		mReasonPhrase = pReasonPhrase;
		mBodyReason = pBodyResponse;
	}

	public JsonObject toJson() {
		JsonObject objRoot = new JsonObject();
		JsonObject objbody = new JsonObject();

		objbody.addProperty("statusCode: ", mStatusCode);
		if (mReasonPhrase != null) {
			objbody.addProperty("reason: ", mReasonPhrase);
		}
		if (mBodyReason != null) {
			objbody.addProperty("stsmsg: ", mBodyReason);
		}
		objRoot.add("Exception: ", objbody);
		return objRoot;
	}


}
