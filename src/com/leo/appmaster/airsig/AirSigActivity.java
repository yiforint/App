package com.leo.appmaster.airsig;


import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airsig.airsigengmulti.ASEngine;
import com.leo.appmaster.R;
import com.leo.appmaster.airsig.airsigsdk.ASGui;
import com.leo.appmaster.db.LeoSettings;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonToolbar;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;


public class AirSigActivity extends BaseActivity implements View.OnClickListener {
    public final static String AIRSIG_SWITCH = "airsig_switch";
    public final static String AIRSIG_OPEN_EVER = "airsig_open_ever";
    public final static String AIRSIG_TIMEOUT_EVER = "airsig_timeout_ever";
    private final static int SET_DONE = 1;
    private CommonToolbar mTitleBar;
    private RippleView rpBtn;
    private RippleView rpBtnTwo;

    private TextView mTvSetOne;
    private TextView mTvSetTwo;

    private LEOAlarmDialog mConfirmCloseDialog;

    private android.os.Handler mHandler = new android.os.Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SET_DONE:
                    LeoSettings.setBoolean(AIRSIG_SWITCH, true);
                    LeoSettings.setInteger(AirSigSettingActivity.UNLOCK_TYPE, AirSigSettingActivity.AIRSIG_UNLOCK);
                    switchOn();
                    showMessage(getString(R.string.airsig_settings_activity_toast));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.airsig_activity_select);

        initUI();
    }


    private void initUI() {
        mTitleBar = (CommonToolbar) findViewById(R.id.ctb_main);
        mTitleBar.setToolbarTitle(R.string.airsig_settings_activity_title);
        mTitleBar.setOptionMenuVisible(false);
        mTitleBar.setNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        rpBtn = (RippleView) findViewById(R.id.rv_item_open_airsig);
        rpBtn.setOnClickListener(this);
        rpBtnTwo = (RippleView) findViewById(R.id.rv_item_reset_airsig);
        rpBtnTwo.setOnClickListener(this);
        mTvSetOne = (TextView) findViewById(R.id.tv_title_airsig);
        mTvSetTwo = (TextView) findViewById(R.id.tv_title_reset_airsig);
    }


    @Override
    protected void onResume() {
        super.onResume();
        fillData();
    }

    private void fillData() {
        boolean isAirsigOn = LeoSettings.getBoolean(AIRSIG_SWITCH, false);
        boolean isAirSigVaild = ASGui.getSharedInstance().isValidLicense();
        if (isAirsigOn) {
            if (isAirSigVaild) {
                switchOn();
            } else {
                switchOff();
            }
        } else {
            switchOff();
        }
    }

    private void switchOff() {
        mTvSetOne.setText(getString(R.string.airsig_settings_activity_set_one_on));
        mTvSetTwo.setTextColor(getResources().getColor(R.color.cgy));
        rpBtnTwo.setFocusable(false);
        rpBtnTwo.setEnabled(false);
    }

    private void switchOn() {
        mTvSetOne.setText(getString(R.string.airsig_settings_activity_set_one_off));
        mTvSetTwo.setTextColor(getResources().getColor(R.color.c2));
        rpBtnTwo.setFocusable(true);
        rpBtnTwo.setEnabled(true);
    }

    private void showMessage(final String message) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rv_item_open_airsig:
                switchAirsig();
                break;
            case R.id.rv_item_reset_airsig:
                SDKWrapper.addEvent(this, SDKWrapper.P1, "settings", "airsig_reset");
                setAirsig(false);
                break;
        }
    }

    private void setAirsig(final boolean isNormalSet) {

        boolean isAirSigVaild = ASGui.getSharedInstance().isValidLicense();

        if (!isAirSigVaild) {
            return;
        }

        ASGui.getSharedInstance().showTrainingActivity(1, new ASGui.OnTrainingResultListener() {
            @Override
            public void onResult(boolean isRetrain, boolean success, ASEngine.ASAction action) {
                if (success) {

                    if (isNormalSet) {
                        SDKWrapper.addEvent(AirSigActivity.this, SDKWrapper.P1, "settings", "airsig_enable_suc");
                    } else {
                        SDKWrapper.addEvent(AirSigActivity.this, SDKWrapper.P1, "settings", "airsig_reset_suc");
                    }

                    mHandler.sendEmptyMessage(SET_DONE);
                }
            }
        });
    }

    private void switchAirsig() {
        boolean isAirsigOn = LeoSettings.getBoolean(AIRSIG_SWITCH, false);
        boolean isAirsigReady = ASGui.getSharedInstance().isSignatureReady(1);
        boolean isAirSigVaild = ASGui.getSharedInstance().isValidLicense();

        if (!isAirSigVaild) {
            showUpdateDialog();
            return;
        }

        if (isAirsigOn) {
            SDKWrapper.addEvent(AirSigActivity.this, SDKWrapper.P1, "settings", "airsig_off");
            //dialog to close
            if (mConfirmCloseDialog == null) {
                mConfirmCloseDialog = new LEOAlarmDialog(this);
            }
            mConfirmCloseDialog.setContent(getString(R.string.airsig_settings_activity_dialog));
            mConfirmCloseDialog.setRightBtnStr(getString(R.string.close_batteryview_confirm_sure));
            mConfirmCloseDialog.setLeftBtnStr(getString(R.string.close_batteryview_confirm_cancel));
            mConfirmCloseDialog.setRightBtnListener(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SDKWrapper.addEvent(AirSigActivity.this, SDKWrapper.P1, "settings", "airsig_off_suc");
                    LeoSettings.setBoolean(AIRSIG_SWITCH, false);
                    switchOff();
                    mConfirmCloseDialog.dismiss();
                }
            });
            if (!isFinishing()) {
                mConfirmCloseDialog.show();
            }
        } else if (isAirsigReady) {
            SDKWrapper.addEvent(this, SDKWrapper.P1, "settings", "airsig_enable");
            //open
            LeoSettings.setInteger(AirSigSettingActivity.UNLOCK_TYPE, AirSigSettingActivity.AIRSIG_UNLOCK);
            LeoSettings.setBoolean(AIRSIG_SWITCH, true);
            switchOn();
            SDKWrapper.addEvent(AirSigActivity.this, SDKWrapper.P1, "settings", "airsig_enable_suc");
        } else {
            SDKWrapper.addEvent(this, SDKWrapper.P1, "settings", "airsig_enable");
            //set Airsig
            setAirsig(true);
        }
    }

    private void showUpdateDialog() {
        if (mConfirmCloseDialog == null) {
            mConfirmCloseDialog = new LEOAlarmDialog(this);
        }
        mConfirmCloseDialog.setContent(getString(R.string.airsig_tip_toast_update_text));
        mConfirmCloseDialog.setRightBtnStr(getString(R.string.makesure));
        mConfirmCloseDialog.setLeftBtnStr(getString(R.string.close_batteryview_confirm_cancel));
        mConfirmCloseDialog.setRightBtnListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SDKWrapper.checkUpdate();
                mConfirmCloseDialog.dismiss();
            }
        });
        if (!isFinishing()) {
            mConfirmCloseDialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConfirmCloseDialog != null && mConfirmCloseDialog.isShowing()) {
            mConfirmCloseDialog.dismiss();
            mConfirmCloseDialog = null;
        }
    }
}
