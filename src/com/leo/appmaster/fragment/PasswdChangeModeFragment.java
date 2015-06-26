
package com.leo.appmaster.fragment;

import java.util.List;

import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.animation.AnimationListenerAdapter;
import com.leo.appmaster.applocker.AppLockListActivity;
import com.leo.appmaster.applocker.LockScreenActivity;
import com.leo.appmaster.applocker.LockChangeModeActivity;
import com.leo.appmaster.applocker.PasswdProtectActivity;
import com.leo.appmaster.applocker.manager.LockManager;
import com.leo.appmaster.applocker.model.LockMode;
import com.leo.appmaster.home.HomeActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOMessageDialog;

public class PasswdChangeModeFragment extends BaseFragment implements
        OnDismissListener, OnDiaogClickListener, OnClickListener {

    private TextView tv1, tv2, tv3, tv4, tv5, tv6, tv7, tv8, tv9, tv0;

    private ImageView iv_delete;
    private TextView mTvPasswd1, mTvPasswd2, mTvPasswd3, mTvPasswd4;
    private TextView mInputTip, mTvPasswdFuncTip, mTvBottom, mSwitchBottom;

    private int mInputCount = 1;
    private String mTempFirstPasswd = "";
    private String mTempSecondPasswd = "";

    private boolean mGotoPasswdProtect;
    private Animation mShake;

    @Override
    protected int layoutResourceId() {
        return R.layout.fragment_passwd_setting;
    }

    @Override
    protected void onInitUI() {
        tv1 = (TextView) findViewById(R.id.tv_1);
        tv2 = (TextView) findViewById(R.id.tv_2);
        tv3 = (TextView) findViewById(R.id.tv_3);
        tv4 = (TextView) findViewById(R.id.tv_4);
        tv5 = (TextView) findViewById(R.id.tv_5);
        tv6 = (TextView) findViewById(R.id.tv_6);
        tv7 = (TextView) findViewById(R.id.tv_7);
        tv8 = (TextView) findViewById(R.id.tv_8);
        tv9 = (TextView) findViewById(R.id.tv_9);
        tv0 = (TextView) findViewById(R.id.tv_0);
        iv_delete = (ImageView) findViewById(R.id.tv_delete);

        tv1.setOnClickListener(this);
        tv2.setOnClickListener(this);
        tv3.setOnClickListener(this);
        tv4.setOnClickListener(this);
        tv5.setOnClickListener(this);
        tv6.setOnClickListener(this);
        tv7.setOnClickListener(this);
        tv8.setOnClickListener(this);
        tv9.setOnClickListener(this);
        tv0.setOnClickListener(this);
        iv_delete.setOnClickListener(this);
        iv_delete.setEnabled(false);

        mTvPasswd1 = (TextView) findViewById(R.id.tv_passwd_1);
        mTvPasswd2 = (TextView) findViewById(R.id.tv_passwd_2);
        mTvPasswd3 = (TextView) findViewById(R.id.tv_passwd_3);
        mTvPasswd4 = (TextView) findViewById(R.id.tv_passwd_4);

        mInputTip = (TextView) findViewById(R.id.tv_passwd_input_tip);
        mTvPasswdFuncTip = (TextView) findViewById(R.id.tv_passwd_function_tip);

        mTvBottom = (TextView) findViewById(R.id.tv_bottom);
        mTvBottom.setOnClickListener(this);
        mSwitchBottom = (TextView) mActivity.findViewById(R.id.switch_bottom);

        if (AppMasterPreference.getInstance(mActivity).getLockType() == AppMasterPreference.LOCK_TYPE_NONE) {
            mInputTip.setText(R.string.first_set_passwd_hint);
            mTvPasswdFuncTip.setText(R.string.digital_passwd_function_hint);
        } else {
            mInputTip.setText(R.string.set_passwd);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_1:
                inputPasswd(1 + "");
                break;
            case R.id.tv_2:
                inputPasswd(2 + "");
                break;
            case R.id.tv_3:
                inputPasswd(3 + "");
                break;
            case R.id.tv_4:
                inputPasswd(4 + "");
                break;
            case R.id.tv_5:
                inputPasswd(5 + "");
                break;
            case R.id.tv_6:
                inputPasswd(6 + "");
                break;
            case R.id.tv_7:
                inputPasswd(7 + "");
                break;
            case R.id.tv_8:
                inputPasswd(8 + "");
                break;
            case R.id.tv_9:
                inputPasswd(9 + "");
                break;
            case R.id.tv_0:
                inputPasswd(0 + "");
                break;
            case R.id.tv_delete:
                deletePasswd();
                break;
            case R.id.tv_ok:
                makesurePasswd();
                break;
            case R.id.tv_bottom:
                resetPasswd();
                break;

            default:
                break;
        }
    }

    private void shakeGestureTip() {
        if (mShake == null) {
            mShake = AnimationUtils.loadAnimation(mActivity,
                    R.anim.left_right_shake);
            mShake.setAnimationListener(new AnimationListenerAdapter() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mTempSecondPasswd = "";
                    mTvPasswdFuncTip.setText(R.string.tip_no_the_same_pswd);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    clearPasswd();
                }

            });
        }
        mTvPasswdFuncTip.startAnimation(mShake);
    }

    private void cancelShake() {
        if (mShake != null && mShake.hasStarted()) {
            mShake.cancel();
            mShake.reset();
        }
    }

    private void resetPasswd() {
        cancelShake();
        clearPasswd();
        mInputCount = 1;
        mTempFirstPasswd = "";
        mTempSecondPasswd = "";
        if (AppMasterPreference.getInstance(mActivity).getLockType() == AppMasterPreference.LOCK_TYPE_NONE) {
            mInputTip.setText(R.string.first_set_passwd_hint);
        } else {
            mInputTip.setText(R.string.set_passwd);
        }
        mTvPasswdFuncTip.setText(R.string.digital_passwd_function_hint);
        mSwitchBottom.setVisibility(View.VISIBLE);
        mTvBottom.setVisibility(View.INVISIBLE);
    }

    private void makesurePasswd() {
        mTvPasswd1.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mInputCount == 1) {
                    mInputCount++;
                    clearPasswd();
                    mInputTip.setText(R.string.make_passwd);
                    mTvPasswdFuncTip.setText(R.string.input_again);
                    iv_delete.setEnabled(false);
                    mSwitchBottom.setVisibility(View.INVISIBLE);
                    mTvBottom.setVisibility(View.VISIBLE);
                } else if (mInputCount == 2) {
                    AppMasterPreference pref = AppMasterPreference.getInstance(mActivity);
                    if (mTempFirstPasswd.equals(mTempSecondPasswd)) {
                        Toast.makeText(mActivity, R.string.set_passwd_suc, 1)
                                .show();
                        if (pref.getLockType() == AppMasterPreference.LOCK_TYPE_NONE) {
                            SDKWrapper.addEvent(PasswdChangeModeFragment.this.mActivity,
                                    SDKWrapper.P1,
                                    "first", "usepwd");
                        }

                        pref.savePassword(mTempFirstPasswd);

                        if (((LockChangeModeActivity) mActivity).isResetPasswd()) {
                            ((LockChangeModeActivity) mActivity).mJustFinish = true;
                            showResetSuc();
                            // notify lock theme change, this is a camouflage
                            // notify to change lock screen UI
                            Intent intent2 = new Intent(LockScreenActivity.THEME_CHANGE);
                            PasswdChangeModeFragment.this.mActivity.sendBroadcast(intent2);
                            return;
                        }

                        LockManager.getInstatnce().timeFilter(mActivity.getPackageName(), 500);

                        if (!pref.hasPswdProtect()) {
                            setPasswdProtect();
                        } else {
                            mActivity.finish();
                        }
                    } else {
                        shakeGestureTip();
                    }
                }
            }
        }, 200);

    }

    private void showResetSuc() {
        LEOMessageDialog d = new LEOMessageDialog(mActivity);
        d.setTitle(mActivity.getString(R.string.reset_passwd));
        d.setContent(mActivity.getString(R.string.reset_passwd_successful));
        d.setOnDismissListener(this);
        d.setCanceledOnTouchOutside(false);
        d.show();
    }

    private void clearPasswd() {
        mTvPasswd1.setText("");
        mTvPasswd2.setText("");
        mTvPasswd3.setText("");
        mTvPasswd4.setText("");
    }

    private void setPasswdProtect() {
        LEOAlarmDialog d = new LEOAlarmDialog(mActivity);
        d.setTitle(mActivity.getString(R.string.set_protect_or_not));
        d.setContent(mActivity.getString(R.string.set_protect_message));
        d.setLeftBtnStr(mActivity.getString(R.string.cancel));
        d.setRightBtnStr(mActivity.getString(R.string.makesure));
        d.setOnClickListener(this);
        d.setOnDismissListener(this);
        d.show();
    }

    private void deletePasswd() {
        if (!mTvPasswd4.getText().equals("")) {
            mTvPasswd4.setText("");
        } else if (!mTvPasswd3.getText().equals("")) {
            mTvPasswd3.setText("");
        } else if (!mTvPasswd2.getText().equals("")) {
            mTvPasswd2.setText("");
        } else if (!mTvPasswd1.getText().equals("")) {
            mTvPasswd1.setText("");
            iv_delete.setEnabled(false);
        }

        if (mInputCount == 1) {
            if (mTempFirstPasswd.length() > 0) {
                mTempFirstPasswd = mTempFirstPasswd.substring(0,
                        mTempFirstPasswd.length() - 1);
            }
        } else {
            if (mTempSecondPasswd.length() > 0) {
                mTempSecondPasswd = mTempSecondPasswd.substring(0,
                        mTempSecondPasswd.length() - 1);
            }
        }

    }

    private void inputPasswd(String s) {
        if (mTvPasswd1.getText().toString().equals("")) {
            mTvPasswd1.setText("*");
            iv_delete.setEnabled(true);
            if (mInputCount == 1) {
                mTempFirstPasswd = mTempFirstPasswd + s;
            } else {
                mTempSecondPasswd = mTempSecondPasswd + s;
            }
        } else if (mTvPasswd2.getText().toString().equals("")) {
            mTvPasswd2.setText("*");
            if (mInputCount == 1) {
                mTempFirstPasswd = mTempFirstPasswd + s;
            } else {
                mTempSecondPasswd = mTempSecondPasswd + s;
            }
        } else if (mTvPasswd3.getText().toString().equals("")) {
            mTvPasswd3.setText("*");
            if (mInputCount == 1) {
                mTempFirstPasswd = mTempFirstPasswd + s;
            } else {
                mTempSecondPasswd = mTempSecondPasswd + s;
            }
        } else if (mTvPasswd4.getText().toString().equals("")) {
            mTvPasswd4.setText("*");
            if (mInputCount == 1) {
                mTempFirstPasswd = mTempFirstPasswd + s;
            } else {
                mTempSecondPasswd = mTempSecondPasswd + s;
            }
            makesurePasswd();
        }

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Intent intent;

        if (mGotoPasswdProtect) {
            if (((LockChangeModeActivity) mActivity).mToLockList) {
                intent = new Intent(mActivity, PasswdProtectActivity.class);
                intent.putExtra("to_lock_list", true);
                mActivity.startActivity(intent);
            } else if (((LockChangeModeActivity) mActivity).mJustFinish) {
                // todo nothing
                if (((LockChangeModeActivity) mActivity).mFromQuickMode) {
                    intent = new Intent(mActivity, PasswdProtectActivity.class);

                    int modeId = ((LockChangeModeActivity) mActivity).mModeId;
                    intent.putExtra("quick_mode", true);
                    intent.putExtra("mode_id", modeId);
                    mActivity.startActivity(intent);
                }
            } else {
                intent = new Intent(mActivity, PasswdProtectActivity.class);
                intent.putExtra("to_home", true);
                mActivity.startActivity(intent);
                SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "first", "setpwdp");
            }
        } else {
            SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "first", "setpwdp_cancel");
            if (((LockChangeModeActivity) mActivity).mToLockList) {
                // to lock list
                intent = new Intent(mActivity, AppLockListActivity.class);
                startActivity(intent);
            } else if (((LockChangeModeActivity) mActivity).mJustFinish) {
                // do nothing
                /*if (((LockChangeModeActivity) mActivity).mFromQuickMode) {
                    int modeId = ((LockChangeModeActivity) mActivity).mModeId;
                    LockManager lm = LockManager.getInstatnce();
                    List<LockMode> modeList = lm.getLockMode();
                    for (LockMode lockMode : modeList) {
                        if (lockMode.modeId == modeId) {
                            showModeActiveTip(lockMode);
                            break;
                        }
                    }
                }*/
            } else {
                intent = new Intent(mActivity, HomeActivity.class);
                mActivity.startActivity(intent);
            }
        }

        iv_delete.postDelayed(new Runnable() {
            @Override
            public void run() {
                LockManager.getInstatnce().startLockService();
            }
        }, 2000);

        mActivity.finish();
    }

    /**
     * show the tip when mode success activating
     */
  /*  private void showModeActiveTip(LockMode mode) {
        View mTipView = LayoutInflater.from(mActivity).inflate(R.layout.lock_mode_active_tip, null);
        TextView mActiveText = (TextView) mTipView.findViewById(R.id.active_text);
        mActiveText.setText(this.getString(R.string.mode_change, mode.modeName));
        Toast toast = new Toast(mActivity);
        toast.setView(mTipView);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }*/

    @Override
    public void onClick(int which) {
        if (which == 0) {
        } else if (which == 1) {
            mGotoPasswdProtect = true;
        }
    }
}
