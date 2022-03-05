
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;


// Add XML defined imports


@SuppressWarnings({"WeakerAccess", "unused", "unchecked"})
public class DbCryptoHolding implements MessageInterface, MessageMongoInterface {
	public static String NAME = "DbCryptoHolding";
    private static final int TX_CACHE_SIZE = 50;

	private String mSid;
	private Long mQuantity;
	private Long mTeTradeSeqno;
	private LinkedHashMap<String, String> mProcessedNetworkTransactions;

	public DbCryptoHolding() {
	}

	public void set( long pQuantityNormalized ) {
		mQuantity = pQuantityNormalized;
		mTeTradeSeqno = 0L;
		mProcessedNetworkTransactions = new LinkedHashMap<>();
	}

    public void teUpdate( long pDeltaQuantityNormalized, long pTeTradeSeq  ) {
        mQuantity =  (mQuantity == null) ? pDeltaQuantityNormalized : (mQuantity + pDeltaQuantityNormalized);
        mTeTradeSeqno = pTeTradeSeq;
    }

	public void directUpdate( long pDeltaQuantityNormalized ) {
		mQuantity =  (mQuantity == null) ? pDeltaQuantityNormalized : (mQuantity + pDeltaQuantityNormalized);
	}

    public void networkUpdate( long pDeltaQuantityNormalized, String pTxid  ) {
        mQuantity = (mQuantity == null) ? pDeltaQuantityNormalized : (mQuantity + pDeltaQuantityNormalized);
        while (mProcessedNetworkTransactions.size() > TX_CACHE_SIZE) {
            mProcessedNetworkTransactions.remove(mProcessedNetworkTransactions.values().iterator().next());
        }
    }

    public boolean isProcessed( long pTeTradeSeqno ) {
        if (mTeTradeSeqno == null) {
            return false;
        }
        return (mTeTradeSeqno >= pTeTradeSeqno);
    }

    public boolean isProcessed( String  pTxid ) {
        if (mProcessedNetworkTransactions == null) {
            return false;
        }
        return mProcessedNetworkTransactions.containsKey( pTxid );
    }


	public DbCryptoHolding(String pJsonString) {
		JsonDecoder tDecoder = new JsonDecoder(pJsonString);
		this.decode(tDecoder);
	}

	public DbCryptoHolding setSid(String pSid) {
		mSid = pSid;
		return this;
	}

	public Optional<String> getSid() {
		return Optional.ofNullable(mSid);
	}

	public DbCryptoHolding setQuantity(Long pQuantity) {
		mQuantity = pQuantity;
		return this;
	}

	public Optional<Long> getQuantity() {
		return Optional.ofNullable(mQuantity);
	}

	public DbCryptoHolding setTeTradeSeqno(Long pTeTradeSeqno) {
		mTeTradeSeqno = pTeTradeSeqno;
		return this;
	}

	public Optional<Long> getTeTradeSeqno() {
		return Optional.ofNullable(mTeTradeSeqno);
	}

	public DbCryptoHolding setProcessedNetworkTransactions(List<String> pProcessedNetworkTransactions) {
		if (pProcessedNetworkTransactions == null) {
			mProcessedNetworkTransactions = null;
		} else {
			mProcessedNetworkTransactions = new LinkedHashMap<>();
            for( String txid : pProcessedNetworkTransactions ) {
                mProcessedNetworkTransactions.put(txid, txid );
            }
		}
		return this;
	}

	public Optional<LinkedHashMap<String,String>> getProcessedNetworkTransactions() {
		return Optional.ofNullable(mProcessedNetworkTransactions);
	}


	public String getMessageName() {
		return "DbCryptoHolding";
	}


	public JsonObject toJson() {
		JsonEncoder tEncoder = new JsonEncoder();
		this.encode(tEncoder);
		return tEncoder.toJson();
	}


	public void encode(JsonEncoder pEncoder) {
		JsonEncoder tEncoder = pEncoder;
		//Encode Attribute: mSid Type: String List: false
		tEncoder.add("sid", mSid);

		//Encode Attribute: mQuantity Type: long List: false
		tEncoder.add("quantity", mQuantity);

		//Encode Attribute: mTeTradeSeqno Type: long List: false
		tEncoder.add("teTradeSeqno", mTeTradeSeqno);

		//Encode Attribute: mProcessedNetworkTransactions Type: String List: true
        List<String> tTxids = (mProcessedNetworkTransactions == null) ? null : new ArrayList<>();
        if (mProcessedNetworkTransactions != null) {
            mProcessedNetworkTransactions.values().stream().forEach( tx -> tTxids.add( tx ));
        }
		tEncoder.addStringArray("processedNetworkTransactions", tTxids);

	}


	public void decode(JsonDecoder pDecoder) {


		JsonDecoder tDecoder = pDecoder;

		//Decode Attribute: mSid Type:String List: false
		mSid = tDecoder.readString("sid");

		//Decode Attribute: mQuantity Type:long List: false
		mQuantity = tDecoder.readLong("quantity");

		//Decode Attribute: mTeTradeSeqno Type:long List: false
		mTeTradeSeqno = tDecoder.readLong("teTradeSeqno");

		//Decode Attribute: mProcessedNetworkTransactions Type:String List: true
        List<String> tTxids = tDecoder.readStringArray("processedNetworkTransactions", "linked");
		if (tTxids == null) {
            mProcessedNetworkTransactions = null;
        } else {
            mProcessedNetworkTransactions = new LinkedHashMap<>();
            tTxids.stream().forEach( tx -> mProcessedNetworkTransactions.put( tx,tx ));
        }


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

		pEncoder.add("sid", mSid);
		pEncoder.add("quantity", mQuantity);
		pEncoder.add("teTradeSeqno", mTeTradeSeqno);

        List<String> tTxids = (mProcessedNetworkTransactions == null) ? null : new ArrayList<>();
        if (mProcessedNetworkTransactions != null) {
            mProcessedNetworkTransactions.values().stream().forEach( tx -> tTxids.add( tx ));
        }
        pEncoder.addStringArray("processedNetworkTransactions", tTxids);
	}

	public void decodeMongoDocument(Document pDoc) {

		Document tDoc = null;
		List<Document> tDocLst = null;
		MongoDecoder tDecoder = new MongoDecoder(pDoc);


		mSid = tDecoder.readString("sid");

		mQuantity = tDecoder.readLong("quantity");

		mTeTradeSeqno = tDecoder.readLong("teTradeSeqno");

        List<String> tTxids = tDecoder.readStringArray("processedNetworkTransactions", "linked");
        if (tTxids == null) {
            mProcessedNetworkTransactions = null;
        } else {
            mProcessedNetworkTransactions = new LinkedHashMap<>();
            tTxids.stream().forEach( tx -> mProcessedNetworkTransactions.put( tx,tx ));
        }
	} // End decodeMongoDocument


	public static Builder getDbCryptoHoldingBuilder() {
		return new DbCryptoHolding.Builder();
	}


	public static class Builder {
		private DbCryptoHolding mInstance;

		private Builder() {
			mInstance = new DbCryptoHolding();
		}


		public Builder setSid(String pValue) {
			mInstance.setSid(pValue);
			return this;
		}

		public Builder setQuantity(Long pValue) {
			mInstance.setQuantity(pValue);
			return this;
		}

		public Builder setTeTradeSeqno(Long pValue) {
			mInstance.setTeTradeSeqno(pValue);
			return this;
		}

		public Builder setProcessedNetworkTransactions(List<String> pValue) {
			mInstance.setProcessedNetworkTransactions(pValue);
			return this;
		}


		public DbCryptoHolding build() {
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

    