package com.leo.appmaster.callfilter;

import java.util.ArrayList;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.service.StatusBarEventService;
import com.leo.appmaster.home.DeskProxyActivity;
import com.leo.appmaster.intruderprotection.IntruderprotectionActivity;
import com.leo.appmaster.mgr.CallFilterContextManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOChoiceDialog;
import com.leo.appmaster.ui.dialog.LEOWithSingleCheckboxDialog;
import com.leo.appmaster.ui.dialog.MultiChoicesWitchSummaryDialog;
import com.leo.appmaster.utils.Utilities;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RemoteViews;

public class CallFIlterUIHelper {
    //    private static LEOWithSIngleCheckboxDialog mConfirmRemoveFromBlacklistDialog;
    private static CallFIlterUIHelper mInstance = null;

    public LEOWithSingleCheckboxDialog getConfirmRemoveFromBlacklistDialog(Context context) {
        LEOWithSingleCheckboxDialog dialog = new LEOWithSingleCheckboxDialog(context);
        dialog.setCheckboxText(context.getResources().
                getString(R.string.call_filter_remove_from_blacklist_checkbox_text));
        dialog.setTitle(context.getResources().
                getString(R.string.call_filter_remove_from_blacklist_checkbox_title));
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public LEOChoiceDialog getCallHandleDialog(String title, Context context, boolean isNeedMarkItem) {
        LEOChoiceDialog dialog = new LEOChoiceDialog(context);
        ArrayList<String> list = new ArrayList<String>();
        list.add(context.getResources().getString(R.string.call_filter_delete_record));
        list.add(context.getResources().getString(R.string.call_filter_remove_from_blacklist));
        if (isNeedMarkItem) {
            list.add(context.getResources().getString(R.string.call_filter_mark));
        }
        dialog.setNeedCheckbox(false);
        dialog.setTitle(title);
        dialog.setTitleGravity(Gravity.CENTER);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setItemsWithDefaultStyle(list, 0);
        return dialog;
    }

    public MultiChoicesWitchSummaryDialog getCallHandleDialogWithSummary(String number, Context context, boolean isContentShow, int filterType) {
        final MultiChoicesWitchSummaryDialog dialog = new MultiChoicesWitchSummaryDialog(context);
        dialog.getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.setNowItemPosition(position);
            }
        });
        dialog.setTitle(String.format(context.getString(R.string.call_filter_marked_and_add_tips),number));
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setContentVisible(isContentShow);
        dialog.setContent(context.getResources().getString(R.string.call_filter_ask_add_to_blacklist));
        String[] itemContent = {context.getResources().getString(R.string.call_filter_mark_as_sr),
                context.getResources().getString(R.string.call_filter_mark_as_tx),
                context.getResources().getString(R.string.call_filter_mark_as_zp)};
        dialog.setCanceledOnTouchOutside(true);
        dialog.fillData(itemContent);
        if (filterType > 0 && filterType < 4) {
            dialog.setNowItemPosition(filterType - 1);
        } else {
            dialog.setNowItemPosition(0);
        }
        return dialog;
    }

    public static synchronized CallFIlterUIHelper getInstance() {
        if (mInstance == null) {
            mInstance = new CallFIlterUIHelper();
        }
        return mInstance;
    }

    public LEOAlarmDialog getConfirmClearAllRecordDialog(Context mContext) {
        LEOAlarmDialog dialog = new LEOAlarmDialog(mContext);
        dialog.setContentVisiable(false);
        dialog.setTitle(mContext.getResources().getString(R.string.call_filter_confirm_clear_record));
        dialog.setCanceledOnTouchOutside(true);
        dialog.setDialogIconVisibility(false);
        return dialog;
    }

    private CallFIlterUIHelper() {
    }

    public void showReceiveCallNotification(String number) {
        AppMasterApplication ama = AppMasterApplication.getInstance();
        /*判断通知提示是否打开*/
        CallFilterContextManager cmp = (CallFilterContextManager) MgrContext.getManager(MgrContext.MGR_CALL_FILTER);
        boolean filOpSta = cmp.getFilterNotiOpState();
        boolean isFilterSelected = cmp.getFilterOpenState();
        boolean isFilterTab = CallFilterManager.getInstance(ama).isIsFilterTab();
        if (!filOpSta || !isFilterSelected || isFilterTab) {
            return;
        }
        RemoteViews mRemoteViews = new RemoteViews(ama.getPackageName(), R.layout.remoteview_callfilter);
        mRemoteViews.setTextViewText(R.id.tv_title, ama.getResources().getString(R.string.call_filter_notifacation));
        mRemoteViews.setTextViewText(R.id.tv_content, number);
        mRemoteViews.setImageViewResource(R.id.iv_ic, R.drawable.intercept);

        Intent intent = new Intent(AppMasterApplication.getInstance(), DeskProxyActivity.class);
        intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, DeskProxyActivity.mFilterNoti);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ama, 1, intent, Notification.FLAG_AUTO_CANCEL);
        NotificationManager mNotificationManager = (NotificationManager) ama.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ama);
        mBuilder.setContent(mRemoteViews)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher);
        mNotificationManager.notify(1, mBuilder.build());
        
    }

    public void showStrangerNotification(int count) {
        Context context = AppMasterApplication.getInstance();
        String title = String.format(context.getResources().getString(R.string.str_noti_title_txt), "("+count+")");
        String content = String.format(context.getResources().getString(R.string.str_noti_content_txt), count);
        RemoteViews mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.remoteview_callfilter);
        mRemoteViews.setTextViewText(R.id.tv_title, title);
        mRemoteViews.setTextViewText(R.id.tv_content, content);
        mRemoteViews.setImageViewResource(R.id.iv_ic, R.drawable.intercept);
        Intent intent = new Intent(AppMasterApplication.getInstance(), DeskProxyActivity.class);
        intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, DeskProxyActivity.mStrangerCallNoti);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        mBuilder.setContent(mRemoteViews)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher);
        mNotificationManager.notify(CallFilterConstants.NOTI_ID_STRA, mBuilder.build());
    }

    public LEOAlarmDialog getConfirmAddToBlacklistDialog(Context context, String number, String markedPeople) {
        LEOAlarmDialog dialog = new LEOAlarmDialog(context);
        String titleS = context.getResources().getString(R.string.call_filter_confirm_add_to_blacklist);
        String titleF = String.format(titleS, number);
        String summaryS = context.getResources().getString(R.string.call_filter_confirm_add_to_blacklist_summary);
        String summaryF = String.format(summaryS, markedPeople);
        dialog.setTitle(titleF);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.setContent(summaryF);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setDialogIconVisibility(false);
        return dialog;
    }

    /**
     * 未接通知提示
     * @param count
     * @param number
     */
    public void showMissCallNotification(int count, String number) {
        Context context = AppMasterApplication.getInstance();
        AppMasterApplication ama = AppMasterApplication.getInstance();
        Intent intent = new Intent(AppMasterApplication.getInstance(), DeskProxyActivity.class);
        intent.putExtra(StatusBarEventService.EXTRA_EVENT_TYPE, DeskProxyActivity.mStrangerCallNoti);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(ama, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager mNotificationManager = (NotificationManager) ama.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ama);
        String title = context.getResources().getString(R.string.miss_noti_tit);
        String content = String.format(context.getResources().getString(R.string.miss_noti_content), number, count);
        mBuilder.setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher);
        mNotificationManager.notify(CallFilterConstants.NOTI_ID_STRA, mBuilder.build());
    }
}
