package com.leo.appmaster.privacy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.AppLockListActivity;
import com.leo.appmaster.applocker.lockswitch.BlueToothLockSwitch;
import com.leo.appmaster.applocker.lockswitch.SwitchGroup;
import com.leo.appmaster.applocker.lockswitch.WifiLockSwitch;
import com.leo.appmaster.applocker.model.LockMode;
import com.leo.appmaster.applocker.service.StatusBarEventService;
import com.leo.appmaster.db.LeoSettings;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.model.AppItemInfo;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.NotificationUtil;
import com.leo.appmaster.utils.PrefConst;

/**
 * Created by Jasper on 2016/3/31.
 */
public class LockPrivacy extends Privacy<AppItemInfo> {
    public static final String FROM_HOME_RESULT_APP = "from_home_result_app";

    @Override
    public String getTag() {
        return "LockPrivacy";
    }

    @Override
    protected boolean isConsumed() {
        return LeoSettings.getBoolean(PrefConst.KEY_APP_COMSUMED, false);
    }

    @Override
    public int getFoundStringId() {
        return R.string.hd_found_app;
    }

    @Override
    public int getNewStringId() {
        return R.string.hd_new_app;
    }

    @Override
    public int getProceedStringId() {
        return R.string.hd_locked_app;
    }

    @Override
    public int getAddStringId() {
        return R.string.hd_add_locked_app;
    }

    @Override
    public void ignoreNew() {
        clearNewList();

        LockManager lm = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        lm.ignore();
    }

    @Override
    public int getDangerTipId() {
        return R.string.hd_app_danger_tip;
    }

    @Override
    public String getNotificationText() {
        String result = LeoSettings.getString(PrefConst.KEY_NOTIFY_APP_TITLE, null);
        if (result == null) {
            result = mContext.getString(R.string.hd_lock_privacy_title);
        }
        return result;
    }

    @Override
    public String getNotificationSummary() {
        String result = LeoSettings.getString(PrefConst.KEY_NOTIFY_APP_CONTENT, null);
        if (result == null) {
            result = mContext.getString(R.string.hd_lock_privacy_summary);
        }
        return result;
    }

    @Override
    public int getNotificationIconId() {
        return R.drawable.noti_lock;
    }

    @Override
    public int getPrivacyLimit() {
        return LeoSettings.getInteger(PrefConst.KEY_NOTIFY_APP_COUNT, 5);
    }

    @Override
    public int getTotalCount() {
        int count = super.getTotalCount();

        SwitchGroup wifi = new WifiLockSwitch();
        SwitchGroup bluetooth = new BlueToothLockSwitch();

        LockManager lm = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        LockMode lockMode = lm.getCurLockMode();
        if (lockMode == null) {
            return count;
        }
        if (wifi.isLockNow(lockMode)) {
            count++;
        }

        if (bluetooth.isLockNow(lockMode)) {
            count++;
        }
        return count;
    }

    @Override
    public int getNewCount() {
        int count = super.getNewCount();
        if (isConsumed()) {
            return count;
        }
        SwitchGroup wifi = new WifiLockSwitch();
        SwitchGroup bluetooth = new BlueToothLockSwitch();

        LockManager lm = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        LockMode lockMode = lm.getCurLockMode();
        if (lockMode == null) {
            return count;
        }
        if (wifi.isLockNow(lockMode)) {
            count++;
        }

        if (bluetooth.isLockNow(lockMode)) {
            count++;
        }
        return count;
    }

    @Override
    public int getProceedCount() {
        int count = super.getProceedCount();

        SwitchGroup wifi = new WifiLockSwitch();
        SwitchGroup bluetooth = new BlueToothLockSwitch();

        LockManager lm = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
        LockMode lockMode = lm.getCurLockMode();
        if (lockMode == null) {
            return count;
        }
        if (wifi.isLockNow(lockMode)) {
            count++;
        }

        if (bluetooth.isLockNow(lockMode)) {
            count++;
        }
        return count;
    }

    @Override
    public int getPrivacyType() {
        return PrivacyHelper.PRIVACY_APP_LOCK;
    }

    @Override
    public void jumpAction(Activity activity) {
        int status = getStatus();
        Privacy privacy = PrivacyHelper.getAppPrivacy();
        switch (status) {
            case STATUS_NEW_ADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_new_cli");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cli_$new"+privacy.getPrivacyCountText());
                break;
            case STATUS_PROCEED:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_hidden_cli");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cli_$hidden"+privacy.getPrivacyCountText());
                break;
            case STATUS_FOUND:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_all_cli");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cli_$all"+privacy.getPrivacyCountText());
                break;
            case STATUS_TOADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_add_cli");
                break;
        }
        Intent appIntent = new Intent(activity, AppLockListActivity.class);
        if (getNewCount() > 0 && getNewCount() != getTotalCount()) {
            appIntent.putExtra(Constants.FROM_APP_SCAN_RESULT, true);
        }
        appIntent.putExtra(FROM_HOME_RESULT_APP, true);
        activity.startActivity(appIntent);

    }

    @Override
    public void reportExposure() {
        int status = getStatus();
        Privacy privacy = PrivacyHelper.getAppPrivacy();
        switch (status) {
            case STATUS_NEW_ADD:
                LeoLog.e("reportExposureApp","新增");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_new_sh");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cnts_$new"+privacy.getPrivacyCountText());
                break;
            case STATUS_PROCEED:
                LeoLog.e("reportExposureApp","已加锁");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_hidden_cli");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cnts_$hidden"+privacy.getPrivacyCountText());
                break;
            case STATUS_FOUND:
                LeoLog.e("reportExposureApp","待处理");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_all_sh");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home_advice", "app_cnts_$all"+privacy.getPrivacyCountText());
                break;
            case STATUS_TOADD:
                LeoLog.e("reportExposureApp","无加锁");
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "lock_add_sh");
                break;
        }
    }

    @Override
    public void showNotification() {
        Intent intent = new Intent(mContext, StatusBarEventService.class);
        intent.putExtra(Constants.PRIVACY_ENTER_SCAN_TYPE, getPrivacyType());
        intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, StatusBarEventService.EVENT_PRIVACY_APP);
        Notification notif = new Notification();
        PendingIntent contentIntent = PendingIntent.getService(mContext, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        notif.icon = getNotificationIconId();

        String title = getNotificationText();
        String content = getNotificationSummary();
        notif.tickerText = getNotificationText();
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(mContext, title, content, contentIntent);
        NotificationUtil.setBigIcon(notif, getNotificationIconId());
        notif.when = System.currentTimeMillis();
        NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTI_ID, notif);

        SDKWrapper.addEvent(mContext, SDKWrapper.P1, "prilevel", "prilevel_notice_app");
    }

    @Override
    public boolean isNotifyOpen() {
        return LeoSettings.getBoolean(PrefConst.KEY_NOTIFY_APP, true);
    }

    @Override
    protected boolean haveIgnored() {
        return isConsumed();
    }
}
