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

package com.hoddmimes.te.common.transport;

import com.google.gson.JsonObject;
import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.transport.tcpip.Ipmg;
import com.hoddmimes.te.messages.generated.MessageFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class IpmgSubscriber extends Thread
{
    Ipmg mIpmg;
    Logger  mLog;
    MessageFactory tMsgFactory;
    List<IpmgSubscriberListenerInterface> mSubscribers;


    public  void initialize( String pGroupAddress, int pPort ) throws Exception {
        initialize( pGroupAddress, pPort, null);
    }

    public void initialize( String pGroupAddress, int pPort, String pInterfaceAdress ) throws Exception{
            mIpmg = new Ipmg( pGroupAddress, pInterfaceAdress, pPort, 8192, 127 );
            this.start();

    }

    public IpmgSubscriber() {
        mSubscribers = new ArrayList<>();
        tMsgFactory = new MessageFactory();
        mLog = LogManager.getLogger( IpmgSubscriber.class );
    }

    public void addSubscriber( IpmgSubscriberListenerInterface pSubscriber ) {
        mSubscribers.add( pSubscriber );
    }

    public void run() {
        JsonObject jObject = null;
        setName("Multicast-Subscriber");
        ByteBuffer tBuffer = ByteBuffer.allocate(8192);
        while( true ) {
            tBuffer.clear();
            try {
                SocketAddress tSocketAddr = mIpmg.receive(tBuffer);
            }
            catch( Exception e) {
                throw new RuntimeException( e );
            }
            byte[] tStrBuf = new byte[tBuffer.position()];
            System.arraycopy( tBuffer.array(), 0, tStrBuf, 0, tBuffer.position());
            String tString =  new String(tStrBuf);
            MessageInterface tMsg = (MessageInterface) tMsgFactory.getMessageInstance( tString );
            if (tMsg == null) {
                mLog.error("failed to parse multicast message " + new String(tStrBuf));
            } else {
                for( IpmgSubscriberListenerInterface tCallback : mSubscribers) {
                    tCallback.multicastReceived( tMsg );
                }
            }
        }
    }

}
