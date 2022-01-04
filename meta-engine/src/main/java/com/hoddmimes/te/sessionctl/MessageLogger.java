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

package com.hoddmimes.te.sessionctl;


import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class MessageLogger extends Thread
{
    public enum FlushMode {NONE,INTERVAL,SYNC};

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final SimpleDateFormat DATETIMEFMT = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
    private PrintWriter             mWriter;
    private FileOutputStream        mOutStream;
    private BlockingQueue<String>   mMsgLogQueue;
    private String                  mFilename;
    private FlushMode               mFlushMode;
    private long                    mFlushInterval;
    private long                    mLastFlushMs;



    public MessageLogger(String pFilename ) {
        this( pFilename, FlushMode.NONE, 10000L );
    }

    public MessageLogger(String pFilename, FlushMode pFlushMode, long pFlushInterval ) {
        mFilename = pFilename;
        mLastFlushMs = System.currentTimeMillis();
        mFlushInterval = pFlushInterval;
        mFlushMode = pFlushMode;

        if (pFilename.contains("%datetime%")) {
            String tTimeStr = DATETIMEFMT.format( System.currentTimeMillis());
            mFilename = pFilename.replace("%datetime%", tTimeStr );
        }

        try {
            mOutStream = new FileOutputStream(mFilename);
            mWriter = new PrintWriter( mOutStream );
        }
        catch( IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        mMsgLogQueue = new LinkedBlockingDeque<>();
        this.start();
    }

    public String getFilename() {
      return mFilename;
    }

    public void log( String pEventMsg ) {
        mMsgLogQueue.add( pEventMsg );
    }

    public void logRequestMessage(RequestContext pRequestContext, MessageInterface pMessage) {
        mMsgLogQueue.add( String.format("[RQST       ] sid: %-20s account: %-12s %s",
                pRequestContext.getSessionContext().getSessionId(),
                pRequestContext.getAccountId(),
                pMessage.toJson().toString()));

    }

    public void logResponseMessage(RequestContext pRequestContext, MessageInterface pMessage, long pTxExecTime) {
        try {
            mMsgLogQueue.add(String.format("[RESP (%04d)] sid: %-20s account: %-12s %s",
                    pTxExecTime,
                    pRequestContext.getSessionContext().getSessionId(),
                    pRequestContext.getAccountId(),
                    pMessage.toJson().toString()));
        }
        catch( IllegalFormatConversionException fe) {
            fe.printStackTrace();
        }

    }

    public void logResponseMessage(RequestContext pRequestContext, MessageInterface pMessage) {
        logResponseMessage( pRequestContext, pMessage,  pRequestContext.getExecTimeUsec() );
    }


    public void toFile( String pLogEntry ) {
        if (pLogEntry == null) {
            return;
        }

        mWriter.println( SDF.format( System.currentTimeMillis()) + " " + pLogEntry);

        if (mFlushMode == FlushMode.INTERVAL) {
            long tNow = System.currentTimeMillis();
            if ((mLastFlushMs - tNow) >= mFlushInterval) {
                mWriter.flush();
                try {mOutStream.getFD().sync();} catch (IOException e)
                {
                    e.printStackTrace();
                }
                mLastFlushMs = tNow;
            }
        } else if (mFlushMode == FlushMode.SYNC) {
            mWriter.flush();
            try {
                mOutStream.getFD().sync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void toFile( List<String> pLogEntryList ) {
        if ((pLogEntryList == null) || (pLogEntryList.size() == 0)) {
            return;
        }

        for( String le : pLogEntryList) {
            toFile( le );
        }
    }


    @Override
    public void run() {
        List<String> tMsgList = new ArrayList<>(30);
        String tEventMsg = null;

        setName("MessageLogger");
        while( true ) {
            tEventMsg = null;
            try {tEventMsg = mMsgLogQueue.take();}
            catch( InterruptedException e) {}
            toFile( tEventMsg );
            tMsgList.clear();
            mMsgLogQueue.drainTo( tMsgList, 30);
            toFile( tMsgList );
        }
    }

}
