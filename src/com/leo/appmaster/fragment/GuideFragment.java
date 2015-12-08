package com.leo.appmaster.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.leo.appmaster.R;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.eventbus.LeoEventBus;
import com.leo.appmaster.eventbus.event.BackupEvent;
import com.leo.appmaster.eventbus.event.CommonEvent;
import com.leo.appmaster.eventbus.event.EventId;
import com.leo.appmaster.home.HomeActivity;
import com.leo.appmaster.utils.PrefConst;
import com.leo.tools.animator.ObjectAnimator;


public class GuideFragment extends Fragment implements View.OnClickListener {

    private static final long HOME_GUIDE_ANIM_TIME = 500;
    private static final int HOME_GUIDE_REPEAT_COUNT = -1;
    private static final float HOME_GUIDE_TRANSLA_VALUE1 = 0.0f;
    private static final String ANIME_PROPERTY_NAME = "translationY";
    public static final String EVENT_HOME_GUIDE_MSG = "HOME_GUIDE_MSG";

    private View mRootView;
    private RelativeLayout mHomeGuideRt;
    private RelativeLayout mPicVideoGuideRt;
    private RelativeLayout mVideoGuideRt;
    private FrameLayout mHomeGuideFt;
    private ObjectAnimator mHomeGuideAnim;

    /*引导类型*/
    public enum GUIDE_TYPE {
        HOME_MORE_GUIDE, PIC_GUIDE, VIDEO_GUIDE
    }

    public static GuideFragment newInstance() {
        GuideFragment fragment = new GuideFragment();
        return fragment;
    }

    public GuideFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_base_guide, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRootView = view.findViewById(R.id.guide_ft);
        initUI();
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    private void initUI() {
        if (mRootView != null) {
            initHomeMoreGuide(mRootView);
            initPicVidEditGuide(mRootView);
            initVideoEditGuide(mRootView);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHomeGuideAnim != null) {
            if (mHomeGuideAnim.isRunning()) {
                mHomeGuideAnim.cancel();
            }
            mHomeGuideFt.clearAnimation();
            mHomeGuideAnim = null;
        }
    }

    /*首次处理完图片，视频返回首页引导*/
    private void initHomeMoreGuide(View view) {
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.home_more_guide);
        if (viewStub == null) {
            return;
        }
        View inClude = viewStub.inflate();
        mHomeGuideRt = (RelativeLayout) inClude.findViewById(R.id.pic_vid_home_rt);
        mHomeGuideFt = (FrameLayout) inClude.findViewById(R.id.home_guide_ft);
        mHomeGuideRt.setOnClickListener(this);
    }

    /*图片隐藏页编辑按钮引导*/
    private void initPicVidEditGuide(View view) {
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.pic_vid_edit_guide);
        if (viewStub == null) {
            return;
        }
        View inCloude = viewStub.inflate();
        mPicVideoGuideRt = (RelativeLayout) inCloude.findViewById(R.id.pic_vid_edit_rt);
        mPicVideoGuideRt.setOnClickListener(this);
    }

    /*视频隐藏页编辑按钮引导*/
    private void initVideoEditGuide(View view) {
        ViewStub viewStub = (ViewStub) view.findViewById(R.id.video_edit_guide);
        if (viewStub == null) {
            return;
        }
        View inCloude = viewStub.inflate();
        mVideoGuideRt = (RelativeLayout) inCloude.findViewById(R.id.video_edit_rt);
        mVideoGuideRt.setOnClickListener(this);
    }

    public void setEnable(boolean enable, GUIDE_TYPE type) {
        if (enable) {
            mRootView.setVisibility(View.VISIBLE);
            setVisiblityGuide(type);
        } else {
            mRootView.setVisibility(View.GONE);
            if (mHomeGuideAnim != null) {
                if (mHomeGuideAnim.isRunning()) {
                    mHomeGuideAnim.cancel();
                }
                mHomeGuideFt.clearAnimation();
                mHomeGuideAnim = null;
            }
        }
    }

    private void setVisiblityGuide(GUIDE_TYPE type) {
        if (type == null) {
            return;
        }
        if (GUIDE_TYPE.HOME_MORE_GUIDE == type) {
            if (mHomeGuideRt != null) {
                mHomeGuideRt.setVisibility(View.VISIBLE);
                float tranY = getActivity().getResources().getDimension(R.dimen.home_guide_trans_y);
                showHomeGuideAnim(mHomeGuideRt, HOME_GUIDE_TRANSLA_VALUE1, -tranY);
            }
        } else if (GUIDE_TYPE.PIC_GUIDE == type) {
            if (mPicVideoGuideRt != null) {
                mPicVideoGuideRt.setVisibility(View.VISIBLE);
                float tranY = getActivity().getResources().getDimension(R.dimen.home_guide_trans_y);
                showHomeGuideAnim(mPicVideoGuideRt, HOME_GUIDE_TRANSLA_VALUE1, tranY);
            }

        } else if (GUIDE_TYPE.VIDEO_GUIDE == type) {
            if (mVideoGuideRt != null) {
                mVideoGuideRt.setVisibility(View.VISIBLE);
                float tranY = getActivity().getResources().getDimension(R.dimen.home_guide_trans_y);
                showHomeGuideAnim(mVideoGuideRt, HOME_GUIDE_TRANSLA_VALUE1, tranY);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.pic_vid_home_rt:
                Toast.makeText(getActivity(), "首页引导", Toast.LENGTH_SHORT).show();
                mRootView.setVisibility(View.GONE);
                CommonEvent event = new CommonEvent(EventId.EVENT_HOME_GUIDE_UP_ARROW, EVENT_HOME_GUIDE_MSG);
                LeoEventBus.getDefaultBus().post(event);
                break;
            case R.id.pic_vid_edit_rt:
                Toast.makeText(getActivity(), "图片编辑引导", Toast.LENGTH_SHORT).show();
                mRootView.setVisibility(View.GONE);
                PreferenceTable pre = PreferenceTable.getInstance();
                pre.putBoolean(PrefConst.KEY_PIC_EDIT_GUIDE, true);
                break;
            case R.id.video_edit_rt:
                Toast.makeText(getActivity(), "视频编辑引导", Toast.LENGTH_SHORT).show();
                mRootView.setVisibility(View.GONE);
                PreferenceTable preTab = PreferenceTable.getInstance();
                preTab.putBoolean(PrefConst.KEY_VIDEO_EDIT_GUIDE, true);
                break;
            default:
                break;
        }

    }

    /*首页引导动画*/
    private void showHomeGuideAnim(View view, float value1, float value2) {
        if (view != null) {
            view.clearAnimation();
            if (mHomeGuideAnim != null) {
                if (mHomeGuideAnim.isRunning()) {
                    mHomeGuideAnim.cancel();
                }
                mHomeGuideAnim = null;
            }
            mHomeGuideAnim = ObjectAnimator.ofFloat(view, ANIME_PROPERTY_NAME, value1, value2);
            mHomeGuideAnim.setFloatValues();
            mHomeGuideAnim.setRepeatCount(HOME_GUIDE_REPEAT_COUNT);
            mHomeGuideAnim.setRepeatMode(ObjectAnimator.REVERSE);
            mHomeGuideAnim.setDuration(HOME_GUIDE_ANIM_TIME);

            mHomeGuideAnim.start();
        }
    }

}
