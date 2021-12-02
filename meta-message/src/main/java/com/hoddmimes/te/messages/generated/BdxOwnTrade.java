
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
            public class BdxOwnTrade implements MessageInterface , EngineBdxInterface
            {
                public static String NAME = "BdxOwnTrade";

            
                    private String mOrderId;
                    private String mSid;
                    private Double mPrice;
                    private Integer mQuantity;
                    private String mTradeId;
                    private String mTime;
                    private String mSide;
                    private String mOrderRef;
               public BdxOwnTrade()
               {
                
               }

               public BdxOwnTrade(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public BdxOwnTrade setOrderId( String pOrderId ) {
            mOrderId = pOrderId;
            return this;
            }
            public Optional<String> getOrderId() {
              return  Optional.ofNullable(mOrderId);
            }
        
            public BdxOwnTrade setSid( String pSid ) {
            mSid = pSid;
            return this;
            }
            public Optional<String> getSid() {
              return  Optional.ofNullable(mSid);
            }
        
            public BdxOwnTrade setPrice( Double pPrice ) {
            mPrice = pPrice;
            return this;
            }
            public Optional<Double> getPrice() {
              return  Optional.ofNullable(mPrice);
            }
        
            public BdxOwnTrade setQuantity( Integer pQuantity ) {
            mQuantity = pQuantity;
            return this;
            }
            public Optional<Integer> getQuantity() {
              return  Optional.ofNullable(mQuantity);
            }
        
            public BdxOwnTrade setTradeId( String pTradeId ) {
            mTradeId = pTradeId;
            return this;
            }
            public Optional<String> getTradeId() {
              return  Optional.ofNullable(mTradeId);
            }
        
            public BdxOwnTrade setTime( String pTime ) {
            mTime = pTime;
            return this;
            }
            public Optional<String> getTime() {
              return  Optional.ofNullable(mTime);
            }
        
            public BdxOwnTrade setSide( String pSide ) {
            mSide = pSide;
            return this;
            }
            public Optional<String> getSide() {
              return  Optional.ofNullable(mSide);
            }
        
            public BdxOwnTrade setOrderRef( String pOrderRef ) {
            mOrderRef = pOrderRef;
            return this;
            }
            public Optional<String> getOrderRef() {
              return  Optional.ofNullable(mOrderRef);
            }
        

        public String getMessageName() {
        return "BdxOwnTrade";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("BdxOwnTrade", tEncoder.toJson() );
            //Encode Attribute: mOrderId Type: String List: false
            tEncoder.add( "orderId", mOrderId );
        
            //Encode Attribute: mSid Type: String List: false
            tEncoder.add( "sid", mSid );
        
            //Encode Attribute: mPrice Type: double List: false
            tEncoder.add( "price", mPrice );
        
            //Encode Attribute: mQuantity Type: int List: false
            tEncoder.add( "quantity", mQuantity );
        
            //Encode Attribute: mTradeId Type: String List: false
            tEncoder.add( "tradeId", mTradeId );
        
            //Encode Attribute: mTime Type: String List: false
            tEncoder.add( "time", mTime );
        
            //Encode Attribute: mSide Type: String List: false
            tEncoder.add( "side", mSide );
        
            //Encode Attribute: mOrderRef Type: String List: false
            tEncoder.add( "orderRef", mOrderRef );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("BdxOwnTrade");
        
            //Decode Attribute: mOrderId Type:String List: false
            mOrderId = tDecoder.readString("orderId");
        
            //Decode Attribute: mSid Type:String List: false
            mSid = tDecoder.readString("sid");
        
            //Decode Attribute: mPrice Type:double List: false
            mPrice = tDecoder.readDouble("price");
        
            //Decode Attribute: mQuantity Type:int List: false
            mQuantity = tDecoder.readInteger("quantity");
        
            //Decode Attribute: mTradeId Type:String List: false
            mTradeId = tDecoder.readString("tradeId");
        
            //Decode Attribute: mTime Type:String List: false
            mTime = tDecoder.readString("time");
        
            //Decode Attribute: mSide Type:String List: false
            mSide = tDecoder.readString("side");
        
            //Decode Attribute: mOrderRef Type:String List: false
            mOrderRef = tDecoder.readString("orderRef");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    
            public String getSubjectName() {
            SID tSID = new SID( this.getSid().get());
            return "/" + this.getMessageName() + "/" + String.valueOf(tSID.getMarket()) + "/" + tSID.getSymbol();
            }
        

        public static  Builder getBdxOwnTradeBuilder() {
            return new BdxOwnTrade.Builder();
        }


        public static class  Builder {
          private BdxOwnTrade mInstance;

          private Builder () {
            mInstance = new BdxOwnTrade();
          }

        
                        public Builder setOrderId( String pValue ) {
                        mInstance.setOrderId( pValue );
                        return this;
                    }
                
                        public Builder setSid( String pValue ) {
                        mInstance.setSid( pValue );
                        return this;
                    }
                
                        public Builder setPrice( Double pValue ) {
                        mInstance.setPrice( pValue );
                        return this;
                    }
                
                        public Builder setQuantity( Integer pValue ) {
                        mInstance.setQuantity( pValue );
                        return this;
                    }
                
                        public Builder setTradeId( String pValue ) {
                        mInstance.setTradeId( pValue );
                        return this;
                    }
                
                        public Builder setTime( String pValue ) {
                        mInstance.setTime( pValue );
                        return this;
                    }
                
                        public Builder setSide( String pValue ) {
                        mInstance.setSide( pValue );
                        return this;
                    }
                
                        public Builder setOrderRef( String pValue ) {
                        mInstance.setOrderRef( pValue );
                        return this;
                    }
                

        public BdxOwnTrade build() {
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

    