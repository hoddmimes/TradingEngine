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

package com.hoddmimes.te.management;

import com.hoddmimes.te.messages.generated.MgmtStatEntry;

import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RateItem
{
	private AtomicInteger    m1SecCount;
	private AtomicInteger    m10SecCount;
	private AtomicInteger    m60SecCount;
	private AtomicLong       mTotals;


	private AtomicInteger  m1Sec,m1Max;
	private AtomicInteger  m10Sec,m10Max;
	private AtomicInteger  m60Sec,m60Max;
	private int     mTicks;
	private NumberFormat numfmt;

	String          mAttribute;

	public RateItem( String pAttribute ) {
		numfmt = NumberFormat.getInstance();
		numfmt.setGroupingUsed(false);
		numfmt.setMaximumFractionDigits(1);
		numfmt.setMinimumFractionDigits(1);
		mTotals = new AtomicLong(0);
		m1SecCount = new AtomicInteger(0);
		m10SecCount = new AtomicInteger(0);
		m60SecCount = new AtomicInteger(0);
		m1Sec = new AtomicInteger(0);
		m1Max = new AtomicInteger(0);
		m10Sec = new AtomicInteger(0);
		m10Max = new AtomicInteger(0);
		m60Sec = new AtomicInteger(0);
		m60Max = new AtomicInteger(0);
		mTicks = 0;
		mAttribute = pAttribute;
	}

	public void increment() {
		mTotals.incrementAndGet();
		m1SecCount.incrementAndGet();
		m10SecCount.incrementAndGet();
		m60SecCount.incrementAndGet();
	}

	public void increment( int pDelta ) {
		mTotals.addAndGet( pDelta );
		m1SecCount.addAndGet( pDelta );
		m10SecCount.addAndGet( pDelta );
		m60SecCount.addAndGet( pDelta );
	}

	public void valuate() {
		mTicks++;
		m1Sec.set(m1SecCount.get());
		m1Max.set( Math.max(m1Sec.get(),m1Max.get()));
		m1SecCount.set(0);


		if ((mTicks % 10) == 0) {
			m10Sec.set(m10SecCount.get());
			m10Max.set( Math.max(m10Sec.get(),m10Max.get()));
			m10SecCount.set(0);
		}
		if ((mTicks % 60) == 0) {
			m60Sec.set(m60SecCount.get());
			m60Max.set( Math.max(m60Sec.get(),m60Max.get()));
			m60SecCount.set(0);
			mTicks = 0;
		}
	}

	public double get1Sec() {
		return (double) m1Sec.get();
	}

	public double get10Sec() {
		return (double) m10Sec.get() / 10.0d;
	}

	public double get60Sec() {
		return (double) m60Sec.get() / 10.0d;
	}

	public double get1SecMax() {
		return (double) m1Max.get();
	}

	public double get10SecMax() {
		return (double) m10Max.get() / 10.0d;
	}

	public double get60SecMax() {
		return (double) m60Max.get() / 60.0d;
	}

	public MgmtStatEntry get1SecStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (1 sec)").setValue( numfmt.format( get1Sec() ));
	}

	public long getTotals() {
		return mTotals.get();
	}

	public MgmtStatEntry get10SecStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (10 sec)").setValue( numfmt.format( get10Sec() ));
	}

	public MgmtStatEntry get60SecStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (60 sec)").setValue( numfmt.format( get60Sec() ));
	}

	public MgmtStatEntry get1SecMaxStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (Max, 1 sec)").setValue( numfmt.format( get1SecMax() ));
	}

	public MgmtStatEntry get10SecMaxStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (Max, 10 sec)").setValue( numfmt.format( get10SecMax() ));
	}

	public MgmtStatEntry get60SecMaxStat() {
		return new MgmtStatEntry().setAttribute( mAttribute + " (Max, 60 sec)" +
				")").setValue( numfmt.format( get60SecMax() ));
	}

}
