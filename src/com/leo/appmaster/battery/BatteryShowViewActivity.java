package com.leo.appmaster.battery;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.Window;
import android.view.WindowManager;

import com.leo.appmaster.R;
import com.leo.appmaster.appmanage.view.BackUpFragment;
import com.leo.appmaster.appmanage.view.RestoreFragment;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.BatteryViewEvent;
import com.leo.appmaster.fragment.BaseFragment;
import com.leo.appmaster.mgr.BatteryManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.WifiSecurityManager;
import com.leo.appmaster.mgr.impl.BatteryManagerImpl;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.BaseFragmentActivity;
import com.leo.appmaster.utils.LeoLog;

import java.util.List;


public class BatteryShowViewActivity extends BaseFragmentActivity implements BatteryManager.BatteryStateListener, ViewPager.OnPageChangeListener {
    private final String TAG = "AskAddToBlacklistActivity";
    private BatteryManagerImpl.BatteryState newState;
    private String mChangeType = BatteryManagerImpl.SHOW_TYPE_IN;
    private int mRemainTime;
    private ViewPager mViewPager;
    private BatteryFragmentHoler[] mFragmentHolders = new BatteryFragmentHoler[2];

    private EmptyFragment backupFragment;
    private BatteryViewFragment restoreFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //隐藏状态栏
        //定义全屏参数
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        //获得当前窗体对象
        Window window = getWindow();
        //设置当前窗体为全屏显示
        window.setFlags(flag, flag);

        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_batter_show_view);

        initAll();
        handleIntent();
    }

    private void initAll() {
        LeoEventBus.getDefaultBus().register(this);
        mBatteryManager.setBatteryStateListener(this);

        mViewPager = (ViewPager) findViewById(R.id.battery_viewpager);
        initFragment();

        mViewPager.setAdapter(new BatteryPagerAdapter(getSupportFragmentManager()));
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setCurrentItem(1);

        mViewPager.setOnPageChangeListener(this);
    }

    private void initFragment() {
        BatteryFragmentHoler holder = new BatteryFragmentHoler();

        holder.title = "type_0";
        backupFragment = new EmptyFragment();
        holder.fragment = backupFragment;
        mFragmentHolders[0] = holder;

        holder = new BatteryFragmentHoler();
        holder.title = "type_1";
        restoreFragment = new BatteryViewFragment();
        holder.fragment = restoreFragment;
        mFragmentHolders[1] = holder;

        // AM-614, remove cached fragments
        FragmentManager fm = getSupportFragmentManager();
        try {
            FragmentTransaction ft = fm.beginTransaction();
            List<Fragment> list = fm.getFragments();
            if (list != null) {
                for (Fragment f : fm.getFragments()) {
                    ft.remove(f);
                }
            }
            ft.commit();
        } catch (Exception e) {

        }
    }

    class BatteryFragmentHoler {
        String title;
        BaseFragment fragment;
    }

    class BatteryPagerAdapter extends FragmentPagerAdapter {
        public BatteryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentHolders[position].fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentHolders[position].title;
        }

        @Override
        public int getCount() {
            return mFragmentHolders.length;
        }
    }


    private void handleIntent() {
        Intent intent = getIntent();
        newState = (BatteryManagerImpl.BatteryState)
                intent.getExtras().get(BatteryManagerImpl.SEND_BUNDLE);
        mChangeType = intent.getStringExtra(BatteryManagerImpl.PROTECT_VIEW_TYPE);
        mRemainTime = intent.getIntExtra(BatteryManagerImpl.REMAIN_TIME, 0);
//        process();
    }

    public void onEventMainThread(BatteryViewEvent event) {
        LeoLog.d("testBatteryEvent", "getEvent : " + event.eventMsg);
        if (event.eventMsg.equals("finish_activity")) {
            finish();
        }
    }


    @Override
    public void finish() {
        super.finish();
//        BatteryProtectView.handleHide();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onStateChange(BatteryManager.EventType type, BatteryManager.BatteryState state, int remainTime) {
        if (type.equals(BatteryManager.EventType.BAT_EVENT_CHARGING)) {
            //3
            mChangeType = BatteryManagerImpl.UPDATE_UP;
        } else if (type.equals(BatteryManager.EventType.BAT_EVENT_CONSUMING)) {
            //4
            mChangeType = BatteryManagerImpl.UPDATE_DONW;
        } else if (type.equals(BatteryManager.EventType.SHOW_TYPE_OUT)) {
            //2
            mChangeType = BatteryManagerImpl.SHOW_TYPE_OUT;
        } else {
            //1
            mChangeType = BatteryManagerImpl.SHOW_TYPE_IN;
        }
        newState = state;
        mRemainTime = remainTime;
        process();
    }

    private void process() {
        BatteryProtectView mProtectView = BatteryProtectView.makeText(this);

        LeoLog.d("testBatteryEvent", "mChangeType : " + mChangeType);

        if (mChangeType.equals(BatteryManagerImpl.SHOW_TYPE_IN)) {
            mProtectView.setBatteryStatus(true);
            mProtectView.setBatteryLevel(newState.level);
            mProtectView.notifyViewUi();
            mProtectView.show();
        } else if (mChangeType.equals(BatteryManagerImpl.SHOW_TYPE_OUT)) {
            mProtectView.setBatteryLevel(newState.level);
            mProtectView.setBatteryStatus(false);
            mProtectView.notifyViewUi();
        } else if (mChangeType.equals(BatteryManagerImpl.UPDATE_UP)) {
            mProtectView.setBatteryLevel(newState.level);
            mProtectView.setBatteryStatus(true);
            mProtectView.notifyViewUi();
        } else {
            mProtectView.setBatteryLevel(newState.level);
            mProtectView.setBatteryStatus(true);
            mProtectView.notifyViewUi();
        }

    }

    @Override
    public void onPageScrolled(int i, float v, int i1) {

    }

    @Override
    public void onPageSelected(int i) {
        if (i == 0) {
            mViewPager.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 250);

        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }
}
