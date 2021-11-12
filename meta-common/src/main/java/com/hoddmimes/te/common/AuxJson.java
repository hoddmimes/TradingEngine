package com.hoddmimes.te.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuxJson
{
	private static Pattern REF_PATTERN = Pattern.compile("\"ref\"\\s*:\\s*\"([^\"]+)\"");

	public static List<JsonElement> loadAndParseFile( String pFilename) throws IOException
	{
		List<JsonElement> tJsonElements = new ArrayList<>();

		FileReader tFileReader = new FileReader( pFilename );
		JsonReader jReader = new JsonReader( tFileReader );
		try {
			while (true) {
				JsonElement jElement = JsonParser.parseReader(jReader);
				tJsonElements.add(jElement);
			}
		}
		catch( IllegalArgumentException e) {
			// No more JSON objects in file
		}
		tFileReader.close();
		return tJsonElements;
	}

	public static boolean navigateBoolean( JsonObject pObject, String pPath ) {
		return navigateBoolean( pObject, pPath, null);
	}
	public static int navigateInt( JsonObject pObject, String pPath ) {
		return navigateInt( pObject, pPath, null);
	}
	public static double navigateDouble( JsonObject pObject, String pPath ) {
		return navigateDouble( pObject, pPath, null);
	}
	public static long navigateLong( JsonObject pObject, String pPath ) {
		return navigateLong( pObject, pPath, null);
	}
	public static String navigateString( JsonObject pObject, String pPath ) {
		return navigateString( pObject, pPath, null);
	}
	public static JsonObject navigateObject( JsonObject pObject, String pPath ) {
		return navigateObject( pObject, pPath, null);
	}

	public static int navigateInt( JsonObject pObject, String pPath, Integer pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsInt();
	}

	public static double navigateDouble( JsonObject pObject, String pPath, Double pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsDouble();
	}

	public static String navigateString( JsonObject pObject, String pPath, String pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsString();
	}

	public static boolean navigateBoolean( JsonObject pObject, String pPath, Boolean pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsBoolean();
	}

	public static JsonObject navigateObject( JsonObject pObject, String pPath, JsonObject pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsJsonObject();
	}

	public static long navigateLong( JsonObject pObject, String pPath, Long pDefaultValue ) {
		JsonElement tElement = navigate( pObject, pPath);
		if ((tElement == null) && (pDefaultValue == null)) {
			throw new RuntimeException("json path \"" +pPath+"\" not found");
		}
		return (tElement == null) ?  pDefaultValue : tElement.getAsLong();
	}


	private static JsonElement navigate(JsonObject pObject, String pPath ) {
		String pLevels[] = pPath.split("/");
		JsonElement tElement = pObject;
		for (int i = 0; i < pLevels.length; i++) {
			tElement = tElement.getAsJsonObject().get( pLevels[i]);
			if (tElement == null) {
				return null;
			}
		}
		return tElement;
	}
	public static String getMessageRef( String pJsonMsgStr) {
		return getMessageRef( pJsonMsgStr, null);
	}
	public static String getMessageRef( String pJsonMsgStr, String pDefault ) {
		if (pJsonMsgStr == null) {
			return null;
		}
		Matcher m = REF_PATTERN.matcher( pJsonMsgStr );
		return (m.find()) ? m.group(1) : pDefault;
	}

	public static JsonObject getMessageBody( JsonObject pMessage ) {
		Set<Map.Entry<String, JsonElement>> tEntrySet = pMessage.entrySet();
		if (tEntrySet.size() != 1) {
			throw new RuntimeException("Invalid Message structure, object must have one attribute on top level");
		}
		return pMessage.get( tEntrySet.iterator().next().getKey() ).getAsJsonObject();
	}

	public static String tagMessageBody( String pMessageTagName, String pJsonMessageBody ) {
		return "{ \"" + pMessageTagName + "\" : " + pJsonMessageBody + " }";
	}
}
