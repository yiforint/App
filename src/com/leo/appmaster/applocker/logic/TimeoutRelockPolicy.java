package com.leo.appmaster.applocker.logic;

import java.util.HashMap;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.utils.LeoLog;

import android.content.Context;

public class TimeoutRelockPolicy implements ILockPolicy {

	private static final String TAG = "TimeoutRelockPolicy";

	Context mContext;

	private HashMap<String, Long> mLockapp = new HashMap<String, Long>();

	public TimeoutRelockPolicy(Context mContext) {
		super();
		this.mContext = mContext;

	}

	public int getRelockTime() {
		return AppMasterPreference.getInstance(mContext).getRelockTimeout();
	}

	@Override
	public boolean onHandleLock(String pkg) {
		long curTime = System.currentTimeMillis();
		if (mLockapp.containsKey(pkg)) {
			long lastLockTime = mLockapp.get(pkg);
			LeoLog.d(TAG, " curTime -  lastLockTime = "
					+ (curTime - lastLockTime) + "       mRelockTimeout =  "
					+ getRelockTime());
			if ((curTime - lastLockTime) < getRelockTime())
				return true;
		} else {
			mLockapp.put(pkg, curTime);
		}
		return false;
	}

	public void clearLockApp() {
		mLockapp.clear();
	}

	@Override
	public void onUnlocked(String pkg) {
		long curTime = System.currentTimeMillis();
		mLockapp.put(pkg, curTime);
	}

}
