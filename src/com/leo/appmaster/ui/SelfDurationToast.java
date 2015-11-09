package com.leo.appmaster.ui;

import com.leo.appmaster.AppMasterApplication;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.appmanage.FlowActivity;
import com.leo.appmaster.mgr.LockManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.WifiSecurityManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.BuildProperties;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.FileOperationUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.wifiSecurity.WifiSecurityActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;


public class SelfDurationToast {
    public static final int SHOWNAME = 1;
    private static final int API_LEVEL_19 = 19;
    public static ImageView realLoading;
    public static TextView tv_clean_rocket;
    public static String wifiName;
    private static Context mContext;
    private static View contentView, loadingView;
    private static ImageView mArrow;

    private static android.os.Handler handler = new android.os.Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SHOWNAME:
                    WifiSecurityManager wsm = (WifiSecurityManager)
                            MgrContext.getManager(MgrContext.MGR_WIFI_SECURITY);
                    boolean isWifiOpen = wsm.isWifiOpen();
                    //connect wifi?
                    boolean isSelectWifi = wsm.getIsWifi();
                    if (isWifiOpen && isSelectWifi) {
                        realLoading.clearAnimation();
                        loadingView.setVisibility(View.GONE);
                        contentView.setVisibility(View.VISIBLE);
                        int wifiSate = (Integer) msg.obj;
                        if (wifiSate == 2) {
                            String a1 = mContext.getString(R.string.over_traffic_toast_unsafe, wifiName);
                            tv_clean_rocket.setText(Html.fromHtml(a1));
                            mArrow.setImageResource(R.drawable.wifi_toast_redarrow);
                        } else {
                            String a2 = mContext.getString(R.string.over_traffic_toast_safe, wifiName);
                            tv_clean_rocket.setText(Html.fromHtml(a2));
                            mArrow.setImageResource(R.drawable.wifi_toast_bluearrow);
                        }
                    } else {
                        loadingView.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    };

    public static SelfDurationToast makeText(final Context context, String text, int duration, final int wifiState) {
        SelfDurationToast result = new SelfDurationToast(context);


        SDKWrapper.addEvent(context,
                SDKWrapper.P1, "wifi_scan", "wifi_cnts_toast");
        mContext = context;
        wifiName = text;
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.wifi_change_toast, null);
        view.setBackgroundResource(R.color.transparent);
        mArrow = (ImageView) view.findViewById(R.id.iv_arrow);
        realLoading = (ImageView) view.findViewById(R.id.loding_iv);
        realLoading.setImageResource(R.drawable.real_loading);
        Animation loadingAnimation = AnimationUtils.
                loadAnimation(context, R.anim.loading_animation);
        realLoading.setAnimation(loadingAnimation);

