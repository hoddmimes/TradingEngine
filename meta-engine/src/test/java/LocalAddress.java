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

import java.net.*;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class LocalAddress extends Thread {
		Integer x;

	public static void main(String[] args) {
		LocalAddress t = new LocalAddress();
		t.test();
	}


	private void test() {
		try {
			Iterator<NetworkInterface> tNics = NetworkInterface.getNetworkInterfaces().asIterator();
			while( tNics.hasNext()) {
				Iterator<InetAddress> tItrAddresses = tNics.next().getInetAddresses().asIterator();
				while ( tItrAddresses.hasNext() ) {
					InetAddress ia = tItrAddresses.next();
					if ((ia instanceof Inet4Address) && ((ia.isSiteLocalAddress()))) {
						Inet4Address ia4 = (Inet4Address) ia;
						System.out.println( ia.getHostAddress());
					}
				}
			}
	} catch (SocketException e) {
		e.printStackTrace();
	}
}
}


