package com.hoddmimes.te.sessionctl;


import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.interfaces.SessionCntxInterface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
        mMsgLogQueue.add( String.format("%24s [RQST       ] sid: %-20s account: %-12s %s",
                SDF.format( System.currentTimeMillis()),
                pRequestContext.getSessionContext().getSessionId(),
                pRequestContext.getAccountId(),
                pMessage.toJson().toString()));

    }

    public void logResponseMessage(RequestContext pRequestContext, MessageInterface pMessage) {
        mMsgLogQueue.add( String.format("%24s [RESP (%04d)] sid: %-20s account: %-12s %s",
                SDF.format( System.currentTimeMillis()),
                pRequestContext.getExecTimeUsec(),
                pRequestContext.getSessionContext().getSessionId(),
                pRequestContext.getAccountId(),
                pMessage.toJson().toString()));

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