        final int sendWifiState = wifiState;
        contentView = view.findViewById(R.id.wifi_result_content);
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LockManager mLockManager = (LockManager) MgrContext.getManager(MgrContext.MGR_APPLOCKER);
//                mLockManager.filterSelfOneMinites();
                mLockManager.filterPackage(mContext.getPackageName(), 1000);
                Intent wifiIntent = new Intent(mContext, WifiSecurityActivity.class);
                wifiIntent.putExtra("from", "toast");
                wifiIntent.putExtra("wifistate", sendWifiState);
                wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                try {
                    mContext.startActivity(wifiIntent);
                } catch (Exception e) {

                }
            }
        });
        loadingView = view.findViewById(R.id.loading_content);
        tv_clean_rocket = (TextView) view.findViewById(R.id.tv_clean_rocket);
        result.mNextView = view;
        result.mDuration = duration;

        readyShow(wifiState);

        return result;
    }

    private static void readyShow(final int wifiState) {
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.what = SHOWNAME;
                msg.obj = wifiState;
                handler.sendMessageDelayed(msg, 2000);
            }
        });
    }

    public static final int LENGTH_SHORT = 2000;
    public static final int LENGTH_LONG = 3500;

    private final Handler mHandler = new Handler();
    private int mDuration = LENGTH_SHORT;
    private int mGravity = Gravity.CENTER;
    private int mX, mY;
    private float mHorizontalMargin;
    private float mVerticalMargin;
    private View mView;
    private View mNextView;

    private WindowManager mWM;
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();


    public SelfDurationToast(Context context) {
        init(context);
    }

    /**
     * Set the view to show.
     *
     * @see #getView
     */
    public void setView(View view) {
        mNextView = view;
    }

    /**
     * Return the view.
     *
     * @see #setView
     */
    public View getView() {
        return mNextView;
    }

    /**
     * Set how long to show the view for.
     *
     * @see #LENGTH_SHORT
     * @see #LENGTH_LONG
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * Return the duration.
     *
     * @see #setDuration
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * Set the margins of the view.
     *
     * @param horizontalMargin The horizontal margin, in percentage of the
     *                         container width, between the container's edges and the
     *                         notification
     * @param verticalMargin   The vertical margin, in percentage of the
     *                         container height, between the container's edges and the
     *                         notification
     */
    public void setMargin(float horizontalMargin, float verticalMargin) {
        mHorizontalMargin = horizontalMargin;
        mVerticalMargin = verticalMargin;
    }

    /**
     * Return the horizontal margin.
     */
    public float getHorizontalMargin() {
        return mHorizontalMargin;
    }

    /**
     * Return the vertical margin.
     */
    public float getVerticalMargin() {
        return mVerticalMargin;
    }

    /**
     * Set the location at which the notification should appear on the screen.
     *
     * @see android.view.Gravity
     * @see #getGravity
     */
    public void setGravity(int gravity, int xOffset, int yOffset) {
        mGravity = gravity;
        mX = xOffset;
        mY = yOffset;
    }

    /**
     * Get the location at which the notification should appear on the screen.
     *
     * @see android.view.Gravity
     * @see #getGravity
     */
    public int getGravity() {
        return mGravity;
    }

    /**
     * Return the X offset in pixels to apply to the gravity's location.
     */
    public int getXOffset() {
        return mX;
    }

    /**
     * Return the Y offset in pixels to apply to the gravity's location.
     */
    public int getYOffset() {
        return mY;
    }

    /**
     * schedule handleShow into the right thread
     */
    public void show() {
        setGravity(Gravity.LEFT | Gravity.TOP, DipPixelUtil.dip2px(mContext, 7), DipPixelUtil.dip2px(mContext, 32));
        mHandler.post(mShow);
        if (mDuration > 0) {
            mHandler.postDelayed(mHide, mDuration);
        }
    }

    /**
     * schedule handleHide into the right thread
     */
    public void hide() {
        mHandler.post(mHide);
    }

    private final Runnable mShow = new Runnable() {
        public void run() {
            handleShow();
        }
    };

    private final Runnable mHide = new Runnable() {
        public void run() {
            handleHide();
        }
    };

    private void init(Context context) {
        final WindowManager.LayoutParams params = mParams;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;

//        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
//                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
//                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        params.format = PixelFormat.TRANSLUCENT;
        params.windowAnimations = android.R.style.Animation_Toast;
        int currSDK_INT = Build.VERSION.SDK_INT;
        if (currSDK_INT < API_LEVEL_19) {
            params.type = WindowManager.LayoutParams.TYPE_PRIORITY_PHONE;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_TOAST;
        }

        mWM = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
    }


    private void handleShow() {

        if (mView != mNextView) {
            // remove the old view if necessary
            handleHide();
            mView = mNextView;
//            mWM = WindowManagerImpl.getDefault();
            final int gravity = mGravity;
            mParams.gravity = gravity;
            if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.FILL_HORIZONTAL) {
                mParams.horizontalWeight = 1.0f;
            }
            if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.FILL_VERTICAL) {
                mParams.verticalWeight = 1.0f;
            }
            mParams.x = mX;
            mParams.y = mY;
            mParams.verticalMargin = mVerticalMargin;
            mParams.horizontalMargin = mHorizontalMargin;
            if (mView.getParent() != null) {
                mWM.removeView(mView);
            }
            mWM.addView(mView, mParams);
        }
    }

    private void handleHide() {
        if (mView != null) {
            if (mView.getParent() != null) {
                mWM.removeView(mView);
            }
            mView = null;
        }
    }
}