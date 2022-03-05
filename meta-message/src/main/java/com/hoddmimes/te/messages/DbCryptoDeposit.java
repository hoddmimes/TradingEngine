
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
package com.hoddmimes.te.messages;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.*;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// Add XML defined imports


@SuppressWarnings({"WeakerAccess", "unused", "unchecked"})
public class DbCryptoDeposit implements MessageInterface, MessageMongoInterface {
	public static String NAME = "DbCryptoDeposit";


	private String mMongoId = null;
	private String mAccountId;
	private List<DbCryptoHolding> mHoldings;

	public DbCryptoDeposit() {
        mHoldings = new ArrayList<>();
	}

    private DbCryptoHolding getHoldingEntry( String pSid ) {
      for( DbCryptoHolding tHolding : mHoldings) {
          if (tHolding.getSid().get().contentEquals(pSid)) {
              return tHolding;
          }
      }
        DbCryptoHolding tHolding = new DbCryptoHolding().setSid(pSid).setQuantity(0L).setTeTradeSeqno(0L).setProcessedNetworkTransactions( new ArrayList<>());
        addHoldings( tHolding );
        return tHolding;
    }

	public void set( String pSid, long pDeltaQuantityNormalized ) {
		DbCryptoHolding tHolding = getHoldingEntry(pSid);
		tHolding.set( pDeltaQuantityNormalized );
	}

    public boolean teUpdate( String pSid, long pDeltaQuantityNormalized, long pTeTradeSeq  ) {
        DbCryptoHolding tHolding = getHoldingEntry(pSid);
        if (tHolding.getTeTradeSeqno().get() < pTeTradeSeq) {
            tHolding.teUpdate(pDeltaQuantityNormalized, pTeTradeSeq);
            return true;
        }
        return false;
    }


    public boolean networkUpdate( String pSid, long pDeltaQuantityNormalized, String pTxid  ) {
        DbCryptoHolding tHolding = getHoldingEntry(pSid);
        if (!tHolding.isProcessed(pTxid)) {
            tHolding.networkUpdate( pDeltaQuantityNormalized, pTxid );
            return true;
        }
        return false;
    }


	public void directUpdate( String pSid, long pDeltaQuantityNormalized ) {
		DbCryptoHolding tHolding = getHoldingEntry(pSid);
		tHolding.directUpdate(pDeltaQuantityNormalized);
	}


    public long getHolding( String pSid ) {
        DbCryptoHolding tHolding = getHoldingEntry(pSid);
        return tHolding.getQuantity().get();
    }


	public DbCryptoDeposit(String pJsonString) {

		JsonDecoder tDecoder = new JsonDecoder(pJsonString);
		this.decode(tDecoder);
	}

	public String getMongoId() {
		return this.mMongoId;
	}

	public void setMongoId(String pMongoId) {
		this.mMongoId = pMongoId;
	}

	public DbCryptoDeposit setAccountId(String pAccountId) {
		mAccountId = pAccountId;
		return this;
	}

	public Optional<String> getAccountId() {
		return Optional.ofNullable(mAccountId);
	}

	public DbCryptoDeposit setHoldings(List<DbCryptoHolding> pHoldings) {
		if (pHoldings == null) {
			mHoldings = null;
			return this;
		}


		if (mHoldings == null)
			mHoldings = ListFactory.getList("array");


		mHoldings.addAll(pHoldings);
		return this;
	}


	public DbCryptoDeposit addHoldings(List<DbCryptoHolding> pHoldings) {

		if (mHoldings == null)
			mHoldings = ListFactory.getList("array");

		mHoldings.addAll(pHoldings);
		return this;
	}

	public DbCryptoDeposit addHoldings(DbCryptoHolding pHoldings) {

		if (pHoldings == null) {
			return this; // Not supporting null in vectors, well design issue
		}

		if (mHoldings == null) {
			mHoldings = ListFactory.getList("array");
		}

		mHoldings.add(pHoldings);
		return this;
	}


