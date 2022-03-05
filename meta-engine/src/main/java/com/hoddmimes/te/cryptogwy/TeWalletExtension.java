/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
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

package com.hoddmimes.te.cryptogwy;

import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.WalletExtension;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class TeWalletExtension implements WalletExtension
{
	public final static String WALLET_EXTENSION = "TeWalletExtension";

	private long        mCreationTime;
	private String      mCreationTimeString;
	private long        mSaveTime;
	private String      mSaveTimeString;
	private long        mSaves;
	private             SimpleDateFormat mSDF;


		public TeWalletExtension() {
			mSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
			mCreationTime = System.currentTimeMillis();
			mCreationTimeString = mSDF.format( mCreationTime );
			mSaves = 0;
			mSaveTime = 0;
			mSaveTimeString = null;
		}

		public String getSaveTimeString() {
		return mCreationTimeString;
	}
		public String getCreationTimeString() {
			return mCreationTimeString;
		}

		public long getFileSavesCount() {
			return mSaves;
		}

		public long getCreationTime() {
			return mCreationTime;
		}
		public long getSaveTime() {
		return mCreationTime;
	}

		public void setCreationTime( long pTimestamp ) {
			mCreationTimeString = mSDF.format( pTimestamp );
			mCreationTime = pTimestamp;
		}

	public void setSaveTime( long pTimestamp ) {
		mSaveTimeString = mSDF.format( pTimestamp );
		mSaveTime = pTimestamp;
	}

		public void incrementsSaves() {
			mSaves += 1;
		}

		@Override
		public String getWalletExtensionID() {
			return WALLET_EXTENSION;
		}

		@Override
		public boolean isWalletExtensionMandatory() {
			return true;
		}

		@Override
		public byte[] serializeWalletExtension()
		{
			ByteBuffer bb = ByteBuffer.allocate( 100 );
			bb.putLong( mSaves );

			bb.putLong( mCreationTime );
			if (mCreationTimeString == null) {
				bb.putInt(0);
			} else {
				bb.putInt( mCreationTimeString.getBytes(StandardCharsets.UTF_8).length );
				bb.put( mCreationTimeString.getBytes(StandardCharsets.UTF_8));
			}

			bb.putLong( mSaveTime );
			if (mSaveTimeString == null) {
				bb.putInt(0);
			} else {
				bb.putInt( mSaveTimeString.getBytes(StandardCharsets.UTF_8).length );
				bb.put( mSaveTimeString.getBytes(StandardCharsets.UTF_8));
			}

			bb.flip();
			byte[] tData = new byte[ bb.limit()];
			bb.get( tData );
			return tData;
		}

		@Override
		public void deserializeWalletExtension(Wallet containingWallet, byte[] data) throws Exception {
			ByteBuffer bb = ByteBuffer.wrap( data );
			mSaves = bb.getLong();

			mCreationTime = bb.getLong();
			int tSize = bb.getInt();
			if (tSize == 0) {
				mCreationTimeString = null;
			} else {
				byte[] tBuf = new byte[ tSize ];
				bb.get( tBuf );
				mCreationTimeString = new String( tBuf );
			}

			mSaveTime = bb.getLong();
			tSize = bb.getInt();
			if (tSize == 0) {
				mSaveTimeString = null;
			} else {
				byte[] tBuf = new byte[ tSize ];
				bb.get( tBuf );
				mSaveTimeString = new String( tBuf );
			}
		}

		@Override
		public String toString() {
			return "wallet created: " + mCreationTimeString + " saved-count: " + mSaves + " save-time: " + mSaveTimeString;
		}
}
