package com.hoddmimes.te.common;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TupleToJson
{
	public static JsonObject toJson( Map<String,String> pMap) {
		if (pMap == null) {
			return null;
		}
		JsonObject jObject = new JsonObject();
		for(Map.Entry<String,String> tuple : pMap.entrySet()) {
			addJsonAttribute(jObject, tuple.getKey(), tuple.getValue());
		}
		return jObject;
	}

	public static JsonObject toJson( String pValueNameList) {
		if (pValueNameList == null) {
			return null;
		}
		String tStrArr[] = pValueNameList.split(",");

		JsonObject jObject = new JsonObject();
		for (int i = 0; i < tStrArr.length; i++) {
			addJsonAttribute(jObject, tStrArr[i]);
		}
		return jObject;
	}


	private static void addJsonAttribute(JsonObject pJsonObject, String pName, String pValue )
	{
		if (isInteger(pValue.trim())) {
			pJsonObject.addProperty(pValue.trim(), Integer.parseInt(pValue.trim()));
		} else if (isDouble(pValue.trim())) {
			pJsonObject.addProperty(pValue.trim(), Double.parseDouble(pValue.trim()));
		} else if (isBoolean(pValue.trim())) {
			pJsonObject.addProperty(pValue.trim(), Boolean.parseBoolean(pValue.trim()));
		} else {
			pJsonObject.addProperty(pValue.trim(), pValue.trim());
		}
	}

	private static void addJsonAttribute(JsonObject pJsonObject, String pNameValue) {
		String tNameValue[] = pNameValue.split(":");
		if (tNameValue.length != 2) {
			new RuntimeException("Invalid name/value pair (" + pNameValue + ")");
		}
		addJsonAttribute( pJsonObject, tNameValue[0], tNameValue[1]);

	}

	private static boolean isDouble(String pValue) {
		try {
			Double.parseDouble(pValue);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isInteger(String pValue) {
		try {
			Integer.parseInt(pValue);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isBoolean(String pValue) {
		try {
			if ((pValue.toLowerCase().contentEquals("true")) || (pValue.toLowerCase().contentEquals("false"))) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}
}
