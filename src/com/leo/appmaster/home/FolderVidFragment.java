package com.leo.appmaster.home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.SecurityNotifyChangeEvent;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.videohide.VideoItemBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jasper on 2015/10/22.
 */
public class FolderVidFragment extends FolderFragment<VideoItemBean> {
    private static final String TAG = "FolderVidFragment";

    public static FolderVidFragment newInstance() {
        return new FolderVidFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new FolderVidAdapter();
        LeoEventBus.getDefaultBus().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LeoEventBus.getDefaultBus().unregister(this);
    }

    public void onEventMainThread(SecurityNotifyChangeEvent event) {
        if (!MgrContext.MGR_PRIVACY_DATA.equals(event.mgr)) return;

        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
                List<VideoItemBean> list = pdm.getAddVid();
                if (list == null) return;

                if (list.size() != mDataList.size()) {
                    mDataList.clear();
                    mDataList.addAll(list);
                    mAdapter.setList(list);
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vid_folder, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProcessTv.setText(R.string.pri_pro_hide_vid);
    }

    private void hideAllVidBackground(final List<String> photoItems, final int incScore) {
        mHidingTimeout = false;
        mHidingFinish = false;
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
                pdm.onHideAllVid(photoItems);
                mHidingFinish = true;
                if (!mHidingTimeout) {
                    onProcessFinish(incScore);
                }
            }
        });
    }

    private void onProcessFinish(final int incScore) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.onProcessFinish(incScore, MgrContext.MGR_PRIVACY_DATA);
            }
        });
    }

    @Override
    protected int getListViewId() {
        return R.id.floating_video_lv;
    }

    @Override
    protected void onIgnoreClick() {
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "vid_skip_cnts");
        if (mIgnoreDlg == null) {
            initIgnoreDlg();
        }
        if (mActivity.shownIgnoreDlg()) {
            mActivity.onIgnoreClick(0, MgrContext.MGR_PRIVACY_DATA);
        } else {
            mIgnoreDlg.show();
            mActivity.setShownIngoreDlg();
        }
    }

    @Override
    protected void onProcessClick() {
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "vid_hide_cnts");
        mActivity.onProcessClick(this);
        PreferenceTable.getInstance().putBoolean(PrefConst.KEY_SCANNED_VID, true);
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                List<VideoItemBean> list = mAdapter.getSelectData();
                PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);

                List<String> photos = new ArrayList<String>(list.size());
                for (VideoItemBean videoItemBean : list) {
                    photos.add(videoItemBean.getPath());
                }
                final int incScore = pdm.haveCheckedVid();
                hideAllVidBackground(photos, incScore);
                ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHidingTimeout = true;
                        if (!mHidingFinish) {
                            onProcessFinish(incScore);
                        }
                    }
                }, 8000);
            }
        });
    }

    @Override
    protected void onIgnoreConfirmClick() {
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "vid_skip_confirm");
        onIgnoreClick();
    }

    @Override
    protected void onIgnoreCancelClick() {
        SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process", "vid_skip_cancel");
    }

    @Override
    protected void onFloatingCheckClick() {
        int groupPos = mCurrentGroup;
        if (mFloatingCb.isChecked()) {
            SDKWrapper.addEvent(getActivity(), SDKWrapper.P1, "process",
                    "vid_folder_full_" + mAdapter.getGroupName(groupPos));
            mAdapter.selectAll(groupPos);
        } else {
            mAdapter.deselectAll(groupPos);
        }
    }


}