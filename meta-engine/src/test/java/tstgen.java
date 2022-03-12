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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

public class tstgen
{
	NumberFormat nfmt;
	int mRefIdx = 1;
	Random mRandom;

	public  tstgen() {
		mRandom = new Random();
	}

	String getnxtref() {
		return String.format("%04d", mRefIdx++);
	}

	public static void main(String[] args) {
		tstgen t = new tstgen();
		t.gen("1:AMZN",10, 20);
	}

	private void gen(String pSymbol, int pMatches, int pOrderCount )
	{
		genMatches( pSymbol, pMatches);
		genOrders( pSymbol, pOrderCount);
	}



	String order( String pSymbol, String pSide, long pPrice, int pQuantity ) {
		return  "{ \"method\" : \"POST\", \"endpoint\" : \"addOrder\", \"body\" : {\"sid\":\"" + pSymbol + "\",\"price\":"  + String.valueOf( pPrice ) +
				",\"quantity\": " + pQuantity + ",\"side\":\"" + pSide + "\",\"ref\" : \"" + getnxtref() +"\"}},";
	}

	private void genOrders( String pSymbol, int pOrderCount )
	{
		long tPrice = 1000000;

		// Buy Orders
		for (int i = 0; i < pOrderCount; i++)
		{
			int qty = (mRandom.nextInt(20) + 80);
			long price = (((mRandom.nextInt(90) * 10) + 100) * -1) + 1000000L;
			System.out.println( order( pSymbol, "BUY", price, qty ));
		}
		for (int i = 0; i < pOrderCount; i++)
		{
			int qty = (mRandom.nextInt(20) + 80);
			long price = (((mRandom.nextInt(90) * 10) + 100) * 1) + 1000000L;
			System.out.println( order( pSymbol, "SELL", price, qty ));
		}
	}

	private void genMatches( String pSymbol, int pMatches )
	{
		long tPrice = 1000000;
		for (int i = 0; i < pMatches; i++)
		{
			int qty = (mRandom.nextInt(20) + 80);
			System.out.println( order( pSymbol, "BUY", tPrice, qty ));
			System.out.println( order( pSymbol, "SELL", tPrice, qty ));
			tPrice -= 250;
		}
	}
}
