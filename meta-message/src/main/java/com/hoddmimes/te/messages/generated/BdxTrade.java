
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

            
                    private String mSid;
                    private Double mLast;
                    private Integer mQuantity;
                    private Double mOpen;
                    private Double mLow;
                    private Double mHigh;
                    private Integer mTotQuantity;
               public BdxTrade()
               {
                
               }

               public BdxTrade(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public BdxTrade setSid( String pSid ) {
            mSid = pSid;
            return this;
            }
            public Optional<String> getSid() {
              return  Optional.ofNullable(mSid);
            }
        
            public BdxTrade setLast( Double pLast ) {
            mLast = pLast;
            return this;
            }
            public Optional<Double> getLast() {
              return  Optional.ofNullable(mLast);
            }
        
            public BdxTrade setQuantity( Integer pQuantity ) {
            mQuantity = pQuantity;
            return this;
            }
            public Optional<Integer> getQuantity() {
              return  Optional.ofNullable(mQuantity);
            }
        
            public BdxTrade setOpen( Double pOpen ) {
            mOpen = pOpen;
            return this;
            }
            public Optional<Double> getOpen() {
              return  Optional.ofNullable(mOpen);
            }
        
            public BdxTrade setLow( Double pLow ) {
            mLow = pLow;
            return this;
            }
            public Optional<Double> getLow() {
              return  Optional.ofNullable(mLow);
            }
        
            public BdxTrade setHigh( Double pHigh ) {
            mHigh = pHigh;
            return this;
            }
            public Optional<Double> getHigh() {
              return  Optional.ofNullable(mHigh);
            }
        
            public BdxTrade setTotQuantity( Integer pTotQuantity ) {
            mTotQuantity = pTotQuantity;
            return this;
            }
            public Optional<Integer> getTotQuantity() {
              return  Optional.ofNullable(mTotQuantity);
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
            //Encode Attribute: mSid Type: String List: false
            tEncoder.add( "sid", mSid );
        
            //Encode Attribute: mLast Type: double List: false
            tEncoder.add( "last", mLast );
        
            //Encode Attribute: mQuantity Type: int List: false
            tEncoder.add( "quantity", mQuantity );
        
            //Encode Attribute: mOpen Type: double List: false
            tEncoder.add( "open", mOpen );
        
            //Encode Attribute: mLow Type: double List: false
            tEncoder.add( "low", mLow );
        
            //Encode Attribute: mHigh Type: double List: false
            tEncoder.add( "high", mHigh );
        
            //Encode Attribute: mTotQuantity Type: int List: false
            tEncoder.add( "totQuantity", mTotQuantity );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("BdxTrade");
        
            //Decode Attribute: mSid Type:String List: false
            mSid = tDecoder.readString("sid");
        
            //Decode Attribute: mLast Type:double List: false
            mLast = tDecoder.readDouble("last");
        
            //Decode Attribute: mQuantity Type:int List: false
            mQuantity = tDecoder.readInteger("quantity");
        
            //Decode Attribute: mOpen Type:double List: false
            mOpen = tDecoder.readDouble("open");
        
            //Decode Attribute: mLow Type:double List: false
            mLow = tDecoder.readDouble("low");
        
            //Decode Attribute: mHigh Type:double List: false
            mHigh = tDecoder.readDouble("high");
        
            //Decode Attribute: mTotQuantity Type:int List: false
            mTotQuantity = tDecoder.readInteger("totQuantity");
        

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
        

        public static  Builder getBdxTradeBuilder() {
            return new BdxTrade.Builder();
        }


        public static class  Builder {
          private BdxTrade mInstance;

          private Builder () {
            mInstance = new BdxTrade();
          }

        
                        public Builder setSid( String pValue ) {
                        mInstance.setSid( pValue );
                        return this;
                    }
                
                        public Builder setLast( Double pValue ) {
                        mInstance.setLast( pValue );
                        return this;
                    }
                
                        public Builder setQuantity( Integer pValue ) {
                        mInstance.setQuantity( pValue );
                        return this;
                    }
                
                        public Builder setOpen( Double pValue ) {
                        mInstance.setOpen( pValue );
                        return this;
                    }
                
                        public Builder setLow( Double pValue ) {
                        mInstance.setLow( pValue );
                        return this;
                    }
                
                        public Builder setHigh( Double pValue ) {
                        mInstance.setHigh( pValue );
                        return this;
                    }
                
                        public Builder setTotQuantity( Integer pValue ) {
                        mInstance.setTotQuantity( pValue );
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

    