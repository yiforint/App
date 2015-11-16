package com.leo.appmaster.home;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.imagehide.PhotoItem;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.ExpandableGridView;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Jasper on 2015/11/13.
 */
public abstract class FolderFragment<T> extends Fragment implements AbsListView.OnScrollListener,
        ExpandableListView.OnGroupClickListener, FolderAdapter.OnFolderClickListener, RippleView.OnRippleCompleteListener ,
        View.OnClickListener {
    private Dictionary<Integer, Integer> mItemHeights = new Hashtable<Integer, Integer>();

    protected FolderAdapter<T> mAdapter;
    protected HomeActivity mActivity;
    protected List<T> mDataList;
    protected ExpandableListView mListView;

    protected TextView mProcessTv;

    private int mEmptyHeight;

    private int mToolbarHeight;

    private View mFloatingView;
    private TextView mFloatingTv;
    protected CheckBox mFloatingCb;
    private ImageView mFloatingArrow;
    private RippleView mFloatingRv;

    private RippleView mProcessBtn;
    private RippleView mIgnoreBtn;
    protected LEOAlarmDialog mIgnoreDlg;

    private View mOffsetBg;
    protected int mCurrentGroup;
    private int mLastGroup = -1;
    protected boolean mHidingTimeout;
    protected boolean mHidingFinish;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = (HomeActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToolbarHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        mEmptyHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.pri_pro_header);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ExpandableGridView) view.findViewById(getListViewId());
        mListView.addHeaderView(getEmptyHeader());
        mListView.setAdapter(mAdapter);
        mAdapter.setList(mDataList);
        mAdapter.setOnFolderClickListener(this);

        mListView.setOnScrollListener(this);
        mListView.setOnGroupClickListener(this);

        for (int i = 0; i < mAdapter.getGroupCount(); i++) {
            mListView.collapseGroup(i);
        }

        mFloatingView = view.findViewById(R.id.floating_lable);
        mFloatingView.setVisibility(View.INVISIBLE);

        mFloatingTv = (TextView) mFloatingView.findViewById(R.id.pri_pro_new_label_tv);
        mFloatingCb = (CheckBox) mFloatingView.findViewById(R.id.pri_pro_cb);
        mFloatingRv = (RippleView) mFloatingView.findViewById(R.id.pri_pro_click_rv);
        mFloatingRv.setOnRippleCompleteListener(this);
        mFloatingCb.setOnClickListener(this);

        mProcessBtn = (RippleView) view.findViewById(R.id.pp_process_rv);
        mIgnoreBtn = (RippleView) view.findViewById(R.id.pp_process_ignore_rv);
        mProcessTv = (TextView) view.findViewById(R.id.pp_process_tv);

        mProcessBtn.setOnRippleCompleteListener(this);
        mIgnoreBtn.setOnRippleCompleteListener(this);
        mOffsetBg = view.findViewById(R.id.pri_offset_bg);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        View c = view.getChildAt(0); //this is the first visible row
        if (c == null) return;

        int scrollY = -c.getTop();
        mItemHeights.put(view.getFirstVisiblePosition(), c.getHeight());
        for (int i = 0; i < view.getFirstVisiblePosition(); ++i) {
            if (mItemHeights.get(i) != null) // (this is a sanity check)
                scrollY += mItemHeights.get(i); //add all heights of the views that are gone
        }

        mActivity.onListScroll(scrollY);
        int maxScrollHeight = mEmptyHeight - mToolbarHeight;
        if (scrollY > maxScrollHeight) {
            mOffsetBg.setTranslationY(-maxScrollHeight);
        } else {
            mOffsetBg.setTranslationY(-scrollY);
        }
        if (scrollY > maxScrollHeight && mListView.isGroupExpanded(0)) {
            mFloatingView.setVisibility(View.VISIBLE);

            FolderAdapter.ItemsWrapper<T> wrapper = (FolderAdapter.ItemsWrapper<T>)
                    mAdapter.getFirstVisibleGroup(firstVisibleItem);
            if (wrapper == null) return;
            mCurrentGroup = mAdapter.getFirstVisibleGroupPosition(firstVisibleItem);
            if (mCurrentGroup != mLastGroup) {
                mAdapter.setLableContent(mFloatingTv, wrapper.parentName, wrapper.items.size());
                mFloatingCb.setChecked(mAdapter.isGroupChecked(wrapper));
            }

            mLastGroup = mCurrentGroup;
        } else {
            mLastGroup = -1;
            mFloatingView.setVisibility(View.INVISIBLE);
        }

    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        parent.expandGroup(groupPosition);
        return true;
    }

    @Override
    public void onGroupClick(final int groupPosition, boolean isExpanded) {
        if (isExpanded) {
            for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                mListView.collapseGroup(i);
            }
        } else {
            for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                mListView.expandGroup(i);
            }
        }
        ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mListView.setSelectedGroup(groupPosition);
            }
        }, 100);
    }

    @Override
    public void onGroupCheckChanged(int groupPosition, boolean checked) {
        int group = mCurrentGroup;
        if (group == groupPosition) {
            mFloatingCb.setChecked(checked);
        }
    }

    @Override
    public void onSelectionChange(boolean selectAll, int selectedCount) {
        if (selectedCount > 0) {
            mProcessBtn.setEnabled(true);
            mProcessBtn.setBackgroundResource(R.drawable.green_radius_btn_shape);
        } else {
            mProcessBtn.setEnabled(false);
            mProcessBtn.setBackgroundResource(R.drawable.green_radius_shape_disable);
        }
    }

    @Override
    public void onRippleComplete(RippleView rippleView) {
        if (mFloatingRv == rippleView) {
            for (int i = 0; i < mAdapter.getGroupCount(); i++) {
                mListView.collapseGroup(i);
                final int group = mCurrentGroup;
                ThreadManager.getUiThreadHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mListView.setSelectedGroup(group);
                    }
                }, 100);
            }
        } else if (mProcessBtn == rippleView) {
            onProcessClick();
        } else if (mIgnoreBtn == rippleView) {
            onIgnoreClick();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mFloatingCb) {
            onFloatingCheckClick();
        }
    }

    public void setData(List<T> list) {
        if (list == null) return;

        mDataList = new ArrayList<T>();
        for (T data : list) {
            mDataList.add(data);
        }

        if (mAdapter != null) {
            mAdapter.setList(mDataList);
        }
    }

    protected View getEmptyHeader() {
        TextView textView = new TextView(getActivity());
        textView.setLayoutParams(new AbsListView.LayoutParams(1, mEmptyHeight - mToolbarHeight));
        textView.setBackgroundResource(R.color.transparent);
        textView.setClickable(false);
        textView.setEnabled(false);
        textView.setWidth(1);

        return textView;
    }

    protected abstract int getListViewId();
    protected abstract void onIgnoreClick();
    protected abstract void onProcessClick();

    protected abstract void onIgnoreConfirmClick();
    protected abstract void onIgnoreCancelClick();

    protected abstract void onFloatingCheckClick();

    protected void initIgnoreDlg() {
        if (mIgnoreDlg != null) return;

        mIgnoreDlg = new LEOAlarmDialog(getActivity());
        String content = getString(R.string.pri_pro_ignore_dialog);
        mIgnoreDlg.setContent(content);
        mIgnoreDlg.setLeftBtnListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onIgnoreCancelClick();
                mIgnoreDlg.dismiss();
            }
        });
        mIgnoreDlg.setRightBtnListener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onIgnoreConfirmClick();
                mIgnoreDlg.dismiss();
            }
        });
    }
}