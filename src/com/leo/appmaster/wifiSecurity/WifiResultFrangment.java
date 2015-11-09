package com.leo.appmaster.wifiSecurity;

import java.util.Dictionary;
import java.util.Hashtable;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.applocker.manager.MobvistaEngine.MobvistaListener;
import com.leo.appmaster.appmanage.FlowActivity;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.WifiSecurityManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.RippleView;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.mobvista.sdk.m.core.entity.Campaign;

/**
 * Created by qili on 15-10-27.
 */
public class WifiResultFrangment extends Fragment implements RippleView.OnRippleCompleteListener {
    private Dictionary<Integer, Integer> listViewItemHeights = new Hashtable<Integer, Integer>();
    private WifiSecurityActivity mActivity;
    private View mRootView, mSafeView, mUnsafeView, mBottomView;
    private View mPingView;
    private View mUnsafeViewBottom;
    private RippleView mTrafficBtn, mOtherWifiBtn;
    protected LockManager mLockManager;
    private boolean mZeroLoadState, mOneLoadState, mTwoLoadState, mThridLoadState, mFourLoadState;
    private ImageView mOneImg, mTwoImg, mThreeImg, mFourImg;
    private int mToolbarHeight;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (WifiSecurityActivity) activity;
        mToolbarHeight = activity.getResources().getDimensionPixelSize(R.dimen.toolbar_height);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLockManager = mActivity.getLockManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wifi_result_fragment, container, false);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MobvistaEngine.getInstance(mActivity).release(Constants.UNIT_ID_60);
        ImageLoader.getInstance().clearMemoryCache();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRootView = view;
        mRootView.setVisibility(View.INVISIBLE);

        mSafeView = mRootView.findViewById(R.id.safe_content);
        mUnsafeView = mRootView.findViewById(R.id.unsafe_result_content);
        mUnsafeViewBottom = mRootView.findViewById(R.id.bottom_content_empty_unsafe);

        mBottomView = mRootView.findViewById(R.id.wifi_below_view_recomment);

        mTrafficBtn = (RippleView) mRootView.findViewById(R.id.wifi_resulte_sure);
        mTrafficBtn.setOnRippleCompleteListener(this);
        mOtherWifiBtn = (RippleView) mRootView.findViewById(R.id.wifi_resulte_other_wifi);
        mOtherWifiBtn.setOnRippleCompleteListener(this);

        mPingView = mUnsafeView.findViewById(R.id.unsafe_ping_result);
        mOneImg = (ImageView) mUnsafeView.findViewById(R.id.unsafe_wifi_is_connect_icon);
        mTwoImg = (ImageView) mUnsafeView.findViewById(R.id.unsafe_wifi_second_connect_icon);
        mThreeImg = (ImageView) mUnsafeView.findViewById(R.id.unsafe_wifi_ssl_icon);
        mFourImg = (ImageView) mUnsafeView.findViewById(R.id.unsafe_wifi_pas_type_icon);

        loadAd(mRootView);
    }

    private void loadAd(final View rootView) {
        AppMasterPreference amp = AppMasterPreference.getInstance(mActivity);
        if (amp.getADWifiScan() == 1) {
            MobvistaEngine.getInstance(mActivity).loadMobvista(Constants.UNIT_ID_60, new MobvistaListener() {

                @Override
                public void onMobvistaFinished(int code, Campaign campaign, String msg) {
                    if (code == MobvistaEngine.ERR_OK) {
                        initAdLayout(rootView, campaign, Constants.UNIT_ID_60);

                        SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "ad_act", "adv_shws_wifi");
                    }
                }

                @Override
                public void onMobvistaClick(Campaign campaign) {
                    LockManager lm = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
                    lm.filterSelfOneMinites();

                    SDKWrapper.addEvent(mActivity, SDKWrapper.P1, "ad_cli", "adv_cnts_wifi");
                }
            });
        }
    }


    private void initAdLayout(View rootView, Campaign campaign, String unitId) {
        View adView = rootView.findViewById(R.id.ad_content);
        TextView tvTitle = (TextView) adView.findViewById(R.id.item_title);
        tvTitle.setText(campaign.getAppName());
        ImageView preview = (ImageView) adView.findViewById(R.id.item_ad_preview);
        TextView summary = (TextView) adView.findViewById(R.id.item_summary);
        summary.setText(campaign.getAppDesc());
        Button btnCTA = (Button) adView.findViewById(R.id.ad_result_cta);
        btnCTA.setText(campaign.getAdCall());
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        displayImage(preview, campaign.getImageUrl());
        adView.setVisibility(View.VISIBLE);
        MobvistaEngine.getInstance(mActivity).registerView(Constants.UNIT_ID_60, adView);
    }

    private void displayImage(ImageView view, String uri) {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true).build();
        ImageLoader.getInstance().displayImage(uri, view, options);
    }

    public void showTab(boolean isWifiSafe, boolean isWifiConnect) {
        down2Up(isWifiSafe, isWifiConnect);
    }

    public void showTab(boolean isWifiSafe, boolean isWifiConnect, boolean mPingOk,
                        boolean mOneState, boolean mTwoState,
                        boolean mThridState, boolean mFourState) {
        mZeroLoadState = mPingOk;
        down2Up(isWifiSafe, isWifiConnect);
        mOneLoadState = mOneState;
        mTwoLoadState = mTwoState;
        mThridLoadState = mThridState;
        mFourLoadState = mFourState;
    }

    public void down2Up(final boolean isWifiSafe, boolean isWifiConnect) {
        if (mRootView.getVisibility() == View.VISIBLE) return;
        mRootView.setVisibility(View.VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.anim_down_to_up);

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (isWifiSafe) {
                    mSafeView.setVisibility(View.VISIBLE);
                } else {
                    mUnsafeView.setVisibility(View.VISIBLE);
                    mUnsafeViewBottom.setVisibility(View.VISIBLE);
                    mBottomView.setVisibility(View.VISIBLE);
                    setUnsafePageIcon();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mRootView.startAnimation(animation);
    }

    private void setUnsafePageIcon() {
        setImgIcon(mOneImg, mOneLoadState);
        setImgIcon(mTwoImg, mTwoLoadState);
        setImgIcon(mThreeImg, mThridLoadState);
        setImgIcon(mFourImg, mFourLoadState);
    }

    private void setImgIcon(ImageView view, boolean mFlag) {
        if (mFlag) {
            view.setImageResource(R.drawable.wifi_complete);
        } else {
            view.setImageResource(R.drawable.wifi_error);
        }
    }

    @Override
    public void onRippleComplete(RippleView rippleView) {
        if (mTrafficBtn == rippleView) {
//            mLockManager.filterPackage(mActivity.getPackageName(), 1000);
            Intent intent = new Intent(mActivity, FlowActivity.class);
            mActivity.startActivity(intent);
            SDKWrapper.addEvent(mActivity,
                    SDKWrapper.P1, "wifi_rst", "wifi_rst_safe_dataflow");
        } else if (mOtherWifiBtn == rippleView) {
            Intent wifiIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            try {
                startActivity(wifiIntent);
            } catch (Exception e) {
            }
            SDKWrapper.addEvent(mActivity,
                    SDKWrapper.P1, "wifi_rst", "wifi_rst_risk_other");
        }
    }
}