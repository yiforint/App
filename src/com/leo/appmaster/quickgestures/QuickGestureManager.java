
package com.leo.appmaster.quickgestures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.text.TextUtils;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.model.BaseInfo;
import com.leo.appmaster.quickgestures.model.QuickSwitcherInfo;

public class QuickGestureManager {
    public static final String TAG = "QuickGestureManager";

    private Context mContext;
    private static QuickGestureManager mInstance;
    private TreeSet<AppLauncherRecorder> mAppLaunchRecorders;

    private QuickGestureManager(Context ctx) {
        mContext = ctx;
        init();
    }

    public static synchronized QuickGestureManager getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new QuickGestureManager(ctx);
        }
        return mInstance;
    }

    private void init() {
        loadAppLaunchReoder();
    }

    public void loadAppLaunchReoder() {
        mAppLaunchRecorders = new TreeSet<QuickGestureManager.AppLauncherRecorder>();
        String recoders = AppMasterPreference.getInstance(mContext).getAppLaunchRecoder();
        AppLauncherRecorder temp = null;
        int sIndex = -1;
        if (!TextUtils.isEmpty(recoders)) {
            recoders = recoders.substring(0, recoders.length() - 1);
            String[] recoderList = recoders.split(";");
            for (String recoder : recoderList) {
                sIndex = recoder.indexOf(':');
                if (sIndex != -1) {
                    temp = new AppLauncherRecorder();
                    temp.pkg = recoder.substring(0, sIndex);
                    temp.launchCount = Integer.parseInt(recoder.substring(sIndex + 1));
                }
            }
        }
    }

    public void recodeAppLaunch(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        boolean hit = false;
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            if (recorder.pkg.equals(pkg)) {
                recorder.launchCount++;
                hit = true;
                break;
            }
        }
        if (!hit) {
            AppLauncherRecorder recoder = new AppLauncherRecorder();
            recoder.pkg = pkg;
            recoder.launchCount = 1;
            mAppLaunchRecorders.add(recoder);
        }

        StringBuilder resault = new StringBuilder();
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            resault.append(recorder.pkg).append(':').append(recorder.launchCount).append(';');
        }
        AppMasterPreference.getInstance(mContext).setAppLaunchRecoder(resault.toString());
    }

    public void removeAppLaunchRecoder(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        AppLauncherRecorder hitRecoder = null;
        for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
            if (recorder.pkg.equals(pkg)) {
                hitRecoder = recorder;
                break;
            }
        }
        if (hitRecoder != null) {
            mAppLaunchRecorders.remove(hitRecoder);
            StringBuilder resault = new StringBuilder();
            for (AppLauncherRecorder recorder : mAppLaunchRecorders) {
                resault.append(recorder.pkg).append(':').append(recorder.launchCount).append(';');
            }
            AppMasterPreference.getInstance(mContext).setAppLaunchRecoder(resault.toString());
        }
    }

    public List<BaseInfo> getDynamicList() {

        return null;
    }

    public List<BaseInfo> getMostUsedList() {

        return null;
    }

    public List<QuickSwitcherInfo> getSwitcherList() {

        return null;
    }

    public void updateSwitcherData(List<QuickSwitcherInfo> infos) {

    }

    public void onRunningPkgChanged(String pkg) {

    }

    public List<String> getFreeDisturbAppName() {
        List<String> packageNames = new ArrayList<String>();
        String packageName = AppMasterPreference.getInstance(mContext)
                .getFreeDisturbAppPackageName();
        if (!AppMasterPreference.PREF_QUICK_GESTURE_FREE_DISTURB_APP_PACKAGE_NAME
                .equals(packageName)) {
            String[] names = packageName.split(";");
            packageNames = Arrays.asList(names);
        }
        return packageNames;
    }

    class AppLauncherRecorder implements Comparable<AppLauncherRecorder> {
        String pkg;
        int launchCount;

        @Override
        public int compareTo(AppLauncherRecorder another) {
            if (launchCount > another.launchCount) {
                return 1;
            } else if (launchCount == another.launchCount) {
                return 0;
            } else {
                return -1;
            }
        }

    }

}
