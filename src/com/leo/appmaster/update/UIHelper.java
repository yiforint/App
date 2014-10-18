
package com.leo.appmaster.update;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.leo.appmaster.R;
import com.leoers.leoanalytics.update.IUIHelper;
import com.leoers.leoanalytics.update.UpdateManager;

public class UIHelper implements IUIHelper {

    private final static String TAG = UIHelper.class.getSimpleName();

    private static UIHelper sUIHelper = null;
    private Context mContext = null;
    private UpdateManager mManager = null;
    private OnProgressListener listener = null;

    private NotificationManager nm = null;
    // private RemoteViews updateRv = null;
    private RemoteViews downloadRv = null;
    private Notification updateNotification = null;
    private Notification downloadNotification = null;

    public final static String ACTION_NEED_UPDATE = "com.leo.appmaster.update";
    public final static String ACTION_CANCEL_UPDATE = "com.leo.appmaster.update.cancel";
    public final static String ACTION_DOWNLOADING = "com.leo.appmaster.download";
    public final static String ACTION_CANCEL_DOWNLOAD = "com.leo.appmaster.update.cancel";

    private final static int DOWNLOAD_NOTIFICATION_ID = 1001;
    private final static int UPDATE_NOTIFICATION_ID = 1002;

    private int mUIType = IUIHelper.TYPE_CHECKING;
    private int mUIParam = 0;
    private int mProgress = 0;

