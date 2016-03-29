package com.leo.appmaster.home;

import android.widget.LinearLayout;

import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.LeoPreference;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.tools.animator.Animator;
import com.leo.tools.animator.ObjectAnimator;
import com.leo.tools.animator.ValueAnimator;

/**
 * Created by Jasper on 2015/11/20.
 */
public class HomeScanningController {
    private static final String TAG = "HomeScanningController";

    private HomeScanningFragment mFragment;
    private HomeActivity mActivity;

    private LinearLayout mNewAppLayout;
    private LinearLayout mNewPicLayout;
    private LinearLayout mNewVidLayout;
    private LinearLayout mNewLostLayout;
    private LinearLayout mNewWifiLayout;
    private LinearLayout mNewInstructLayout;
    private LinearLayout mNewContactLayout;
    private ObjectAnimator mNewAppAnim;
    private ObjectAnimator mNewPicAnim;
    private ObjectAnimator mNewVidAnim;
    private ObjectAnimator mNewLostAnim;
    private ObjectAnimator mNewWifiAnim;
    private ObjectAnimator mNewInstructAnim;
    private ObjectAnimator mNewContactAnim;

    private static final int START_TIME = 40;
    private static final int NEW_PER_LOST = 55;
    private static final int NEW_PER_VID = 65;
    private static final int NEW_PER_PIC = 85;
    private static final int NEW_PER_APP = 95;
    private static final int NEW_PER_PRI = 100;

    private static final int FIRST_IN_TIME = 3500;
    private static final int NEW_UP_LIMIT_APP = 6000;
    private static final int NEW_UP_LIMIT_PIC = 20000;
    private static final int NEW_UP_LIMIT_PIC_PROCESSED = 15000;
    private static final int NEW_UP_LIMIT_VID = 2000;
    private static final int NEW_UP_LIMIT_LOST = 1000;

    private int mDurationPrivacy;

    private boolean mIsInsValiable = true;


    public HomeScanningController(HomeActivity activity, HomeScanningFragment fragment,
                                  LinearLayout newAppLayout, LinearLayout newPicLayout,
                                  LinearLayout newVidLayout, LinearLayout newLostLayout,
                                  LinearLayout newWifiLayout, LinearLayout newInstructLayout,
                                  LinearLayout newContactLayout, boolean isInsValiable) {

        mActivity = activity;
        mFragment = fragment;

        mNewAppLayout = newAppLayout;
        mNewPicLayout = newPicLayout;
        mNewVidLayout = newVidLayout;
        mNewLostLayout = newLostLayout;
        mNewWifiLayout = newWifiLayout;
        mNewInstructLayout = newInstructLayout;
        mNewContactLayout = newContactLayout;
        mIsInsValiable = isInsValiable;
    }

    public void startScanning() {
        LeoLog.d(TAG, "startScanning...");
        mFragment.startAnimator(mNewContactLayout);
        mActivity.scanningFromPercent(FIRST_IN_TIME, 0, START_TIME);
    }

    public void startItemScanning() {
//        start from app
        mNewContactAnim = getItemAnimation(mNewContactLayout);
        mNewContactAnim.start();
    }


    private ObjectAnimator getItemAnimation(final LinearLayout layout) {
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(this, "scaleX", 1f, 1f);

        if (layout == mNewContactLayout || layout == mNewInstructLayout || layout == mNewWifiLayout) {
            alphaAnim.setDuration(200);
        } else if (layout == mNewPicLayout) {
            alphaAnim.setDuration(500);
        }

        alphaAnim.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnim.addListener(new SimpleAnimatorListener() {

            @Override
            public void onAnimationRepeat(Animator animation) {
                onItemAnimRepeat(animation);
            }
        });

        return alphaAnim;
    }

