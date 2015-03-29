
package com.leo.appmaster.privacycontact;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.EventId;
import com.leo.appmaster.fragment.BaseFragment;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOProgressDialog;

public class PrivacyCalllogFragment extends BaseFragment {

    private TextView mTextView;
    private LinearLayout mDefaultText;
    private ListView mContactCallLog;
    private CallLogAdapter mAdapter;
    private ArrayList<ContactCallLog> mContactCallLogs;
    private Context mContext;
    private boolean mIsEditModel = false;
    private LEOAlarmDialog mAddCallLogDialog;
    private List<ContactCallLog> mDeleteCallLog;
    private int mCallLogCount;
    private Handler mHandler;
    private LEOProgressDialog mProgressDialog;
    private SimpleDateFormat mSimpleDateFormate;
    private boolean mIsRead = false;
    private boolean mIsUpdate = true;
    private boolean mIsShow = false;

    @Override
    protected int layoutResourceId() {
        return R.layout.fragment_privacy_call_log;
    }

    @Override
    protected void onInitUI() {
        mContext = getActivity();
        mSimpleDateFormate = new SimpleDateFormat("yy/MM/dd");
        mTextView = (TextView) findViewById(R.id.content);
        mContactCallLog = (ListView) findViewById(R.id.contactLV);
        mDefaultText = (LinearLayout) findViewById(R.id.call_log_default_tv);
        mDeleteCallLog = new ArrayList<ContactCallLog>();
        mContactCallLogs = new ArrayList<ContactCallLog>();
        LeoEventBus.getDefaultBus().register(this);
        mAdapter = new CallLogAdapter(mContactCallLogs);
        mContactCallLog.setAdapter(mAdapter);
        mContactCallLog.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
                ContactCallLog calllog = mContactCallLogs.get(position);
                if (!mIsEditModel) {
                    String name = calllog.getCallLogName();
                    String number = calllog.getCallLogNumber();
                    String[] bundleData = new String[] {
                            name, number
                    };
                    Bundle bundle = new Bundle();
                    bundle.putStringArray(PrivacyContactUtils.CONTACT_CALL_LOG, bundleData);
                    Intent intent = new Intent(mContext, PrivacyCallLogListActivity.class);
                    intent.putExtras(bundle);
                    try {
                        startActivity(intent);
                        // 标记为已读
                        String readNumberFlag = PrivacyContactUtils.formatePhoneNumber(calllog
                                .getCallLogNumber());
                        updateMessageMyselfIsRead(1,
                                "call_log_phone_number LIKE ? and call_log_is_read = 0",
                                new String[] {
                                    "%" + readNumberFlag
                                }, mContext);
                        mAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                    }
                } else {
                    ImageView image = (ImageView) view.findViewById(R.id.call_log_itemCB);
                    if (!calllog.isCheck()) {
                        image.setImageDrawable(getResources().getDrawable(
                                R.drawable.select));
                        calllog.setCheck(true);
                        mDeleteCallLog.add(calllog);
                        mCallLogCount = mCallLogCount + 1;
                    } else {
                        image.setImageDrawable(getResources().getDrawable(
                                R.drawable.unselect));
                        calllog.setCheck(false);
                        mDeleteCallLog.remove(calllog);
                        mCallLogCount = mCallLogCount - 1;
                    }
                }
            }
        });

        mContactCallLog.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                LeoEventBus.getDefaultBus().post(
                        new PrivacyMessageEventBus(EventId.EVENT_PRIVACY_EDIT_MODEL,
                                PrivacyContactUtils.FROM_CALL_LOG_EVENT));
                mIsEditModel = true;
                mAdapter.notifyDataSetChanged();
                return true;
            }
        });

        PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
        task.execute("");
    }

    public void onEventMainThread(PrivacyDeletEditEventBus event) {
        if (PrivacyContactUtils.CANCEL_EDIT_MODEL.equals(event.editModel)) {
            restoreParameter();
            if (mContactCallLogs == null || mContactCallLogs.size() == 0) {
                mDefaultText.setVisibility(View.VISIBLE);
            } else {
                mDefaultText.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        } else if (PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE
                .equals(event.editModel)) {
            if (!mDeleteCallLog.isEmpty()) {
                showRestoreMessageDialog(
                        getResources().getString(R.string.privacy_call_delete_call_log),
                        PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE);
            }
        } else if (PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT.equals(event.editModel)
                || PrivacyContactUtils.CONTACT_DETAIL_DELETE_LOG_UPDATE_CALL_LOG_LIST
                        .equals(event.editModel)) {
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
        } else if (PrivacyContactUtils.PRIVACY_INTERCEPT_CONTACT_EVENT.equals(event.editModel)) {
            mIsRead = false;
            PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
            task.execute("");
            mIsUpdate = false;
            mIsShow = true;
            Log.d("PrivacyCallLogFrag", "Notification intercept!");
        } else if (PrivacyContactUtils.PRIVACY_ALL_CALL_NOTIFICATION_HANG_UP
                .equals(event.editModel)) {
            if (mIsUpdate) {
                // Log.e("PrivacyCallLogFrag", "Not intercept!");
                mIsRead = true;
                PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
                task.execute("");
            } else {
                // Log.e("PrivacyCallLogFrag", "Intercept!");
                mIsUpdate = true;
                if (mIsShow) {
                    // Log.e("PrivacyCallLogFrag", "Intercept show red tip!");
                    PrivacyContactCallLogTask task = new PrivacyContactCallLogTask();
                    task.execute("");
                    mIsShow = false;
                }
            }
        }
    }

    // 恢复编辑状态之前的参数状态
    public void restoreParameter() {
        mIsEditModel = false;
        mDeleteCallLog.clear();
        setCallLOgCheck(false);
        mCallLogCount = 0;
    }

    // 设置选中状态
    public void setCallLOgCheck(boolean flag) {
        for (ContactCallLog calllog : mContactCallLogs) {
            calllog.setCheck(flag);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        LeoEventBus.getDefaultBus().unregister(this);
        super.onDestroyView();
    }

    public void setContent(String content) {
        if (mTextView != null) {
            mTextView.setText(content);
        }
    }

    @SuppressLint("CutPasteId")
    private class CallLogAdapter extends BaseAdapter {
        LayoutInflater relativelayout;

        public CallLogAdapter(ArrayList<ContactCallLog> contacts) {
            relativelayout = LayoutInflater.from(mContext);
            // this.contacts = contacts;
        }

        @Override
        public int getCount() {

            return (mContactCallLogs != null) ? mContactCallLogs.size() : 0;
        }

        @Override
        public Object getItem(int position) {

            return mContactCallLogs.get(position);
        }

        @Override
        public long getItemId(int position) {

            return position;
        }

        class ViewHolder {
            ImageView checkImage, typeImage, bottomLine;
            CircleImageView contactIcon;
            TextView name, number, content, count;
        }

        @SuppressLint("InflateParams")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            ContactCallLog mb = mContactCallLogs.get(position);
            if (convertView == null) {
                vh = new ViewHolder();
                convertView = relativelayout.inflate(R.layout.fragmetn_privacy_call_log_list, null);
                vh.name = (TextView) convertView.findViewById(R.id.call_log_item_nameTV);
                vh.number = (TextView) convertView.findViewById(R.id.message_item_numberTV);
                vh.content = (TextView) convertView.findViewById(R.id.call_log_list_dateTV);
                vh.typeImage = (ImageView) convertView.findViewById(R.id.call_log_type);
                vh.checkImage = (ImageView) convertView.findViewById(R.id.call_log_itemCB);
                vh.checkImage.setTag(mb);
                vh.contactIcon = (CircleImageView) convertView.findViewById(R.id.contactIV);
                // vh.count = (TextView)
                // convertView.findViewById(R.id.call_log_list_countTV);
                vh.bottomLine = (ImageView) convertView.findViewById(R.id.bottom_line);
                vh.checkImage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        if (!mIsEditModel) {
                            ContactCallLog calllog = (ContactCallLog) arg0.getTag();
                            // 查询该号码是否为隐私联系人
                            String formateNumber = PrivacyContactUtils
                                    .formatePhoneNumber(calllog.getCallLogNumber());
                            ContactBean privacyConatact = MessagePrivacyReceiver.getPrivateMessage(
                                    formateNumber, mContext);
                            PrivacyContactManager.getInstance(mContext)
                                    .setLastCall(privacyConatact);
                            Uri uri = Uri.parse("tel:" + calllog.getCallLogNumber());
                            // 跳到拨号界面
                            // Intent intent = new Intent(Intent.ACTION_DIAL,
                            // uri);
                            // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            // 直接拨打电话
                            Intent intent = new Intent(Intent.ACTION_CALL, uri);
                            try {
                                startActivity(intent);
                                // // 添加到隐私通话中
                                // ContentValues values = new ContentValues();
                                // values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER,
                                // calllog.getCallLogNumber());
                                // values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME,
                                // calllog.getCallLogName());
                                // String date = mSimpleFormate.format(new
                                // Date());
                                // values.put(Constants.COLUMN_CALL_LOG_DATE,
                                // date);
                                // values.put(Constants.COLUMN_CALL_LOG_TYPE,
                                // CallLog.Calls.OUTGOING_TYPE);
                                // /*
                                // * 来电：CallLog.Calls.INCOMING_TYPE (常量值：1),
                                // * 已拨：CallLog.Calls.OUTGOING_TYPE (常量值：2)
                                // * 未接：CallLog.Calls.MISSED_TYPE (常量值：3)
                                // */
                                // mContext.getContentResolver().insert(
                                // Constants.PRIVACY_CALL_LOG_URI, values);
                                // // 删除系统通话记录
                                // String number = PrivacyContactUtils
                                // .formatePhoneNumber(mCallLogNumber);
                                // PrivacyContactUtils.deleteCallLogFromSystem("number LIKE ?",
                                // number,
                                // mContext);
                            } catch (Exception e) {
                            }
                        } else {
                            return;
                        }
                    }
                });
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            if (mb.getCallLogName() != null && !"".equals(mb.getCallLogName())) {
                vh.name.setText(mb.getCallLogName());
            } else {
                vh.name.setText(mb.getCallLogNumber());
            }
            vh.number.setText(mb.getCallLogNumber());
            Date tempDate = null;
            try {
                tempDate = mSimpleDateFormate.parse(mb.getClallLogDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            String date = mSimpleDateFormate.format(tempDate);
            vh.content.setText(date);
            if (mb != null) {
                vh.typeImage.setVisibility(View.VISIBLE);
                if (mb.getClallLogType() == CallLog.Calls.INCOMING_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.into_icon);
                } else if (mb.getClallLogType() == CallLog.Calls.OUTGOING_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.exhale_icon);
                } else if (mb.getClallLogType() == CallLog.Calls.MISSED_TYPE) {
                    vh.typeImage.setImageResource(R.drawable.into_icon);
                }
            }
            if (mIsEditModel) {
                // vh.checkImage.setOnClickListener(null);
                if (mb.isCheck()) {
                    vh.checkImage.setImageResource(R.drawable.select);
                } else {
                    vh.checkImage.setImageResource(R.drawable.unselect);
                }
            } else {
                vh.checkImage.setImageResource(R.drawable.privacy_call_log_item_call_bt_selecter);
            }
            // 设置未读数量
            if (mb.getCallLogCount() > 0) {
                vh.contactIcon.setAnswerStatus(PrivacyContactUtils.RED_TIP);
            } else {
                vh.contactIcon.setAnswerStatus("");
            }
            Bitmap icon = mb.getContactIcon();
            vh.contactIcon.setImageBitmap(icon);
            if (mContactCallLogs != null && mContactCallLogs.size() > 0) {
                if (position == mContactCallLogs.size() - 1) {
                    vh.bottomLine.setVisibility(View.GONE);
                } else {
                    vh.bottomLine.setVisibility(View.VISIBLE);
                }
            }
            return convertView;
        }
    }

    /**
     * getCallLog
     * 
     * @param phoneNumber
     * @return
     */
    private void getCallLog() {
        Map<String, ContactCallLog> callLogList = new HashMap<String, ContactCallLog>();
        Cursor cursor = mContext.getContentResolver()
                .query(Constants.PRIVACY_CALL_LOG_URI, null, null, null, "call_log_date desc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ContactCallLog callLog = new ContactCallLog();
                int count = cursor.getCount();
                String number = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_PHONE_NUMBER));
                String name = cursor.getString(cursor
                        .getColumnIndex(Constants.COLUMN_CALL_LOG_CONTACT_NAME));
                String date = cursor
                        .getString(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_DATE));
                callLog.setClallLogDate(date);
                int type = cursor.getInt(cursor.getColumnIndex(Constants.COLUMN_CALL_LOG_TYPE));
                callLog.setCallLogCount(count);
                callLog.setCallLogName(name);
                callLog.setCallLogNumber(number);
                callLog.setClallLogType(type);
                Bitmap icon = PrivacyContactUtils.getContactIcon(mContext, number);
                if (icon != null) {
                    callLog.setContactIcon(icon);
                } else {
                    callLog.setContactIcon(((BitmapDrawable) mContext.getResources().getDrawable(
                            R.drawable.default_user_avatar)).getBitmap());
                }
                if (!callLogList.containsKey(number)) {
                    if (!mIsRead) {
                        callLog.setCallLogCount(threadIdMessage(number));
                    } else {
                        // 正常接听模式红点处理
                        callLog.setCallLogCount(-1);
                        if (callLog.getIsRead() == 0) {
                            String readNumberFlag = PrivacyContactUtils.formatePhoneNumber(callLog
                                    .getCallLogNumber());
                            PrivacyCalllogFragment.updateMessageMyselfIsRead(1,
                                    "call_log_phone_number LIKE ? ",
                                    new String[] {
                                        "%" + readNumberFlag
                                    }, mContext);
                            callLog.setIsRead(1);
                        }
                    }
                    callLogList.put(number, callLog);
                }
            }
            Iterable<ContactCallLog> it = callLogList.values();
            for (ContactCallLog contactCallLog : it) {
                mContactCallLogs.add(contactCallLog);
            }
            Collections.sort(mContactCallLogs, PrivacyContactUtils.mCallLogCamparator);
            cursor.close();
        }
    }

    private void showProgressDialog(int maxValue, int currentValue) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEOProgressDialog(mContext);
        }
        String title = getResources().getString(R.string.privacy_contact_progress_dialog_title);
        String content = getResources().getString(R.string.privacy_contact_progress_dialog_content);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(content);
        mProgressDialog.setMax(maxValue);
        mProgressDialog.setProgress(currentValue);
        mProgressDialog.setButtonVisiable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private void showRestoreMessageDialog(String content, final String flag) {
        if (mAddCallLogDialog == null) {
            mAddCallLogDialog = new LEOAlarmDialog(mContext);
        }
        mAddCallLogDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    if (PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE.equals(flag)) {
                        /* sdk */
                        SDKWrapper.addEvent(mContext, SDKWrapper.P1, "privacyedit", "deletecall");
                        mHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                int currentValue = msg.what;
                                if (currentValue >= mCallLogCount) {
                                    if (mProgressDialog != null) {
                                        mProgressDialog.cancel();
                                        if (mContactCallLogs == null
                                                || mContactCallLogs.size() == 0) {
                                            mDefaultText.setVisibility(View.VISIBLE);
                                        } else {
                                            mDefaultText.setVisibility(View.GONE);
                                        }
                                        mAdapter.notifyDataSetChanged();
                                    }
                                } else {
                                    mProgressDialog.setProgress(currentValue);
                                }
                                super.handleMessage(msg);
                            }
                        };
                        showProgressDialog(mCallLogCount, 0);
                        PrivacyCallLogTask task = new PrivacyCallLogTask();
                        task.execute(PrivacyContactUtils.CALL_LOG_EDIT_MODEL_OPERATION_DELETE);

                    }
                } else if (which == 0) {
                    mAddCallLogDialog.cancel();
                    restoreParameter();
                    mAdapter.notifyDataSetChanged();
                    LeoEventBus.getDefaultBus().post(
                            new PrivacyMessageEventBus(EventId.EVENT_PRIVACY_EDIT_MODEL,
                                    PrivacyContactUtils.EDIT_MODEL_RESTOR_TO_SMS_CANCEL));
                }

            }
        });
        mAddCallLogDialog.setCanceledOnTouchOutside(false);
        mAddCallLogDialog.setContent(content);
        mAddCallLogDialog.show();
    }

    // 删除隐私通话记录
    private class PrivacyCallLogTask extends AsyncTask<String, Boolean, Boolean>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            int count = 0;
            for (ContactCallLog calllog : mDeleteCallLog) {
                int flagNumber = PrivacyContactUtils.deleteCallLogFromMySelf(
                        Constants.COLUMN_CALL_LOG_PHONE_NUMBER + " = ? ",
                        calllog.getCallLogNumber(),
                        mContext);
                if (flagNumber != -1) {
                    mContactCallLogs.remove(calllog);
                    Message messge = new Message();
                    count = count + 1;
                    messge.what = count;
                    mHandler.sendMessage(messge);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mIsEditModel = false;
            mCallLogCount = 0;
            mAdapter.notifyDataSetChanged();
            mDeleteCallLog.clear();
            LeoEventBus.getDefaultBus().post(
                    new PrivacyMessageEventBus(EventId.EVENT_PRIVACY_EDIT_MODEL,
                            PrivacyContactUtils.EDIT_MODEL_RESTOR_TO_SMS_CANCEL));
            super.onPostExecute(result);
        }
    }

    // 获取未读数量
    private int threadIdMessage(String number) {
        int count = 0;
        String fromateNumber = PrivacyContactUtils.formatePhoneNumber(number);
        Cursor cur = mContext.getContentResolver().query(Constants.PRIVACY_CALL_LOG_URI, null,
                Constants.COLUMN_CALL_LOG_PHONE_NUMBER
                        + " LIKE ? and call_log_is_read = 0",
                new String[] {
                    "%" + fromateNumber
                }, null);
        if (cur != null) {
            count = cur.getCount();
            cur.close();
        }
        return count;
    }

    // 标记为已读
    public static void updateMessageMyselfIsRead(int read, String selection,
            String[] selectionArgs, Context context) {
        ContentValues values = new ContentValues();
        values.put("call_log_is_read", read);
        int flag = context.getContentResolver().update(Constants.PRIVACY_CALL_LOG_URI,
                values, selection,
                selectionArgs);
    }

    private class PrivacyContactCallLogTask extends AsyncTask<String, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            mContactCallLogs.clear();
            getCallLog();
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (mContactCallLogs == null || mContactCallLogs.size() == 0) {
                mDefaultText.setVisibility(View.VISIBLE);
            } else {
                mDefaultText.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
}
