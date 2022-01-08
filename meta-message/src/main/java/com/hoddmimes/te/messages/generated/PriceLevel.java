
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
            public class PriceLevel implements MessageInterface 
            {
                public static String NAME = "PriceLevel";

            
                    private Long mPrice;
                    private Integer mQuantity;
               public PriceLevel()
               {
                
               }

               public PriceLevel(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public PriceLevel setPrice( Long pPrice ) {
            mPrice = pPrice;
            return this;
            }
            public Optional<Long> getPrice() {
              return  Optional.ofNullable(mPrice);
            }
        
            public PriceLevel setQuantity( Integer pQuantity ) {
            mQuantity = pQuantity;
            return this;
            }
            public Optional<Integer> getQuantity() {
              return  Optional.ofNullable(mQuantity);
            }
        

        public String getMessageName() {
        return "PriceLevel";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = pEncoder;
            //Encode Attribute: mPrice Type: long List: false
            tEncoder.add( "price", mPrice );
        
            //Encode Attribute: mQuantity Type: int List: false
            tEncoder.add( "quantity", mQuantity );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder;
        
            //Decode Attribute: mPrice Type:long List: false
            mPrice = tDecoder.readLong("price");
        
            //Decode Attribute: mQuantity Type:int List: false
            mQuantity = tDecoder.readInteger("quantity");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    
            public boolean same( PriceLevel pl ) {
              if ((this.mQuantity != pl.mQuantity) || (this.mPrice != pl.mPrice)) {
                return false;
              }
              return true;
            }
        

        public static  Builder getPriceLevelBuilder() {
            return new PriceLevel.Builder();
        }


        public static class  Builder {
          private PriceLevel mInstance;

          private Builder () {
            mInstance = new PriceLevel();
          }

        
                        public Builder setPrice( Long pValue ) {
                        mInstance.setPrice( pValue );
                        return this;
                    }
                
                        public Builder setQuantity( Integer pValue ) {
                        mInstance.setQuantity( pValue );
                        return this;
                    }
                

        public PriceLevel build() {
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

    