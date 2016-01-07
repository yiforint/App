
package com.leo.appmaster.privacycontact;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.callfilter.BlackListInfo;
import com.leo.appmaster.callfilter.CallFilterConstants;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.PrivacyEditFloatEvent;
import com.leo.appmaster.eventbus.event.PrivacyMessageEvent;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonToolbar;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEORoundProgressDialog;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.Utilities;

public class AddFromMessageListActivity extends BaseActivity implements OnItemClickListener {
    private static final String TAG = "AddFromMessageListActivity";
    private static final int HAVE_BLACK_LIST = -2;
    private ListView mListMessage;
    private MyMessageAdapter mAdapter;
    private List<MessageBean> mMessageList;
    private CommonToolbar mTtileBar;
    private List<MessageBean> mAddPrivacyMessage;
    private LEOAlarmDialog mAddMessageDialog;
    private LEORoundProgressDialog mProgressDialog;
    private ProgressBar mProgressBar;
    private List<MessageBean> mAddMessages;
    private List<ContactCallLog> mAddCallLogs;
    private int mAnswerType = 1;
    private boolean mLogFlag = false;
    private LinearLayout mDefaultText;
    private Button mAutoDddBtn;
    private RippleView rippleView;
    private AddFromMsmHandler mAddFromMsmHandler = new AddFromMsmHandler();
    private String mFrom;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int currentValue = msg.what;

