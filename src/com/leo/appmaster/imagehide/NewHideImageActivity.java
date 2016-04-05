package com.leo.appmaster.imagehide;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.home.HomeScanningFragment;
import com.leo.appmaster.home.PrivacyNewPicFragment;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.privacy.Privacy;
import com.leo.appmaster.privacy.PrivacyHelper;
import com.leo.appmaster.sdk.BaseFragmentActivity;
import com.leo.appmaster.ui.CommonToolbar;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.utils.DataUtils;
import com.leo.appmaster.utils.LeoLog;

import java.util.List;

/**
 * Created by yangyuefeng on 2016/3/29.
 */
public class NewHideImageActivity extends BaseFragmentActivity {

    private static final String TAG = NewHideImageActivity.class.getSimpleName();

    private CommonToolbar mTtileBar;
    private LEOAlarmDialog mDialog;

    private FrameLayout mFragment;
    private ProgressBar mLoading;
    private List<PhotoItem> mPhotoItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_hide_image);
        mTtileBar = (CommonToolbar) findViewById(R.id.layout_title_bar);
        mTtileBar.setToolbarTitle(R.string.new_hidden_image);
        mLoading = (ProgressBar) findViewById(R.id.pb_loading_pic);
        mFragment = (FrameLayout)findViewById(R.id.fl_image_view);
        initLoadData();

        PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
        pdm.haveCheckedPic();
    }

    private void initLoadData() {
//        mLoading.setVisibility(View.VISIBLE);
//        mFragment.setVisibility(View.GONE);
        Privacy privacy = PrivacyHelper.getImagePrivacy();
        mPhotoItems = privacy.getNewList();
        HomeScanningFragment.PhotoList mPhotoList = new HomeScanningFragment.PhotoList();
        mPhotoList.photoItems = mPhotoItems;
        mPhotoList.inDifferentDir = DataUtils.differentDirPic(mPhotoItems);
        LeoLog.v(TAG, "mPhotoList size :" + (mPhotoList.photoItems == null ? 0 : mPhotoList.photoItems.size()));
        Fragment fragment = PrivacyNewPicFragment.getNewHideImageFragment(mPhotoList);
        LeoLog.v(TAG, "fragment != null : " + (fragment != null));
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fl_image_view, fragment);
        ft.commit();
        mLoading.setVisibility(View.GONE);
        mFragment.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
