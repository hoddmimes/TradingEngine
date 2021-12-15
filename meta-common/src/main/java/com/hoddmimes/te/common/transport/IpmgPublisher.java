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


import com.hoddmimes.jsontransform.MessageInterface;
import com.hoddmimes.te.common.transport.tcpip.Ipmg;

import java.nio.ByteBuffer;

public class IpmgPublisher
{
    Ipmg mIpmg;

    public  void initialize( String pGroupAddress, int pPort ) throws Exception {
        initialize( pGroupAddress, pPort, null);
    }

    public void initialize( String pGroupAddress, int pPort, String pInterfaceAdress ) throws Exception{
            mIpmg = new Ipmg( pGroupAddress, pInterfaceAdress, pPort, 8192, 127 );
    }

    public IpmgPublisher() {
    }

    public void publish(MessageInterface pMsg ) throws Exception
    {
        byte[] tBuffer = pMsg.toJson().toString().getBytes();
        ByteBuffer tByteBuffer =  ByteBuffer.wrap( tBuffer );
        tByteBuffer.position( tBuffer.length );
        publish( tByteBuffer );
    }

    public void publish( ByteBuffer pBuffer) throws Exception{
        mIpmg.send( pBuffer );
    }

    public void close() {
        mIpmg.close();
    }
}
