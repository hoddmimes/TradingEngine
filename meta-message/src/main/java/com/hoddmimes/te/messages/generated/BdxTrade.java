
            package com.hoddmimes.te.messages.generated;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.io.IOException;





import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.jsontransform.JsonDecoder;
import com.hoddmimes.jsontransform.JsonEncoder;
import com.hoddmimes.jsontransform.ListFactory;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;



            
        // Add XML defined imports
        
            import com.hoddmimes.te.messages.*;

            @SuppressWarnings({"WeakerAccess","unused","unchecked"})
            public class BdxTrade implements MessageInterface , EngineBdxInterface
            {
                public static String NAME = "BdxTrade";

            
                    private String mSellOrderId;
                    private String mBuyOrderId;
                    private String mSymbol;
                    private Double mPrice;
                    private Integer mVolume;
                    private String mTradeId;
               public BdxTrade()
               {
                
               }

               public BdxTrade(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public BdxTrade setSellOrderId( String pSellOrderId ) {
            mSellOrderId = pSellOrderId;
            return this;
            }
            public Optional<String> getSellOrderId() {
              return  Optional.ofNullable(mSellOrderId);
            }
        
            public BdxTrade setBuyOrderId( String pBuyOrderId ) {
            mBuyOrderId = pBuyOrderId;
            return this;
            }
            public Optional<String> getBuyOrderId() {
              return  Optional.ofNullable(mBuyOrderId);
            }
        
            public BdxTrade setSymbol( String pSymbol ) {
            mSymbol = pSymbol;
            return this;
            }
            public Optional<String> getSymbol() {
              return  Optional.ofNullable(mSymbol);
            }
        
            public BdxTrade setPrice( Double pPrice ) {
            mPrice = pPrice;
            return this;
            }
            public Optional<Double> getPrice() {
              return  Optional.ofNullable(mPrice);
            }
        
            public BdxTrade setVolume( Integer pVolume ) {
            mVolume = pVolume;
            return this;
            }
            public Optional<Integer> getVolume() {
              return  Optional.ofNullable(mVolume);
            }
        
            public BdxTrade setTradeId( String pTradeId ) {
            mTradeId = pTradeId;
            return this;
            }
            public Optional<String> getTradeId() {
              return  Optional.ofNullable(mTradeId);
            }
        

        public String getMessageName() {
        return "BdxTrade";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("BdxTrade", tEncoder.toJson() );
            //Encode Attribute: mSellOrderId Type: String List: false
            tEncoder.add( "sellOrderId", mSellOrderId );
        
            //Encode Attribute: mBuyOrderId Type: String List: false
            tEncoder.add( "buyOrderId", mBuyOrderId );
        
            //Encode Attribute: mSymbol Type: String List: false
            tEncoder.add( "symbol", mSymbol );
        
            //Encode Attribute: mPrice Type: double List: false
            tEncoder.add( "price", mPrice );
        
            //Encode Attribute: mVolume Type: int List: false
            tEncoder.add( "volume", mVolume );
        
            //Encode Attribute: mTradeId Type: String List: false
            tEncoder.add( "tradeId", mTradeId );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("BdxTrade");
        
            //Decode Attribute: mSellOrderId Type:String List: false
            mSellOrderId = tDecoder.readString("sellOrderId");
        
            //Decode Attribute: mBuyOrderId Type:String List: false
            mBuyOrderId = tDecoder.readString("buyOrderId");
        
            //Decode Attribute: mSymbol Type:String List: false
            mSymbol = tDecoder.readString("symbol");
        
            //Decode Attribute: mPrice Type:double List: false
            mPrice = tDecoder.readDouble("price");
        
            //Decode Attribute: mVolume Type:int List: false
            mVolume = tDecoder.readInteger("volume");
        
            //Decode Attribute: mTradeId Type:String List: false
            mTradeId = tDecoder.readString("tradeId");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getBdxTradeBuilder() {
            return new BdxTrade.Builder();
        }


        public static class  Builder {
          private BdxTrade mInstance;

          private Builder () {
            mInstance = new BdxTrade();
          }

        
                        public Builder setSellOrderId( String pValue ) {
                        mInstance.setSellOrderId( pValue );
                        return this;
                    }
                
                        public Builder setBuyOrderId( String pValue ) {
                        mInstance.setBuyOrderId( pValue );
                        return this;
                    }
                
                        public Builder setSymbol( String pValue ) {
                        mInstance.setSymbol( pValue );
                        return this;
                    }
                
                        public Builder setPrice( Double pValue ) {
                        mInstance.setPrice( pValue );
                        return this;
                    }
                
                        public Builder setVolume( Integer pValue ) {
                        mInstance.setVolume( pValue );
                        return this;
                    }
                
                        public Builder setTradeId( String pValue ) {
                        mInstance.setTradeId( pValue );
                        return this;
                    }
                

        public BdxTrade build() {
            return mInstance;
        }

        }
    
            }
            
        /**
            Possible native attributes
            o "boolean" mapped to JSON "Boolean"
            o "byte" mapped to JSON "Integer"
            o "char" mapped to JSON "Integer"
            o "short" mapped to JSON "Integer"
            o "int" mapped to JSON "Integer"
            o "long" mapped to JSON "Integer"
            o "double" mapped to JSON "Numeric"
            o "String" mapped to JSON "String"
            o "byte[]" mapped to JSON "String" (Base64 string)


            An attribute could also be an "constant" i.e. having the property "constantGroup", should then refer to an defined /Constang/Group
             conastants are mapped to JSON strings,


            If the type is not any of the types below it will be refer to an other structure / object

        **/

    