    private void onItemAnimRepeat(final Animator animation) {
        if (mFragment.isRemoving() || mFragment.isDetached()) return;

        if (animation == mNewAppAnim) {
            if (mFragment.isItemScanFinish(mNewAppLayout)) {
                onItemAnimationEnd(mNewAppAnim);
            }
        } else if (animation == mNewPicAnim) {
            if (mFragment.isItemScanFinish(mNewPicLayout)) {
                onItemAnimationEnd(mNewPicAnim);
            }
        } else if (animation == mNewVidAnim) {
            if (mFragment.isItemScanFinish(mNewVidLayout)) {
                onItemAnimationEnd(mNewVidAnim);
            }
        } else if (animation == mNewLostAnim) {
            onItemAnimationEnd(mNewLostAnim);
        } else if (animation == mNewWifiAnim) {
            onItemAnimationEnd(mNewWifiAnim);
        } else if (animation == mNewInstructAnim) {
            onItemAnimationEnd(mNewInstructAnim);
        } else if (animation == mNewContactAnim) {
            onItemAnimationEnd(mNewContactAnim);
        }
    }

    public void onItemAnimationEnd(final Animator animation) {
        if (mFragment.isRemoving() || mFragment.isDetached()) return;

        ThreadManager.getUiThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                animation.end();
                animation.cancel();
                LeoLog.d(TAG, "onRepeat End...");
            }
        });

        if (animation == mNewContactAnim) {
            mFragment.OnItemAnimationEnd(mNewContactLayout);
            mNewWifiAnim = getItemAnimation(mNewWifiLayout);

            mNewWifiAnim.start();
        } else if (animation == mNewInstructAnim) {
            mFragment.OnItemAnimationEnd(mNewInstructLayout);

            mNewLostAnim = getItemAnimation(mNewLostLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(NEW_UP_LIMIT_LOST, currPct, NEW_PER_LOST);
            LeoLog.e(TAG, "mNewInstructAnim end");
            mNewLostAnim.start();
        } else if (animation == mNewWifiAnim) {
            mFragment.OnItemAnimationEnd(mNewWifiLayout);

            if (!mIsInsValiable) {
                mNewLostAnim = getItemAnimation(mNewLostLayout);
                mNewLostAnim.start();
            } else {
                mNewInstructAnim = getItemAnimation(mNewInstructLayout);
                mNewInstructAnim.start();
            }

        } else if (animation == mNewLostAnim) {
            mFragment.OnItemAnimationEnd(mNewLostLayout);
            mNewVidAnim = getItemAnimation(mNewVidLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(NEW_UP_LIMIT_VID, currPct, NEW_PER_VID);
            LeoLog.e(TAG, "mNewLostAnim end");
            mNewVidAnim.start();
        } else if (animation == mNewVidAnim) {
            mFragment.OnItemAnimationEnd(mNewVidLayout);
            mNewPicAnim = getItemAnimation(mNewPicLayout);
            int currPct = mActivity.getScanningPercent();
            LeoPreference leoPreference = LeoPreference.getInstance();
            boolean picConsumed = leoPreference.getBoolean(PrefConst.KEY_PIC_COMSUMED, false);
            int duration = picConsumed ? NEW_UP_LIMIT_PIC_PROCESSED : NEW_UP_LIMIT_PIC;
            mActivity.scanningFromPercent(duration, currPct, NEW_PER_PIC);
            LeoLog.e(TAG, "mNewVidAnim end");
            mNewPicAnim.start();
        } else if (animation == mNewPicAnim) {
            mFragment.OnItemAnimationEnd(mNewPicLayout);
            mNewAppAnim = getItemAnimation(mNewAppLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(NEW_UP_LIMIT_APP, currPct, NEW_PER_APP);
            LeoLog.e(TAG, "mNewPicAnim end");
            mNewAppAnim.start();
        } else {
            mFragment.OnItemAnimationEnd(mNewAppLayout);
            int currPct = mActivity.getScanningPercent();
            mActivity.scanningFromPercent(300, currPct, NEW_PER_PRI + 1);
        }
    }

    public void detachTheController() {
        endAnim(mNewAppAnim);
        endAnim(mNewPicAnim);
        endAnim(mNewVidAnim);
        endAnim(mNewInstructAnim);
        endAnim(mNewWifiAnim);
        endAnim(mNewLostAnim);
        endAnim(mNewContactAnim);

    }

    private void endAnim(Animator animator) {
        if (animator != null) {
            animator.end();
            animator.cancel();
            animator = null;
        }
    }

}
