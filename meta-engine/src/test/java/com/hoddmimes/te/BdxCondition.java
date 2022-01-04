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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public class BdxCondition implements BooleanSupplier
{
	String       mBdxName;
	JsonObject   jConditions[];
	JsonObject   jBdxMsg;
	AtomicBoolean mMatchFound;
	AtomicBoolean mConditionTimeout;

	BdxCondition( String pBdxName, String ... pConditions ) {
		mMatchFound = new AtomicBoolean( false );
		mConditionTimeout = new AtomicBoolean( false );

		mBdxName = pBdxName;
		if ((pConditions != null) && (pConditions.length > 0)) {
			jConditions = new JsonObject[ pConditions.length];
			for (int i = 0; i < pConditions.length; i++) {
				jConditions[i] = JsonParser.parseString( toJsonString(pConditions[i])).getAsJsonObject();
			}
		}
	}

	boolean matchConditionUntil( long mTimeout ) {
		long tExiprationTime = System.currentTimeMillis() + mTimeout;
		while( System.currentTimeMillis() < tExiprationTime ) {
			if (mMatchFound.get()) {
				return true;
			}
			try { Thread.sleep(100L); }
			catch( InterruptedException ie) {}
		}

		mConditionTimeout.set( true );
		return false;
	}

	public boolean  isTimedOut() {
		return mConditionTimeout.get();
	}

	public static String condition( String pKey ) {
		return "{'key' : '" + pKey + "'}";
	}

	public static String condition( String pKey, String pValue ) {
			return "{'key' : '" + pKey + "', 'value' : '" + pValue + "'}";
	}

	public static String condition( String pKey, Integer pValue ) {
			return "{'key' : '" + pKey + "', 'value' : " + pValue + "}";
	}

	public static String condition( String pKey, Long pValue ) {
			return "{'key' : '" + pKey + "', 'value' : " + pValue + "}";
	}

	public static String condition( String pKey, Double pValue ) {
		return "{'key' : '" + pKey + "', 'value' : " + pValue + "}";
	}

	public static String condition( String pKey, Boolean pValue ) {
			return "{'key' : '" + pKey + "', 'value' : " + pValue + "}";
	}



	boolean matchReceivedBdx( String pBdxMsg ) {
		jBdxMsg = JsonParser.parseString(toJsonString(pBdxMsg)).getAsJsonObject();
		String tBdxName = jBdxMsg.keySet().iterator().next();
		if (!tBdxName.contentEquals( mBdxName )) {
			return false;
		}
		JsonObject jBdx = jBdxMsg.get( tBdxName ).getAsJsonObject();

		if (jConditions != null) {
			for (JsonObject jc : jConditions) {
				if (!jBdx.has(jc.get("key").getAsString())) {
					return false;
				}
				if (jc.has("value")) {
					JsonElement tBdxValue = jBdx.get( jc.get("key").getAsString());
					JsonElement tCondValue = jc.get("value");
					if (tBdxValue.getAsString().compareTo( tCondValue.getAsString()) != 0) {
						return false;
					}
				}
			}
		}
		mMatchFound.set( true );
		return true;
	}


	private String toJsonString( String pSnuttifiedJsonString ) {
		return pSnuttifiedJsonString.replace('\'','\"');
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append( mBdxName );
		if (jConditions != null) {
			for( JsonObject j : jConditions ) {
				sb.append("\n    " + j.toString());
			}
		}
		return sb.toString();
	}

	@Override
	public boolean getAsBoolean() {
		return false;
	}

	/*

	public static void main(String[] args) {
		BdxCondition bc = new BdxCondition("StatusBdx",
				"{'key':'attr1'}",
					"{'key':'attr2','value' : true}",
					"{'key':'attr3','value':'ABC'}",
					"{'key':'attr4','value': 143}");

		bc.match("{'StatusBdx' : {'attr1' : 42, 'attr2' : true, 'attr3' : 'ABC', 'attr4' :123 }}");
		System.out.println(bc);
	}
	*/


}
