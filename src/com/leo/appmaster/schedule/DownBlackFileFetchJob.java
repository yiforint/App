package com.leo.appmaster.schedule;

import android.content.Context;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.HttpRequestAgent;
import com.leo.appmaster.callfilter.CallFilterConstants;
import com.leo.appmaster.callfilter.CallFilterUtils;
import com.leo.appmaster.utils.NetWorkUtil;

/**
 * Created by runlee on 15-12-24.
 */
public class DownBlackFileFetchJob extends FetchScheduleJob {
    public static void startImmediately() {
        /*存在wifi网络再去拉取*/
        if (NetWorkUtil.isWifiConnected(AppMasterApplication.getInstance())) {
            startWork();
        }
    }

    @Override
    protected void work() {
        startWork();
    }


    private static void startWork() {
        DownBlackFileFetchJob job = new DownBlackFileFetchJob();
        FetchScheduleListener listener = job.newJsonObjListener();
        Context context = AppMasterApplication.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(CallFilterUtils.getBlackPath());
        sb.append(CallFilterConstants.BLACK_FILE_NAME);
        String filePath = sb.toString();
        HttpRequestAgent.getInstance(context).downloadBlackList(filePath, listener, listener);
    }
}