
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
            public class InternalPriceLevelResponse implements MessageInterface 
            {
                public static String NAME = "InternalPriceLevelResponse";

            
                    private BdxPriceLevel mBdxPriceLevel;
                    private String mRef;
               public InternalPriceLevelResponse()
               {
                
               }

               public InternalPriceLevelResponse(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    

            public Optional<BdxPriceLevel> getBdxPriceLevel() {
              return  Optional.ofNullable(mBdxPriceLevel);
            }

            public InternalPriceLevelResponse setBdxPriceLevel(BdxPriceLevel pBdxPriceLevel) {
            mBdxPriceLevel = pBdxPriceLevel;
            return this;
            }

        
            public InternalPriceLevelResponse setRef( String pRef ) {
            mRef = pRef;
            return this;
            }
            public Optional<String> getRef() {
              return  Optional.ofNullable(mRef);
            }
        

        public String getMessageName() {
        return "InternalPriceLevelResponse";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("InternalPriceLevelResponse", tEncoder.toJson() );
            //Encode Attribute: mBdxPriceLevel Type: BdxPriceLevel List: false
            tEncoder.add( "bdxPriceLevel", mBdxPriceLevel );
        
            //Encode Attribute: mRef Type: String List: false
            tEncoder.add( "ref", mRef );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("InternalPriceLevelResponse");
        
            //Decode Attribute: mBdxPriceLevel Type:BdxPriceLevel List: false
            mBdxPriceLevel = (BdxPriceLevel) tDecoder.readMessage( "bdxPriceLevel", BdxPriceLevel.class );
        
            //Decode Attribute: mRef Type:String List: false
            mRef = tDecoder.readString("ref");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getInternalPriceLevelResponseBuilder() {
            return new InternalPriceLevelResponse.Builder();
        }


        public static class  Builder {
          private InternalPriceLevelResponse mInstance;

          private Builder () {
            mInstance = new InternalPriceLevelResponse();
          }

        
                    public Builder setBdxPriceLevel( BdxPriceLevel pValue )  {
                        mInstance.setBdxPriceLevel( pValue );
                        return this;
                    }
                
                        public Builder setRef( String pValue ) {
                        mInstance.setRef( pValue );
                        return this;
                    }
                

        public InternalPriceLevelResponse build() {
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

    