            if (currentValue == HAVE_BLACK_LIST) {
                Context context = AddFromMessageListActivity.this;
                String str = getResources().getString(R.string.call_filter_have_add_black_num);
                Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentValue >= mAddPrivacyMessage.size()) {
                if (!mLogFlag) {
                    if (mProgressDialog != null) {
                        mProgressDialog.cancel();
                    }

                    if (!Utilities.isEmpty(mFrom) &&
                            mFrom.equals(CallFilterConstants.FROM_BLACK_LIST)) {
                        Context context = AddFromMessageListActivity.this;
                        String str = getResources().getString(R.string.add_black_list_done);
                        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
                    }
                    AddFromMessageListActivity.this.finish();
                } else {
                    if (mProgressDialog != null) {
                        mProgressDialog.cancel();
                    }
                    mLogFlag = false;
                }
            } else {
                mProgressDialog.setProgress(currentValue);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_privacy_message);
        handleIntent();
        initUI();
        sendMsgHandler();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        mFrom = intent.getStringExtra(CallFilterConstants.FROMWHERE);
    }

    private void initUI() {
        mDefaultText = (LinearLayout) findViewById(R.id.add_message_default_tv);
        rippleView = (RippleView) mDefaultText.findViewById(R.id.moto_add_btn_ripp);
        rippleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Utilities.isEmpty(mFrom) &&
                        mFrom.equals(CallFilterConstants.FROM_BLACK_LIST)) {
                    Intent intent = new Intent(AddFromMessageListActivity.this,
                            PrivacyContactInputActivity.class);
                    intent.putExtra(CallFilterConstants.FROMWHERE,
                            CallFilterConstants.FROM_BLACK_LIST);
                    startActivity(intent);
                    finish();
                } else {
                    /* SDK */
                    SDKWrapper.addEvent(AddFromMessageListActivity.this, SDKWrapper.P1, "contactsadd",
                            "smsemptyadd");
                    Intent intent = new Intent(AddFromMessageListActivity.this,
                            PrivacyContactInputActivity.class);
                    intent.putExtra(PrivacyContactInputActivity.TO_CONTACT_LIST, true);
                    startActivity(intent);
                }
            }
        });
        mAutoDddBtn = (Button) mDefaultText.findViewById(R.id.moto_add_btn);
        mTtileBar = (CommonToolbar) findViewById(R.id.add_privacy_contact_title_bar);
        mTtileBar.setToolbarColorResource(R.color.cb);
        mTtileBar.setOptionImageResource(R.drawable.mode_done);
        mTtileBar.setOptionMenuVisible(true);
        mTtileBar.setOptionClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (mMessageList != null && mMessageList.size() > 0) {
                    if (mAddPrivacyMessage.size() > 0 && mAddPrivacyMessage != null) {
                        if (!Utilities.isEmpty(mFrom) &&
                                mFrom.equals(CallFilterConstants.FROM_BLACK_LIST)) {
                            //black list process
                            if (mAddPrivacyMessage.size() == 1) {
                                boolean isHaveBlackNum = mCallManger.
                                        isExistBlackList(mAddPrivacyMessage.get(0).getPhoneNumber());
                                if (isHaveBlackNum) {
                                    Message messge = new Message();
                                    messge.what = HAVE_BLACK_LIST;
                                    if (messge != null && mHandler != null) {
                                        mHandler.sendMessage(messge);
                                    }
                                } else {
                                    showProgressDialog(mAddPrivacyMessage.size(), 0);
                                    sendImpMsmHandler(CallFilterConstants.ADD_BLACK_LIST_MODEL);
                                }
                            } else {
                                showProgressDialog(mAddPrivacyMessage.size(), 0);
                                sendImpMsmHandler(CallFilterConstants.ADD_BLACK_LIST_MODEL);
                            }
                        } else {
                            //privacy list process
                            if (mAddPrivacyMessage.size() == 1) {
                                boolean isHaveBlackNum = mCallManger.
                                        isExistBlackList(mAddPrivacyMessage.get(0).getPhoneNumber());
                                if (isHaveBlackNum) {
                                    Message messge = new Message();
                                    messge.what = HAVE_BLACK_LIST;
                                    if (messge != null && mHandler != null) {
                                        mHandler.sendMessage(messge);
                                    }
                                } else {
                                    showProgressDialog(mAddPrivacyMessage.size(), 0);
                                    sendImpMsmHandler(PrivacyContactUtils.ADD_CONTACT_MODEL);
                                }
                            } else {
                                showProgressDialog(mAddPrivacyMessage.size(), 0);
                                sendImpMsmHandler(PrivacyContactUtils.ADD_CONTACT_MODEL);
                            }
                        }
                    } else {
                        Toast.makeText(AddFromMessageListActivity.this,
                                getResources().getString(R.string.privacy_contact_toast_no_choose),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mTtileBar.setToolbarTitle(R.string.privacy_contact_popumenus_from_message);
        mProgressBar = (ProgressBar) findViewById(R.id.progressbar_loading);
        mMessageList = new ArrayList<MessageBean>();
        mAddPrivacyMessage = new ArrayList<MessageBean>();
        mListMessage = (ListView) findViewById(R.id.add_messageLV);
        mListMessage.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListMessage.post(new Runnable() {

            @Override
            public void run() {
                if (mMessageList != null) {
                    for (MessageBean message : mMessageList) {
                        message.setCheck(false);
                    }
                }

            }
        });

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getItemAtPosition(position);
        if (object instanceof MessageBean) {
            MessageBean mb = mMessageList.get(position);
            ImageView image = (ImageView) view.findViewById(R.id.message_item_check_typeIV);
            if (!mb.isCheck()) {
                mAddPrivacyMessage.add(mb);
                mb.getContactIcon();
                image.setImageDrawable(getResources().getDrawable(R.drawable.select));
                mb.setCheck(true);
            } else {
                mAddPrivacyMessage.remove(mb);
                image.setImageDrawable(getResources().getDrawable(R.drawable.unselect));
                mb.setCheck(false);
            }
        }
    }

    private class MyMessageAdapter extends BaseAdapter {
        LayoutInflater relativelayout;
        List<MessageBean> messages;

        public MyMessageAdapter(List<MessageBean> messages) {
            relativelayout = LayoutInflater.from(AddFromMessageListActivity.this);
            this.messages = messages;
        }

        @Override
        public int getCount() {

            return (messages != null) ? messages.size() : 0;
        }

        @Override
        public Object getItem(int position) {

            return messages.get(position);
        }

        @Override
        public long getItemId(int position) {

            return position;
        }

        class ViewHolder {
            TextView name, content, date;
            ImageView checkView;
            CircleImageView contactIcon;
        }

        @SuppressLint("NewApi")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh = null;
            if (convertView == null) {
                vh = new ViewHolder();
                convertView = relativelayout.inflate(R.layout.activity_add_privacy_message_item,
                        null);
                vh.name = (TextView) convertView.findViewById(R.id.message_item_nameTV);
                vh.content = (TextView) convertView.findViewById(R.id.message_item_contentTV);
                vh.date = (TextView) convertView.findViewById(R.id.message_list_dateTV);
                vh.checkView = (ImageView) convertView.findViewById(R.id.message_item_check_typeIV);
                vh.contactIcon = (CircleImageView) convertView.findViewById(R.id.contactIV);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }
            MessageBean mb = messages.get(position);
            if (mb.getMessageName() != null && !"".equals(mb.getMessageName())) {
                vh.name.setText(mb.getMessageName());
            } else {

                vh.name.setText(mb.getPhoneNumber());
            }
            vh.content.setText(mb.getMessageBody());
            SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd");
            Date tempDate = null;
            try {
                if (mb.getMessageTime() != null) {
                    tempDate = sdf.parse(mb.getMessageTime());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (tempDate != null) {
                vh.date.setText(sdf.format(tempDate));
            }
            if (mb.isCheck()) {
                vh.checkView.setImageResource(R.drawable.select);
            } else {
                vh.checkView.setImageResource(R.drawable.unselect);
            }
            Bitmap icon = mb.getContactIcon();
            vh.contactIcon.setImageBitmap(icon);
            return convertView;
        }

    }

    private void showAddMessageDialog(String title, String content) {
        if (mAddMessageDialog == null) {
            mAddMessageDialog = new LEOAlarmDialog(this);
        }
        mAddMessageDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    if (mHandler == null) {
                        mHandler = new Handler() {
                            @Override
                            public void handleMessage(Message msg) {
                                int totalSize = 0;
                                if (mAddCallLogs != null) {
                                    totalSize = mAddCallLogs.size();
                                }
                                if (mAddMessages != null) {
                                    totalSize = totalSize + mAddMessages.size();
                                }
                                int currentValue = msg.what;
                                if (currentValue >= totalSize) {
                                    if (mProgressDialog != null) {
                                        mProgressDialog.cancel();
                                    }
                                    AddFromContactListActivity
                                            .notificationUpdatePrivacyContactList();
                                    AddFromMessageListActivity.this.finish();
                                } else {
                                    mProgressDialog.setProgress(currentValue);
                                }
                                super.handleMessage(msg);
                            }
                        };
                    }
                    int totalSize = 0;
                    if (mAddCallLogs != null) {
                        totalSize = mAddCallLogs.size();
                    }
                    if (mAddMessages != null) {
                        totalSize = totalSize + mAddMessages.size();
                    }
                    showProgressDialog(totalSize, 0);
//                    PrivacyMessageTask task = new PrivacyMessageTask();
//                    task.execute(PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL);
                    sendImpMsmHandler(PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL);
                    SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                            "import");
                } else if (which == 0) {
                    SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                            "unimport");
                    if (mAddMessageDialog != null) {
                        mAddMessageDialog.cancel();
                    }
                    AddFromMessageListActivity.this.finish();
                }

            }
        });
        mAddMessageDialog.setCanceledOnTouchOutside(false);
        mAddMessageDialog.setTitle(title);
        mAddMessageDialog.setContent(content);

        if (isFinishing()) return;

        try {
            mAddMessageDialog.show();
        } catch (Exception e) {

        } catch (Error e) {

        }
    }

    private void sendImpMsmHandler(final String flag) {
        if (mAddFromMsmHandler != null) {
            ThreadManager.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    int isOtherLogs = PrivacyContactUtils.NO_EXIST_LOG;
                    try {
                        int count = 0;
                        ContentResolver cr = getContentResolver();
                        if (PrivacyContactUtils.ADD_CONTACT_MODEL.equals(flag)) {
                            boolean added = false;
                            PrivacyContactManager pcm = PrivacyContactManager
                                    .getInstance(getApplicationContext());
                            for (MessageBean message : mAddPrivacyMessage) {
                                String name = message.getMessageName();
                                String contactNumber = message.getPhoneNumber();
                                String numberFromMessage = PrivacyContactUtils.simpleFromateNumber(contactNumber);
                                /*隐私联系人去重*/
                                boolean flagContact = PrivacyContactUtils.pryContRemovSame(contactNumber);

                                boolean isHaveBlackNum = mCallManger.isExistBlackList(numberFromMessage);

                                if (!flagContact && !isHaveBlackNum) {
                                    ContentValues values = new ContentValues();
                                    values.put(Constants.COLUMN_PHONE_NUMBER, numberFromMessage);
                                    values.put(Constants.COLUMN_CONTACT_NAME, name);
                                    values.put(Constants.COLUMN_PHONE_ANSWER_TYPE, mAnswerType);
                                    cr.insert(Constants.PRIVACY_CONTACT_URI, values);
                                    pcm.addContact(new ContactBean(0, name, numberFromMessage, null, null,
                                            null, false, mAnswerType, null, 0, 0, 0));
                                    added = true;
                                    Context context = AddFromMessageListActivity.this;
                                   /*4.4以上不去做短信操作*/
                                    boolean isLessLeve19 = PrivacyContactUtils.isLessApiLeve19();
                                    if (isLessLeve19) {
                                        if (mAddMessages == null) {
                                            mAddMessages = PrivacyContactManager.getInstance(context).queryMsmsForNumber(contactNumber);
                                        } else {
                                            List<MessageBean> addMessages = PrivacyContactManager.getInstance(context).queryMsmsForNumber(contactNumber);
                                            mAddMessages.addAll(addMessages);
                                        }
                                    }
                                    if (mAddCallLogs == null) {
                                        mAddCallLogs = PrivacyContactManager.getInstance(context).queryCallsForNumber(contactNumber);
                                    } else {
                                        List<ContactCallLog> addCalllog = PrivacyContactManager.getInstance(context).queryCallsForNumber(contactNumber);
                                        mAddCallLogs.addAll(addCalllog);
                                    }
                                    if (isOtherLogs == PrivacyContactUtils.NO_EXIST_LOG) {
                                        if ((mAddMessages != null && mAddMessages.size() != 0)
                                                || (mAddCallLogs != null && mAddCallLogs.size() != 0)) {
                                            isOtherLogs = PrivacyContactUtils.EXIST_LOG;
                                            mLogFlag = true;
                                        }
                                    }
                                } else {
                                    if (mAddPrivacyMessage.size() == 1 && mAddPrivacyMessage != null) {
                                        String evMsg = PrivacyContactUtils.ADD_CONTACT_FROM_CONTACT_NO_REPEAT_EVENT;
                                        PrivacyMessageEvent event = new PrivacyMessageEvent(evMsg);
                                        LeoEventBus.getDefaultBus().post(event);
                                        isOtherLogs = PrivacyContactUtils.NO_EXIST_LOG;
                                        mLogFlag = false;
                                    }
                                }

                                if (mHandler != null) {
                                    Message messge = new Message();
                                    count = count + 1;
                                    messge.what = count;
                                    mHandler.sendMessage(messge);
                                }
                                flagContact = false;
                                if (added) {
                                    String evMsg = PrivacyContactUtils.PRIVACY_ADD_CONTACT_UPDATE;
                                    PrivacyEditFloatEvent event = new PrivacyEditFloatEvent(evMsg);
                                    // 通知更新隐私联系人列表
                                    LeoEventBus.getDefaultBus().post(event);
                                    SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd", "smsadd");
                                }
                            }
                        } else if (CallFilterConstants.ADD_BLACK_LIST_MODEL.equals(flag)) {
                            for (MessageBean message : mAddPrivacyMessage) {
                                List<BlackListInfo> blackList = new ArrayList<BlackListInfo>();
                                String name = message.getMessageName();
                                String contactNumber = message.getPhoneNumber();
                                String number = PrivacyContactUtils.simpleFromateNumber(contactNumber);
                                /*隐私联系人去重*/
                                boolean flagContact = PrivacyContactUtils.pryContRemovSame(contactNumber);
                                if (!flagContact) {
                                    boolean isHaveBlackNum = mCallManger.isExistBlackList(number);
                                    if (!isHaveBlackNum) {
                                        BlackListInfo info = new BlackListInfo();
                                        info.setNumberName(name);
                                        info.setNumber(number);
                                        info.setIcon(message.getContactIcon());
                                        blackList.add(info);
                                    }
                                }
                                Message messge = new Message();
                                count = count + 1;
                                messge.what = count;
                                if (messge != null && mHandler != null) {
                                    mHandler.sendMessage(messge);
                                }
                                mCallManger.addBlackList(blackList, false);
                            }

                        } else if (PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL.equals(flag)) {
                            // 导入短信和通话记录
                            if (mAddMessages != null) {
                                for (MessageBean message : mAddMessages) {
                                    String number = message.getPhoneNumber();
                                    String name = message.getMessageName();
                                    String body = message.getMessageBody();
                                    String time = message.getMessageTime();
                                    String threadId = message.getMessageThreadId();
                                    int isRead = 1;// 0未读，1已读
                                    int type = message.getMessageType();// 短信类型1是接收到的，2是已发出
                                    ContentValues values = new ContentValues();
                                    values.put(Constants.COLUMN_MESSAGE_PHONE_NUMBER, number);
                                    values.put(Constants.COLUMN_MESSAGE_CONTACT_NAME, name);
                                    String bodyTrim = body.trim();
                                    values.put(Constants.COLUMN_MESSAGE_BODY, bodyTrim);
                                    values.put(Constants.COLUMN_MESSAGE_DATE, time);
                                    values.put(Constants.COLUMN_MESSAGE_THREAD_ID, threadId);

                                    int thread = PrivacyContactUtils.queryContactId(
                                            AddFromMessageListActivity.this, message.getPhoneNumber());
                                    values.put(Constants.COLUMN_MESSAGE_THREAD_ID, thread);
                                    values.put(Constants.COLUMN_MESSAGE_TYPE, type);
                                    Uri messageFlag = null;
                                    try {
                                        messageFlag = cr.insert(Constants.PRIVACY_MESSAGE_URI, values);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    PrivacyContactUtils.deleteMessageFromSystemSMS("address = ?",
                                            new String[]{
                                                    number
                                            }, AddFromMessageListActivity.this);
                                    if (messageFlag != null && mHandler != null) {
                                        Message messge = new Message();
                                        count = count + 1;
                                        messge.what = count;
                                        mHandler.sendMessage(messge);
                                    }
                                }
                            }
                            /*导入通话记录*/
                            if (mAddCallLogs != null) {
                                for (ContactCallLog calllog : mAddCallLogs) {
                                    String number = calllog.getCallLogNumber();
                                    String name = calllog.getCallLogName();
                                    String date = calllog.getClallLogDate();
                                    int type = calllog.getClallLogType();
                                    ContentValues values = new ContentValues();
                                    values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER, number);
                                    values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME, name);
                                    values.put(Constants.COLUMN_CALL_LOG_DATE, date);
                                    values.put(Constants.COLUMN_CALL_LOG_TYPE, type);
                                    values.put(Constants.COLUMN_CALL_LOG_IS_READ, 1);
                                    values.put(Constants.COLUMN_CALL_LOG_DURATION, calllog.getCallLogDuraction());
                                    Uri callLogFlag = null;
                                    try {
                                        callLogFlag = cr.insert(Constants.PRIVACY_CALL_LOG_URI, values);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    PrivacyContactUtils.deleteCallLogFromSystem("number LIKE ?", number,
                                            AddFromMessageListActivity.this);
                                    if (callLogFlag != null && mHandler != null) {
                                        Message messge = new Message();
                                        count = count + 1;
                                        messge.what = count;
                                        mHandler.sendMessage(messge);
                                    }
                                }
                                List<String> addNumbers = new ArrayList<String>();
                                for (MessageBean messageBean : mAddPrivacyMessage) {
                                    addNumbers.add(messageBean.getPhoneNumber());
                                }
                            }

                            if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                                LeoEventBus.getDefaultBus().post(
                                        new PrivacyEditFloatEvent(
                                                PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT));

                            }
                            if (mAddMessages != null && mAddMessages.size() != 0) {

                                LeoEventBus.getDefaultBus().post(
                                        new PrivacyEditFloatEvent(
                                                PrivacyContactUtils.UPDATE_MESSAGE_FRAGMENT));
                            }
                            isOtherLogs = PrivacyContactUtils.NO_EXIST_LOG;
                        }
                    } catch (Exception e) {

                    }

                    Message msg = new Message();
                    msg.what = PrivacyContactUtils.MSG_MSM_QU;
                    msg.arg1 = isOtherLogs;
                    mAddFromMsmHandler.sendMessage(msg);
                }
            });
        }
    }

    private class PrivacyMessageTask extends AsyncTask<String, Boolean, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... arg0) {
            boolean isOtherLogs = false;
            try {
                String flag = arg0[0];
                int count = 0;
                ContentResolver cr = getContentResolver();
                if (PrivacyContactUtils.ADD_CONTACT_MODEL.equals(flag)) {
                    boolean added = false;
                    PrivacyContactManager pcm = PrivacyContactManager
                            .getInstance(getApplicationContext());
                    for (MessageBean message : mAddPrivacyMessage) {
                        String name = message.getMessageName();
                        String contactNumber = message.getPhoneNumber();
                        String numberFromMessage = PrivacyContactUtils.simpleFromateNumber(contactNumber);
                        // 隐私联系人去重
                        String tempNumber =
                                PrivacyContactUtils.formatePhoneNumber(numberFromMessage);
                        boolean flagContact = PrivacyContactUtils.pryContRemovSame(contactNumber);
                        if (!flagContact) {
                            ContentValues values = new ContentValues();
                            values.put(Constants.COLUMN_PHONE_NUMBER, numberFromMessage);
                            values.put(Constants.COLUMN_CONTACT_NAME, name);
                            values.put(Constants.COLUMN_PHONE_ANSWER_TYPE, mAnswerType);
                            cr.insert(Constants.PRIVACY_CONTACT_URI, values);
                            pcm.addContact(new ContactBean(0, name, numberFromMessage, null, null,
                                    null, false, mAnswerType, null, 0, 0, 0));
                            added = true;
                        }
                        Context context = AddFromMessageListActivity.this;
                        if (mAddMessages == null) {
                            mAddMessages = PrivacyContactManager.getInstance(context).queryMsmsForNumber(contactNumber);
                        } else {
                            List<MessageBean> addMessages = PrivacyContactManager.getInstance(context).queryMsmsForNumber(contactNumber);
                            mAddMessages.addAll(addMessages);
                        }
                        if (mAddCallLogs == null) {
                            mAddCallLogs = PrivacyContactManager.getInstance(context).queryCallsForNumber(contactNumber);
                        } else {
                            List<ContactCallLog> addCalllog = PrivacyContactManager.getInstance(context).queryCallsForNumber(contactNumber);
                            mAddCallLogs.addAll(addCalllog);
                        }
                        if (!isOtherLogs) {
                            if ((mAddMessages != null && mAddMessages.size() != 0)
                                    || (mAddCallLogs != null && mAddCallLogs.size() != 0)) {
                                isOtherLogs = true;
                            }
                        }
                        if (flagContact) {
                            if (mAddPrivacyMessage.size() == 1 && mAddPrivacyMessage != null) {
                                LeoEventBus
                                        .getDefaultBus()
                                        .post(
                                                new PrivacyMessageEvent(
                                                        PrivacyContactUtils.ADD_CONTACT_FROM_CONTACT_NO_REPEAT_EVENT));
                                isOtherLogs = false;
                                mLogFlag = isOtherLogs;
                            }
                        }
                        mLogFlag = true;
                        if (mHandler != null) {
                            Message messge = new Message();
                            count = count + 1;
                            messge.what = count;
                            mHandler.sendMessage(messge);
                        }
                        flagContact = false;
                        if (added) {
                            // 通知更新隐私联系人列表
                            LeoEventBus
                                    .getDefaultBus()
                                    .post(new PrivacyEditFloatEvent(
                                            PrivacyContactUtils.PRIVACY_ADD_CONTACT_UPDATE));
//                            PrivacyHelper.getInstance(getApplicationContext()).computePrivacyLevel(
//                                    PrivacyHelper.VARABLE_PRIVACY_CONTACT);
                            SDKWrapper.addEvent(getApplicationContext(), SDKWrapper.P1, "contactsadd",
                                    "smsadd");
                        }
                    }
                } else if (PrivacyContactUtils.ADD_CALL_LOG_AND_MESSAGE_MODEL.equals(flag)) {
                    // 导入短信和通话记录
                    if (mAddMessages != null) {
                        for (MessageBean message : mAddMessages) {
                            String number = message.getPhoneNumber();
                            String name = message.getMessageName();
                            String body = message.getMessageBody();
                            String time = message.getMessageTime();
                            String threadId = message.getMessageThreadId();
                            int isRead = 1;// 0未读，1已读
                            int type = message.getMessageType();// 短信类型1是接收到的，2是已发出
                            ContentValues values = new ContentValues();
                            values.put(Constants.COLUMN_MESSAGE_PHONE_NUMBER, number);
                            values.put(Constants.COLUMN_MESSAGE_CONTACT_NAME, name);
                            String bodyTrim = body.trim();
                            values.put(Constants.COLUMN_MESSAGE_BODY, bodyTrim);
                            values.put(Constants.COLUMN_MESSAGE_DATE, time);
                            values.put(Constants.COLUMN_MESSAGE_THREAD_ID, threadId);

                            int thread = PrivacyContactUtils.queryContactId(
                                    AddFromMessageListActivity.this, message.getPhoneNumber());
                            values.put(Constants.COLUMN_MESSAGE_THREAD_ID, thread);
                            values.put(Constants.COLUMN_MESSAGE_TYPE, type);
                            Uri messageFlag = null;
                            try {
                                messageFlag = cr.insert(Constants.PRIVACY_MESSAGE_URI, values);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            PrivacyContactUtils.deleteMessageFromSystemSMS("address = ?",
                                    new String[]{
                                            number
                                    }, AddFromMessageListActivity.this);
                            if (messageFlag != null && mHandler != null) {
                                Message messge = new Message();
                                count = count + 1;
                                messge.what = count;
                                mHandler.sendMessage(messge);
                            }
                        }
                        // 更新联系人隐私短信
                        // for (MessageBean messageBean : mAddPrivacyMessage) {
                        // pm.removeSysMessage(messageBean);
                        // }
                    }
                    // 导入通话记录
                    if (mAddCallLogs != null) {
                        for (ContactCallLog calllog : mAddCallLogs) {
                            String number = calllog.getCallLogNumber();
                            String name = calllog.getCallLogName();
                            String date = calllog.getClallLogDate();
                            int type = calllog.getClallLogType();
                            ContentValues values = new ContentValues();
                            values.put(Constants.COLUMN_CALL_LOG_PHONE_NUMBER, number);
                            values.put(Constants.COLUMN_CALL_LOG_CONTACT_NAME, name);
                            values.put(Constants.COLUMN_CALL_LOG_DATE, date);
                            values.put(Constants.COLUMN_CALL_LOG_TYPE, type);
                            values.put(Constants.COLUMN_CALL_LOG_IS_READ, 1);
                            values.put(Constants.COLUMN_CALL_LOG_DURATION, calllog.getCallLogDuraction());
                            Uri callLogFlag = null;
                            try {
                                callLogFlag = cr.insert(Constants.PRIVACY_CALL_LOG_URI, values);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            PrivacyContactUtils.deleteCallLogFromSystem("number LIKE ?", number,
                                    AddFromMessageListActivity.this);
                            if (callLogFlag != null && mHandler != null) {
                                Message messge = new Message();
                                count = count + 1;
                                messge.what = count;
                                mHandler.sendMessage(messge);
                            }
                        }
                        List<String> addNumbers = new ArrayList<String>();
                        for (MessageBean messageBean : mAddPrivacyMessage) {
                            addNumbers.add(messageBean.getPhoneNumber());
                        }
                    }

                    if (mAddCallLogs != null && mAddCallLogs.size() != 0) {
                        LeoEventBus.getDefaultBus().post(
                                new PrivacyEditFloatEvent(
                                        PrivacyContactUtils.UPDATE_CALL_LOG_FRAGMENT));

                    }
                    if (mAddMessages != null && mAddMessages.size() != 0) {

                        LeoEventBus.getDefaultBus().post(
                                new PrivacyEditFloatEvent(
                                        PrivacyContactUtils.UPDATE_MESSAGE_FRAGMENT));
                    }
                    isOtherLogs = false;
                }
            } catch (Exception e) {

            }

            return isOtherLogs;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            try {
                if (result) {
                    String title = getResources().getString(
                            R.string.privacy_contact_add_log_dialog_title);
                    String content = getResources().getString(
                            R.string.privacy_contact_add_log_dialog_dialog_content);
                    showAddMessageDialog(title, content);
                } else {
                    AddFromContactListActivity.notificationUpdatePrivacyContactList();
                }
            } catch (Exception e) {

            }
            if (mProgressDialog != null) {
                mProgressDialog.cancel();
            }
            mHandler = null;
            super.onPostExecute(result);
        }
    }

    private void showProgressDialog(int maxValue, int currentValue) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEORoundProgressDialog(this);
        }
        String title = getResources().getString(R.string.privacy_message_progress_dialog_title);
        String content = getResources().getString(R.string.privacy_message_progress_dialog_content);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(content);
        mProgressDialog.setMax(maxValue);
        mProgressDialog.setProgress(currentValue);
        mProgressDialog.setButtonVisiable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        try {
            mProgressDialog.show();
        } catch (Exception e) {

        }
    }

    private class AddFromMsmHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case PrivacyContactUtils.MSG_ADD_MSM:
                    if (msg.obj != null) {
                        LeoLog.i(TAG, "load  messages list finish !");
                        List<MessageBean> msms = (List<MessageBean>) msg.obj;
                        if (mMessageList != null) {
                            mMessageList.clear();
                        }
                        mMessageList = msms;
                        if (mMessageList != null && mMessageList.size() > 0) {
                            mDefaultText.setVisibility(View.GONE);
                            mTtileBar.setOptionImageVisibilit(true);
                        } else {
                            mDefaultText.setVisibility(View.VISIBLE);
                            mTtileBar.setOptionImageVisibilit(false);
                        }
                        mProgressBar.setVisibility(View.GONE);
                        mAdapter = new MyMessageAdapter(mMessageList);
                        mListMessage.setAdapter(mAdapter);
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case PrivacyContactUtils.MSG_MSM_QU:
                    try {
                        if (msg.arg1 == PrivacyContactUtils.EXIST_LOG) {
                            String title = getResources().getString(
                                    R.string.privacy_contact_add_log_dialog_title);
                            String content = getResources().getString(
                                    R.string.privacy_contact_add_log_dialog_dialog_content);
                            showAddMessageDialog(title, content);
                        } else {
                            AddFromContactListActivity.notificationUpdatePrivacyContactList();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (mProgressDialog != null) {
                        mProgressDialog.cancel();
                    }
                    mHandler = null;
                    break;
                default:
                    break;
            }
        }
    }

    private void sendMsgHandler() {
        if (mAddFromMsmHandler != null) {
            ThreadManager.executeOnAsyncThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBar.setVisibility(View.VISIBLE);
                    List<MessageBean> messageList =
                            PrivacyContactUtils.getSysMessage(AddFromMessageListActivity.this, null, null, false, false);
                    if (messageList != null && messageList.size() > 0) {
                        Collections.sort(messageList, PrivacyContactUtils.mMessageCamparator);
                    }

                    Message msg = new Message();
                    msg.what = PrivacyContactUtils.MSG_ADD_MSM;
                    List<MessageBean> newMessageList = new ArrayList<MessageBean>();
                    //blacklist remove 隐私联系人
                    if (!Utilities.isEmpty(mFrom) &&
                            mFrom.equals(CallFilterConstants.FROM_BLACK_LIST)) {
                        for (int i = 0; i < messageList.size(); i++) {
                            MessageBean bean = messageList.get(i);
                            if (!mCallManger.isPrivacyConUse(bean.getPhoneNumber())) {
                                newMessageList.add(bean);
                            }
                        }
                        msg.obj = newMessageList;
                    } else {
                        msg.obj = messageList;
                    }
                    mAddFromMsmHandler.sendMessage(msg);
                }
            });
        }
    }
}
