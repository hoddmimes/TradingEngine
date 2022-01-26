
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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;


public class Test {


	public static void main(String[] args) {
		Test t = new Test();
		t.test();
	}




	private void test() {
		String jText = "{\"tickSize\" : 0.01}";
		JsonObject jObj = JsonParser.parseString( jText ).getAsJsonObject();
		long tTickSize = (long) (jObj.get("tickSize").getAsDouble() * 10000.0);
		System.out.println("new: " + tTickSize);
		jObj.addProperty("tickSize", tTickSize);
		System.out.println("object: " + jObj.toString());
	}





}


