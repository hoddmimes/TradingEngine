
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
            public class QueryPriceLevelsRequest implements MessageInterface 
            {
                public static String NAME = "QueryPriceLevelsRequest";

            
                    private String mRef;
               public QueryPriceLevelsRequest()
               {
                
               }

               public QueryPriceLevelsRequest(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public QueryPriceLevelsRequest setRef( String pRef ) {
            mRef = pRef;
            return this;
            }
            public Optional<String> getRef() {
              return  Optional.ofNullable(mRef);
            }
        

        public String getMessageName() {
        return "QueryPriceLevelsRequest";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("QueryPriceLevelsRequest", tEncoder.toJson() );
            //Encode Attribute: mRef Type: String List: false
            tEncoder.add( "ref", mRef );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("QueryPriceLevelsRequest");
        
            //Decode Attribute: mRef Type:String List: false
            mRef = tDecoder.readString("ref");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getQueryPriceLevelsRequestBuilder() {
            return new QueryPriceLevelsRequest.Builder();
        }


        public static class  Builder {
          private QueryPriceLevelsRequest mInstance;

          private Builder () {
            mInstance = new QueryPriceLevelsRequest();
          }

        
                        public Builder setRef( String pValue ) {
                        mInstance.setRef( pValue );
                        return this;
                    }
                

        public QueryPriceLevelsRequest build() {
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

    