	public Optional<List<DbCryptoHolding>> getHoldings() {

		if (mHoldings == null) {
			return Optional.ofNullable(null);
		}

		//List<DbCryptoHolding> tList = ListFactory.getList("array");
		//tList.addAll( mHoldings );
		// return  Optional.ofNullable(tList);
		return Optional.ofNullable(mHoldings);
	}


	public String getMessageName() {
		return "DbCryptoDeposit";
	}


	public JsonObject toJson() {
		JsonEncoder tEncoder = new JsonEncoder();
		this.encode(tEncoder);
		return tEncoder.toJson();
	}


	public void encode(JsonEncoder pEncoder) {


		JsonEncoder tEncoder = pEncoder;
		//Encode Attribute: mAccountId Type: String List: false
		tEncoder.add("accountId", mAccountId);

		//Encode Attribute: mHoldings Type: DbCryptoHolding List: true
		tEncoder.addMessageArray("holdings", mHoldings);

	}


	public void decode(JsonDecoder pDecoder) {


		JsonDecoder tDecoder = pDecoder;

		//Decode Attribute: mAccountId Type:String List: false
		mAccountId = tDecoder.readString("accountId");

		//Decode Attribute: mHoldings Type:DbCryptoHolding List: true
		mHoldings = (List<DbCryptoHolding>) tDecoder.readMessageArray("holdings", "array", DbCryptoHolding.class);


	}


	@Override
	public String toString() {
		Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
		return gsonPrinter.toJson(this.toJson());
	}

	public Document getMongoDocument() {
		MongoEncoder tEncoder = new MongoEncoder();

		mongoEncode(tEncoder);
		return tEncoder.getDoc();
	}

	protected void mongoEncode(MongoEncoder pEncoder) {

		pEncoder.add("accountId", mAccountId);
		pEncoder.addMessageArray("holdings", mHoldings);
	}

	public void decodeMongoDocument(Document pDoc) {

		Document tDoc = null;
		List<Document> tDocLst = null;
		MongoDecoder tDecoder = new MongoDecoder(pDoc);


		ObjectId _tId = pDoc.get("_id", ObjectId.class);
		this.mMongoId = _tId.toString();

		mAccountId = tDecoder.readString("accountId");

		tDocLst = (List<Document>) tDecoder.readMessageArray("holdings", "array");
		if (tDocLst == null) {
			mHoldings = null;
		} else {
			mHoldings = ListFactory.getList("array");
			for (Document doc : tDocLst) {
				DbCryptoHolding m = new DbCryptoHolding();
				m.decodeMongoDocument(doc);
				mHoldings.add(m);
			}
		}
	} // End decodeMongoDocument


	public static Builder getDbCryptoDepositBuilder() {
		return new DbCryptoDeposit.Builder();
	}


	public static class Builder {
		private DbCryptoDeposit mInstance;

		private Builder() {
			mInstance = new DbCryptoDeposit();
		}


		public Builder setAccountId(String pValue) {
			mInstance.setAccountId(pValue);
			return this;
		}

		public Builder setHoldings(List<DbCryptoHolding> pValue) {
			mInstance.setHoldings(pValue);
			return this;
		}


		public DbCryptoDeposit build() {
			return mInstance;
		}

	}

}

/**
 * Possible native attributes
 * o "boolean" mapped to JSON "Boolean"
 * o "byte" mapped to JSON "Integer"
 * o "char" mapped to JSON "Integer"
 * o "short" mapped to JSON "Integer"
 * o "int" mapped to JSON "Integer"
 * o "long" mapped to JSON "Integer"
 * o "double" mapped to JSON "Numeric"
 * o "String" mapped to JSON "String"
 * o "byte[]" mapped to JSON "String" (Base64 string)
 * <p>
 * <p>
 * An attribute could also be an "constant" i.e. having the property "constantGroup", should then refer to an defined /Constang/Group
 * conastants are mapped to JSON strings,
 * <p>
 * <p>
 * If the type is not any of the types below it will be refer to an other structure / object
 **/

    