    private UIHelper(Context ctx) {
        mContext = ctx;
        /* new version found */
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEED_UPDATE);
        mContext.registerReceiver(receive, filter);
        /* for cancel update */
        filter = new IntentFilter();
        filter.addAction(ACTION_CANCEL_UPDATE);
        mContext.registerReceiver(receive, filter);
        /* show downloading dialog */
        filter = new IntentFilter();
        filter.addAction(ACTION_DOWNLOADING);
        mContext.registerReceiver(receive, filter);
        /* for cancel download */
        filter = new IntentFilter();
        filter.addAction(ACTION_CANCEL_DOWNLOAD);
        mContext.registerReceiver(receive, filter);
    }

    public static UIHelper getInstance(Context ctx) {
        if (sUIHelper == null) {
            sUIHelper = new UIHelper(ctx);
        }
        return sUIHelper;
    }

    /* all function needs UpdateManager have to invoke after this call */
    public void setManager(UpdateManager manager) {
        mManager = manager;
        nm = (NotificationManager) mContext
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // buildUpdatedNotification();
        buildDownloadNotification();
    }

    public void setOnProgressListener(OnProgressListener l) {
        listener = l;
    }

    @SuppressWarnings("deprecation")
    private void buildDownloadNotification() {
        String appName = mContext.getString(R.string.app_name);
        String downloadTip = mContext.getString(R.string.downloading, appName);
        CharSequence from = appName;
        CharSequence message = downloadTip;
        downloadRv = new RemoteViews(mContext.getPackageName(),
                R.layout.sdk_notification_download);
        downloadRv.setTextViewText(R.id.tv_title, downloadTip);
        // Intent intent = new Intent(UIHelper.ACTION_DOWNLOADING);
        // PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0,
        // intent, 0);
        // go back to app - begin
        Intent intent = new Intent(mContext, UpdateActivity.class);
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                UpdateActivity.class.getName());
        intent.setComponent(componentName);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.addFlags(Notification.FLAG_ONGOING_EVENT);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // go back to app - end
        downloadNotification = new Notification(R.drawable.ic_launcher_notification,
                downloadTip, System.currentTimeMillis());
        downloadNotification.setLatestEventInfo(mContext, from, message, contentIntent);
        downloadNotification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
    }

    public void sendDownloadNotification(int progress) {
        Log.d(TAG, "sendDownloadNotification called ");
        String appName = mContext.getString(R.string.app_name);
        downloadRv.setProgressBar(R.id.pb_download, 100, progress, false);
        downloadRv.setTextViewText(R.id.tv_content, mContext.getString(R.string.downloading_notification,
                appName, progress)+"%");
        // downloadRv.setTextViewText(R.id.tv_progress, progress + "%");
        downloadNotification.contentView = downloadRv;
        nm.notify(DOWNLOAD_NOTIFICATION_ID, downloadNotification);
    }

    public void cancelDownloadNotification() {
        nm.cancel(DOWNLOAD_NOTIFICATION_ID);
    }

    @SuppressWarnings("deprecation")
    private void sendUpdateNotification() {
        String appName = mContext.getString(R.string.app_name);
        String updateTip = mContext.getString(R.string.update_available, appName);
        // Intent intent = new Intent(UIHelper.ACTION_NEED_UPDATE);
        // PendingIntent contentIntent = PendingIntent.getBroadcast(mContext, 0,
        // intent, 0);
        // go back to app - begin
        Intent intent = new Intent(mContext, UpdateActivity.class);
        ComponentName componentName = new ComponentName(mContext.getPackageName(),
                UpdateActivity.class.getName());
        intent.setComponent(componentName);
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.addFlags(Notification.FLAG_ONGOING_EVENT);
        PendingIntent contentIntent = PendingIntent.getActivity(mContext,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // go back to app - end

        updateNotification = new Notification(R.drawable.ic_launcher_notification,
                updateTip, System.currentTimeMillis());
        Intent dIntent = new Intent(UIHelper.ACTION_CANCEL_UPDATE);
        PendingIntent delIntent = PendingIntent.getBroadcast(mContext, 0,
                dIntent, 0);
        updateNotification.deleteIntent = delIntent;
        String contentText = mContext.getString(R.string.version_found, mManager.getVersion());
        updateNotification.setLatestEventInfo(mContext, updateTip, contentText, contentIntent);
        updateNotification.flags = Notification.FLAG_AUTO_CANCEL
                | Notification.FLAG_ONGOING_EVENT;
        nm.notify(UPDATE_NOTIFICATION_ID, updateNotification);
    }

    @Override
    public void onNewState(int ui_type, int param) {
        mUIType = ui_type;
        mUIParam = param;
        if (ui_type == IUIHelper.TYPE_CHECK_NEED_UPDATE && !isRunningForeground(mContext)) {
            Log.e(TAG, "runing on background, show update notification");
            sendUpdateNotification();
        } else {
            showUpdateActivity(ui_type, param);
        }
    }

    private boolean isActivityOnTop(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        if (cn.getClassName().equals(UpdateActivity.class.getName())) {
            return true;
        }
        return false;
    }

    private boolean isAppOnTop(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName)
                && currentPackageName.equals(context.getPackageName())) {
            return true;
        }

        return false;
    }

    private boolean isRunningForeground(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String currentPackageName = cn.getPackageName();
        if (!TextUtils.isEmpty(currentPackageName)
                && currentPackageName.equals(context.getPackageName())) {
            return true;
        }

        return false;
    }

    public int getProgress() {
        return mProgress;
    }

    @Override
    public void onProgress(int complete, int total) {
        long c = complete;
        long t = total;
        mProgress = (total == 0) ? 0 : (int) (c * 100 / t);
        if (!isActivityOnTop(mContext)) {
            Log.d(TAG, "sendDownloadNotification in onProgress of UIHelper");
            sendDownloadNotification(mProgress);
        } else {
            cancelDownloadNotification();
        }
        if (listener != null) {
            listener.onProgress(complete, total);
        }
    }

    private void showUpdateActivity(int type, int param) {
        Log.d(TAG, "type=" + type + "; param=" + param);
        if (isActivityOnTop(mContext) && listener != null) {
            Log.d(TAG, "activity on top");
            listener.onChangeState(type, param);
        } else if (isAppOnTop(mContext)) {
            Log.d(TAG, "showing activity type=" + type);
            Intent i = new Intent();
            i.setClass(mContext, UpdateActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.putExtra(LAYOUT_TYPE, type);
            i.putExtra(LAYOUT_PARAM, param);
            mContext.startActivity(i);
        } else {
            Log.d(TAG, "we should show a notification here");
        }
    }

    BroadcastReceiver receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive action =" + action);
            if (action.equals(ACTION_NEED_UPDATE)) {
                nm.cancel(UPDATE_NOTIFICATION_ID);
                showUpdateActivity(IUIHelper.TYPE_UPDATE, mManager.getReleaseType());
            } else if (action.equals(ACTION_CANCEL_UPDATE)) {
                mManager.onCancelUpdate();
            } else if (action.equals(ACTION_DOWNLOADING)) {
                showUpdateActivity(IUIHelper.TYPE_DOWNLOADING, mManager.getReleaseType());
            } else if (action.equals(ACTION_CANCEL_DOWNLOAD)) {
                mManager.onCancelDownload();
                // TODO: how to stop showming the last progress UI
            }/* done */
        }
    };

    @Override
    public int getLayoutType() {
        return mUIType;
    }

    @Override
    public int getLayoutParam() {
        return mUIParam;
    }

}
