
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
            public class QueryPriceLevelsResponse implements MessageInterface 
            {
                public static String NAME = "QueryPriceLevelsResponse";

            
                    private String mRef;
                    private List<PriceLevelSymbol> mOrderbooks;
               public QueryPriceLevelsResponse()
               {
                
               }

               public QueryPriceLevelsResponse(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public QueryPriceLevelsResponse setRef( String pRef ) {
            mRef = pRef;
            return this;
            }
            public Optional<String> getRef() {
              return  Optional.ofNullable(mRef);
            }
        
            public QueryPriceLevelsResponse setOrderbooks( List<PriceLevelSymbol> pOrderbooks ) {
              if (pOrderbooks == null) {
                mOrderbooks = null;
                return this;
              }


            if ( mOrderbooks == null)
            mOrderbooks = ListFactory.getList("array");


            mOrderbooks .addAll( pOrderbooks );
            return this;
            }


            public QueryPriceLevelsResponse addOrderbooks( List<PriceLevelSymbol> pOrderbooks ) {

            if ( mOrderbooks == null)
            mOrderbooks = ListFactory.getList("array");

            mOrderbooks .addAll( pOrderbooks );
            return this;
            }

            public QueryPriceLevelsResponse addOrderbooks( PriceLevelSymbol pOrderbooks ) {

            if ( pOrderbooks == null) {
            return this; // Not supporting null in vectors, well design issue
            }

            if ( mOrderbooks == null) {
            mOrderbooks = ListFactory.getList("array");
            }

            mOrderbooks.add( pOrderbooks );
            return this;
            }


            public Optional<List<PriceLevelSymbol>> getOrderbooks() {

            if (mOrderbooks == null) {
                return  Optional.ofNullable(null);
            }

             //List<PriceLevelSymbol> tList = ListFactory.getList("array");
             //tList.addAll( mOrderbooks );
             // return  Optional.ofNullable(tList);
             return Optional.ofNullable(mOrderbooks);
            }

        

        public String getMessageName() {
        return "QueryPriceLevelsResponse";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("QueryPriceLevelsResponse", tEncoder.toJson() );
            //Encode Attribute: mRef Type: String List: false
            tEncoder.add( "ref", mRef );
        
            //Encode Attribute: mOrderbooks Type: PriceLevelSymbol List: true
            tEncoder.addMessageArray("orderbooks", mOrderbooks );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("QueryPriceLevelsResponse");
        
            //Decode Attribute: mRef Type:String List: false
            mRef = tDecoder.readString("ref");
        
            //Decode Attribute: mOrderbooks Type:PriceLevelSymbol List: true
            mOrderbooks = (List<PriceLevelSymbol>) tDecoder.readMessageArray( "orderbooks", "array", PriceLevelSymbol.class );
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getQueryPriceLevelsResponseBuilder() {
            return new QueryPriceLevelsResponse.Builder();
        }


        public static class  Builder {
          private QueryPriceLevelsResponse mInstance;

          private Builder () {
            mInstance = new QueryPriceLevelsResponse();
          }

        
                        public Builder setRef( String pValue ) {
                        mInstance.setRef( pValue );
                        return this;
                    }
                
                    public Builder setOrderbooks( List<PriceLevelSymbol> pValue )  {
                        mInstance.setOrderbooks( pValue );
                        return this;
                    }
                

        public QueryPriceLevelsResponse build() {
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

    