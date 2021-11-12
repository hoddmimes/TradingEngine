
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
            public class LogonResponse implements MessageInterface 
            {
                public static String NAME = "LogonResponse";

            
                    private Boolean mIsOk;
                    private String mRef;
                    private String mStatusMessage;
                    private String mSessionAuthId;
                    private String mExceptionMessage;
               public LogonResponse()
               {
                
               }

               public LogonResponse(String pJsonString ) {
                    
                    JsonDecoder tDecoder = new JsonDecoder( pJsonString );
                    this.decode( tDecoder );
               }
    
            public LogonResponse setIsOk( Boolean pIsOk ) {
            mIsOk = pIsOk;
            return this;
            }
            public Optional<Boolean> getIsOk() {
              return  Optional.ofNullable(mIsOk);
            }
        
            public LogonResponse setRef( String pRef ) {
            mRef = pRef;
            return this;
            }
            public Optional<String> getRef() {
              return  Optional.ofNullable(mRef);
            }
        
            public LogonResponse setStatusMessage( String pStatusMessage ) {
            mStatusMessage = pStatusMessage;
            return this;
            }
            public Optional<String> getStatusMessage() {
              return  Optional.ofNullable(mStatusMessage);
            }
        
            public LogonResponse setSessionAuthId( String pSessionAuthId ) {
            mSessionAuthId = pSessionAuthId;
            return this;
            }
            public Optional<String> getSessionAuthId() {
              return  Optional.ofNullable(mSessionAuthId);
            }
        
            public LogonResponse setExceptionMessage( String pExceptionMessage ) {
            mExceptionMessage = pExceptionMessage;
            return this;
            }
            public Optional<String> getExceptionMessage() {
              return  Optional.ofNullable(mExceptionMessage);
            }
        

        public String getMessageName() {
        return "LogonResponse";
        }
    

        public JsonObject toJson() {
            JsonEncoder tEncoder = new JsonEncoder();
            this.encode( tEncoder );
            return tEncoder.toJson();
        }

        
        public void encode( JsonEncoder pEncoder) {

        
            JsonEncoder tEncoder = new JsonEncoder();
            pEncoder.add("LogonResponse", tEncoder.toJson() );
            //Encode Attribute: mIsOk Type: boolean List: false
            tEncoder.add( "isOk", mIsOk );
        
            //Encode Attribute: mRef Type: String List: false
            tEncoder.add( "ref", mRef );
        
            //Encode Attribute: mStatusMessage Type: String List: false
            tEncoder.add( "statusMessage", mStatusMessage );
        
            //Encode Attribute: mSessionAuthId Type: String List: false
            tEncoder.add( "sessionAuthId", mSessionAuthId );
        
            //Encode Attribute: mExceptionMessage Type: String List: false
            tEncoder.add( "exceptionMessage", mExceptionMessage );
        
        }

        
        public void decode( JsonDecoder pDecoder) {

        
            JsonDecoder tDecoder = pDecoder.get("LogonResponse");
        
            //Decode Attribute: mIsOk Type:boolean List: false
            mIsOk = tDecoder.readBoolean("isOk");
        
            //Decode Attribute: mRef Type:String List: false
            mRef = tDecoder.readString("ref");
        
            //Decode Attribute: mStatusMessage Type:String List: false
            mStatusMessage = tDecoder.readString("statusMessage");
        
            //Decode Attribute: mSessionAuthId Type:String List: false
            mSessionAuthId = tDecoder.readString("sessionAuthId");
        
            //Decode Attribute: mExceptionMessage Type:String List: false
            mExceptionMessage = tDecoder.readString("exceptionMessage");
        

        }
    

        @Override
        public String toString() {
             Gson gsonPrinter = new GsonBuilder().setPrettyPrinting().create();
             return  gsonPrinter.toJson( this.toJson());
        }
    

        public static  Builder getLogonResponseBuilder() {
            return new LogonResponse.Builder();
        }


        public static class  Builder {
          private LogonResponse mInstance;

          private Builder () {
            mInstance = new LogonResponse();
          }

        
                        public Builder setIsOk( Boolean pValue ) {
                        mInstance.setIsOk( pValue );
                        return this;
                    }
                
                        public Builder setRef( String pValue ) {
                        mInstance.setRef( pValue );
                        return this;
                    }
                
                        public Builder setStatusMessage( String pValue ) {
                        mInstance.setStatusMessage( pValue );
                        return this;
                    }
                
                        public Builder setSessionAuthId( String pValue ) {
                        mInstance.setSessionAuthId( pValue );
                        return this;
                    }
                
                        public Builder setExceptionMessage( String pValue ) {
                        mInstance.setExceptionMessage( pValue );
                        return this;
                    }
                

        public LogonResponse build() {
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

    