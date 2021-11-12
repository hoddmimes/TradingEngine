
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
            public class BdxOwnOrderbookChange implements MessageInterface , EngineBdxInterface
            {
                public static String NAME = "BdxOwnOrderbookChange";

            
                    private String mOrderId;
                    private String mSymbol;
                    private String mAction;
                    private Double mPrice;
                    private Integer mVolume;
                    private String mSide;
                    private String mRef;
                    private Long mObSeqNo;
               public BdxOwnOrderbookChange()
               {
                
               }

               public BdxOwnOrderbookChange(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public BdxOwnOrderbookChange setOrderId( String pOrderId ) {
            mOrderId = pOrderId;
            return this;
            }
            public Optional<String> getOrderId() {
              return  Optional.ofNullable(mOrderId);
            }
        
            public BdxOwnOrderbookChange setSymbol( String pSymbol ) {
            mSymbol = pSymbol;
            return this;
            }
            public Optional<String> getSymbol() {
              return  Optional.ofNullable(mSymbol);
            }
        
            public BdxOwnOrderbookChange setAction( String pAction ) {
            mAction = pAction;
            return this;
            }
            public Optional<String> getAction() {
              return  Optional.ofNullable(mAction);
            }
        
            public BdxOwnOrderbookChange setPrice( Double pPrice ) {
            mPrice = pPrice;
            return this;
            }
            public Optional<Double> getPrice() {
              return  Optional.ofNullable(mPrice);
            }
        
            public BdxOwnOrderbookChange setVolume( Integer pVolume ) {
            mVolume = pVolume;
            return this;
            }
            public Optional<Integer> getVolume() {
              return  Optional.ofNullable(mVolume);
            }
        
            public BdxOwnOrderbookChange setSide( String pSide ) {
            mSide = pSide;
            return this;
            }
            public Optional<String> getSide() {
              return  Optional.ofNullable(mSide);
            }
        
            public BdxOwnOrderbookChange setRef( String pRef ) {
            mRef = pRef;
            return this;
            }
            public Optional<String> getRef() {
              return  Optional.ofNullable(mRef);
            }
        
            public BdxOwnOrderbookChange setObSeqNo( Long pObSeqNo ) {
            mObSeqNo = pObSeqNo;
            return this;
            }
            public Optional<Long> getObSeqNo() {
              return  Optional.ofNullable(mObSeqNo);
            }
        

        public String getMessageName() {
        return "BdxOwnOrderbookChange";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("BdxOwnOrderbookChange", tEncoder.toJson() );
            //Encode Attribute: mOrderId Type: String List: false
            tEncoder.add( "orderId", mOrderId );
        
            //Encode Attribute: mSymbol Type: String List: false
            tEncoder.add( "symbol", mSymbol );
        
            //Encode Attribute: mAction Type: String List: false
            tEncoder.add( "action", mAction );
        
            //Encode Attribute: mPrice Type: double List: false
            tEncoder.add( "price", mPrice );
        
            //Encode Attribute: mVolume Type: int List: false
            tEncoder.add( "volume", mVolume );
        
            //Encode Attribute: mSide Type: String List: false
            tEncoder.add( "side", mSide );
        
            //Encode Attribute: mRef Type: String List: false
            tEncoder.add( "ref", mRef );
        
            //Encode Attribute: mObSeqNo Type: long List: false
            tEncoder.add( "obSeqNo", mObSeqNo );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("BdxOwnOrderbookChange");
        
            //Decode Attribute: mOrderId Type:String List: false
            mOrderId = tDecoder.readString("orderId");
        
            //Decode Attribute: mSymbol Type:String List: false
            mSymbol = tDecoder.readString("symbol");
        
            //Decode Attribute: mAction Type:String List: false
            mAction = tDecoder.readString("action");
        
            //Decode Attribute: mPrice Type:double List: false
            mPrice = tDecoder.readDouble("price");
        
            //Decode Attribute: mVolume Type:int List: false
            mVolume = tDecoder.readInteger("volume");
        
            //Decode Attribute: mSide Type:String List: false
            mSide = tDecoder.readString("side");
        
            //Decode Attribute: mRef Type:String List: false
            mRef = tDecoder.readString("ref");
        
            //Decode Attribute: mObSeqNo Type:long List: false
            mObSeqNo = tDecoder.readLong("obSeqNo");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getBdxOwnOrderbookChangeBuilder() {
            return new BdxOwnOrderbookChange.Builder();
        }


        public static class  Builder {
          private BdxOwnOrderbookChange mInstance;

          private Builder () {
            mInstance = new BdxOwnOrderbookChange();
          }

        
                        public Builder setOrderId( String pValue ) {
                        mInstance.setOrderId( pValue );
                        return this;
                    }
                
                        public Builder setSymbol( String pValue ) {
                        mInstance.setSymbol( pValue );
                        return this;
                    }
                
                        public Builder setAction( String pValue ) {
                        mInstance.setAction( pValue );
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
                
                        public Builder setSide( String pValue ) {
                        mInstance.setSide( pValue );
                        return this;
                    }
                
                        public Builder setRef( String pValue ) {
                        mInstance.setRef( pValue );
                        return this;
                    }
                
                        public Builder setObSeqNo( Long pValue ) {
                        mInstance.setObSeqNo( pValue );
                        return this;
                    }
                

        public BdxOwnOrderbookChange build() {
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

    