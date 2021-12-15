
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
        // Add XML defined imports
        
            import java.util.Iterator;

            @SuppressWarnings({"WeakerAccess","unused","unchecked"})
            public class BdxPriceLevel implements MessageInterface , EngineBdxInterface
            {
                public static String NAME = "BdxPriceLevel";

            
                    private String mSid;
                    private Integer mLevels;
                    private List<PriceLevel> mBuySide;
                    private List<PriceLevel> mSellSide;
               public BdxPriceLevel()
               {
                
               }

               public BdxPriceLevel(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public BdxPriceLevel setSid( String pSid ) {
            mSid = pSid;
            return this;
            }
            public Optional<String> getSid() {
              return  Optional.ofNullable(mSid);
            }
        
            public BdxPriceLevel setLevels( Integer pLevels ) {
            mLevels = pLevels;
            return this;
            }
            public Optional<Integer> getLevels() {
              return  Optional.ofNullable(mLevels);
            }
        
            public BdxPriceLevel setBuySide( List<PriceLevel> pBuySide ) {
              if (pBuySide == null) {
                mBuySide = null;
                return this;
              }


            if ( mBuySide == null)
            mBuySide = ListFactory.getList("array");


            mBuySide .addAll( pBuySide );
            return this;
            }


            public BdxPriceLevel addBuySide( List<PriceLevel> pBuySide ) {

            if ( mBuySide == null)
            mBuySide = ListFactory.getList("array");

            mBuySide .addAll( pBuySide );
            return this;
            }

            public BdxPriceLevel addBuySide( PriceLevel pBuySide ) {

            if ( pBuySide == null) {
            return this; // Not supporting null in vectors, well design issue
            }

            if ( mBuySide == null) {
            mBuySide = ListFactory.getList("array");
            }

            mBuySide.add( pBuySide );
            return this;
            }


            public Optional<List<PriceLevel>> getBuySide() {

            if (mBuySide == null) {
                return  Optional.ofNullable(null);
            }

             //List<PriceLevel> tList = ListFactory.getList("array");
             //tList.addAll( mBuySide );
             // return  Optional.ofNullable(tList);
             return Optional.ofNullable(mBuySide);
            }

        
            public BdxPriceLevel setSellSide( List<PriceLevel> pSellSide ) {
              if (pSellSide == null) {
                mSellSide = null;
                return this;
              }


            if ( mSellSide == null)
            mSellSide = ListFactory.getList("array");


            mSellSide .addAll( pSellSide );
            return this;
            }


            public BdxPriceLevel addSellSide( List<PriceLevel> pSellSide ) {

            if ( mSellSide == null)
            mSellSide = ListFactory.getList("array");

            mSellSide .addAll( pSellSide );
            return this;
            }

            public BdxPriceLevel addSellSide( PriceLevel pSellSide ) {

            if ( pSellSide == null) {
            return this; // Not supporting null in vectors, well design issue
            }

            if ( mSellSide == null) {
            mSellSide = ListFactory.getList("array");
            }

            mSellSide.add( pSellSide );
            return this;
            }


            public Optional<List<PriceLevel>> getSellSide() {

            if (mSellSide == null) {
                return  Optional.ofNullable(null);
            }

             //List<PriceLevel> tList = ListFactory.getList("array");
             //tList.addAll( mSellSide );
             // return  Optional.ofNullable(tList);
             return Optional.ofNullable(mSellSide);
            }

        

        public String getMessageName() {
        return "BdxPriceLevel";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("BdxPriceLevel", tEncoder.toJson() );
            //Encode Attribute: mSid Type: String List: false
            tEncoder.add( "sid", mSid );
        
            //Encode Attribute: mLevels Type: int List: false
            tEncoder.add( "levels", mLevels );
        
            //Encode Attribute: mBuySide Type: PriceLevel List: true
            tEncoder.addMessageArray("buySide", mBuySide );
        
            //Encode Attribute: mSellSide Type: PriceLevel List: true
            tEncoder.addMessageArray("sellSide", mSellSide );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("BdxPriceLevel");
        
            //Decode Attribute: mSid Type:String List: false
            mSid = tDecoder.readString("sid");
        
            //Decode Attribute: mLevels Type:int List: false
            mLevels = tDecoder.readInteger("levels");
        
            //Decode Attribute: mBuySide Type:PriceLevel List: true
            mBuySide = (List<PriceLevel>) tDecoder.readMessageArray( "buySide", "array", PriceLevel.class );
        
            //Decode Attribute: mSellSide Type:PriceLevel List: true
            mSellSide = (List<PriceLevel>) tDecoder.readMessageArray( "sellSide", "array", PriceLevel.class );
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    
           public boolean same( BdxPriceLevel plm ) {
              if ((this.mBuySide.size() != plm.mBuySide.size()) || (this.mSellSide.size() != plm.mSellSide.size())) {
                 return false;
              }
              Iterator<PriceLevel> tItr1 = this.mBuySide.iterator();
              Iterator<PriceLevel> tItr2 = plm.mBuySide.iterator();
              while( tItr1.hasNext() ) {
                    if (!tItr1.next().same( tItr2.next())) {
                       return false;
                    }
              }

            tItr1 = this.mSellSide.iterator();
            tItr2 = plm.mSellSide.iterator();
            while( tItr1.hasNext() ) {
                if (!tItr1.next().same( tItr2.next())) {
                    return false;
                }
            }

            return true;
          }

          public String getSubjectName() {
            SID tSID = new SID( this.getSid().get());
            return "/" + this.getMessageName() + "/" + String.valueOf(tSID.getMarket()) + "/" + tSID.getSymbol();
          }

        

        public static  Builder getBdxPriceLevelBuilder() {
            return new BdxPriceLevel.Builder();
        }


        public static class  Builder {
          private BdxPriceLevel mInstance;

          private Builder () {
            mInstance = new BdxPriceLevel();
          }

        
                        public Builder setSid( String pValue ) {
                        mInstance.setSid( pValue );
                        return this;
                    }
                
                        public Builder setLevels( Integer pValue ) {
                        mInstance.setLevels( pValue );
                        return this;
                    }
                
                    public Builder setBuySide( List<PriceLevel> pValue )  {
                        mInstance.setBuySide( pValue );
                        return this;
                    }
                
                    public Builder setSellSide( List<PriceLevel> pValue )  {
                        mInstance.setSellSide( pValue );
                        return this;
                    }
                

        public BdxPriceLevel build() {
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

    