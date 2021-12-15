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

package com.hoddmimes.te.marketdata;

class SubjectTokenParser {
	private static final char TOKEN_DELIMITER = '/';
	private int mPosition, mStackPointer;
	private String[] mStrArr;
	private int[] mPostionStack;

	SubjectTokenParser(String pString) {
		mPosition = 0;
		mStackPointer = 0;
		mPostionStack = new int[12];
		mStrArr = useIndexOf(pString, 0, 0);
	}

	int size() {
		return mStackPointer;
	}

	void savePosition() {
		mPostionStack[mStackPointer++] = mPosition;
	}

	void restorePosition() {
		mPosition = mPostionStack[--mStackPointer];
	}

	boolean lastElement() {
		if ((mPosition + 1) == mStrArr.length) {
			return true;
		} else {
			return false;
		}
	}

	String getNextElement() {
		if (mPosition >= mStrArr.length) {
			return null;
		}
		return mStrArr[mPosition++];
	}

	boolean hasMore() {
		return mPosition < mStrArr.length;
	}

	void setNewToken(String pString) {
		mPosition = 0;
		mStrArr = useIndexOf(pString, 0, 0);
	}

	int getPosition() {
		return mPosition;
	}

	void setPosition(int pPosition) {
		mPosition = pPosition;
	}

	String[] useIndexOf(String pIn, int pCnt, int pPos) {
		// Recursive version...

		String[] tRet;

		int tNextpos = pIn.indexOf(TOKEN_DELIMITER, pPos);

		if (tNextpos != -1) {
			tRet = useIndexOf(pIn, pCnt + 1, tNextpos + 1);
			if (pCnt != 0) {
				tRet[pCnt - 1] = pIn.substring(pPos, tNextpos);
			}
		} else {
			tRet = new String[pCnt];
			tRet[pCnt - 1] = pIn.substring(pPos, pIn.length());
		}

		return tRet;
	}

}