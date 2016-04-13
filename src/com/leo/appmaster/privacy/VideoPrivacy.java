package com.leo.appmaster.privacy;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.service.StatusBarEventService;
import com.leo.appmaster.db.LeoPreference;
import com.leo.appmaster.db.LeoSettings;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.NotificationUtil;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.videohide.NewHideVidActivity;
import com.leo.appmaster.videohide.VideoHideMainActivity;
import com.leo.appmaster.videohide.VideoItemBean;

/**
 * Created by Jasper on 2016/3/31.
 */
public class VideoPrivacy extends Privacy<VideoItemBean> {
    @Override
    public String getTag() {
        return "VideoPrivacy";
    }

    @Override
    protected boolean isConsumed() {
        return LeoSettings.getBoolean(PrefConst.KEY_VID_COMSUMED, false);
    }

    @Override
    public int getFoundStringId() {
        return R.string.hd_found_vid;
    }

    @Override
    public int getNewStringId() {
        return R.string.hd_new_vid;
    }

    @Override
    public int getProceedStringId() {
        return R.string.hd_hide_vid;
    }

    @Override
    public int getAddStringId() {
        return R.string.hd_add_hide_vid;
    }

    @Override
    public String getNotificationText() {
        String result = LeoSettings.getString(PrefConst.KEY_NOTIFY_VID_TITLE, null);
        if (result == null) {
            result = mContext.getString(R.string.hd_hide_vid_privacy_title);
        }
        return result;
    }

    @Override
    public String getNotificationSummary() {
        String result = LeoSettings.getString(PrefConst.KEY_NOTIFY_VID_CONTENT, null);
        if (result == null) {
            result = mContext.getString(R.string.hd_hide_vid_privacy_summary);
        }
        return result;
    }

    @Override
    public int getNotificationIconId() {
        return R.drawable.noti_video;
    }

    @Override
    public int getPrivacyLimit() {
        return LeoSettings.getInteger(PrefConst.KEY_NOTIFY_VID_COUNT, 3);
    }

    @Override
    public int getPrivacyType() {
        return PrivacyHelper.PRIVACY_HIDE_VID;
    }

    @Override
    public void jumpAction(Activity activity) {
        Intent imageIntent = null;
        int status = getStatus();
        switch (status) {
            case STATUS_NEW_ADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_new_cli");
                imageIntent = new Intent(activity, NewHideVidActivity.class);
                imageIntent.putExtra(Constants.FIRST_ENTER_VID, NewHideVidActivity.NEW_ADD_VID);
                break;
            case STATUS_FOUND:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_all_cli");
                imageIntent = new Intent(activity, NewHideVidActivity.class);
                imageIntent.putExtra(Constants.FIRST_ENTER_VID, NewHideVidActivity.FOUND_VID);
                break;
            case STATUS_TOADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_add_cli");
                VideoHideMainActivity.mFromHomeEnter = true;
                imageIntent = new Intent(activity, VideoHideMainActivity.class);
                break;
            case STATUS_PROCEED:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_hidden_cli");
                VideoHideMainActivity.mFromHomeEnter = true;
                imageIntent = new Intent(activity, VideoHideMainActivity.class);
                break;
        }
        activity.startActivity(imageIntent);
    }

    @Override
    public void reportExposure() {
        int status = getStatus();
        switch (status) {
            case STATUS_NEW_ADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_new_sh");
                break;
            case STATUS_PROCEED:

                break;
            case STATUS_FOUND:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_all_sh");
                break;
            case STATUS_TOADD:
                SDKWrapper.addEvent(mContext, SDKWrapper.P1, "home", "hidvid_add_sh");
                break;
        }
    }

    @Override
    public void showNotification() {
        Intent intent = new Intent(mContext, StatusBarEventService.class);
        intent.putExtra(Constants.PRIVACY_ENTER_SCAN_TYPE, getPrivacyType());
        intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, StatusBarEventService.EVENT_PRIVACY_VIDEO);
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

        SDKWrapper.addEvent(mContext, SDKWrapper.P1, "prilevel", "prilevel_notice_vid");
    }

    @Override
    public boolean isNotifyOpen() {
        return LeoSettings.getBoolean(PrefConst.KEY_NOTIFY_VID, true);
    }

    @Override
    protected boolean haveIgnored() {
        int lastRecord = LeoPreference.getInstance().getInt(PrefConst.KEY_NEW_LAST_ADD_VID, 0);
        return lastRecord > 0;
    }
}
