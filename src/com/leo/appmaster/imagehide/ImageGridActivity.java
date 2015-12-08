package com.leo.appmaster.imagehide;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.fragment.GuideFragment;
import com.leo.appmaster.home.HomeTabFragment;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.BaseFragmentActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.CommonToolbar;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog;
import com.leo.appmaster.ui.dialog.LEOAlarmDialog.OnDiaogClickListener;
import com.leo.appmaster.ui.dialog.LEOCircleProgressDialog;
import com.leo.appmaster.ui.dialog.OneButtonDialog;
import com.leo.appmaster.utils.FileOperationUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.core.FadeInBitmapDisplayer;
import com.leo.tools.animator.Animator;
import com.leo.tools.animator.AnimatorListenerAdapter;
import com.leo.tools.animator.AnimatorSet;
import com.leo.tools.animator.ObjectAnimator;

public class ImageGridActivity extends BaseFragmentActivity implements OnClickListener, OnItemClickListener {
    public final static int INIT_UI_DONE = 24;
    public final static int LOAD_DATA_DONE = 25;
    public final static int START_CANCEL_OR_HIDE_PIC = 26;
    public final static int HIDE_FINISH = 27;

    public final static int CANCEL_HIDE_MODE = 0;
    public final static int SELECT_HIDE_MODE = 1;
    private int mActicityMode = SELECT_HIDE_MODE;

    private CommonToolbar mTtileBar;
    private PhotoAibum mPhotoAibum;
    private int mPhotoAibumPos;
    private GridView mGridView;
    private ImageAdapter mImageAdapter;
    private DisplayImageOptions mOptions;
    private ImageLoader mImageLoader;
    private LinearLayout mBottomBar;
    private Button mSelectAll;
    private Button mHidePicture;

    private ArrayList<PhotoItem> mClickList = new ArrayList<PhotoItem>();
    private LinkedList<Integer> mClickPosList = new LinkedList<Integer>();
    private List<PhotoItem> mPicturesList = new ArrayList<PhotoItem>();
    private ArrayList<String> mAllListPath = new ArrayList<String>();

    private boolean mIsEditmode = false;
    private LEOCircleProgressDialog mProgressDialog;
    private LEOAlarmDialog mDialog;
    private OneButtonDialog memeryDialog;
    private boolean mIsBackgoundRunning = false;
    private ProgressBar mLoadingBar;
    private Boolean mIsFromIntruderMore = false;
    private Boolean isLoadDone = false;

    private GuideFragment mGuideFragment;
    private boolean mPicGuide;

    private android.os.Handler mHandler = new android.os.Handler() {
        public void handleMessage(final android.os.Message msg) {
            switch (msg.what) {
                case INIT_UI_DONE:
                    asyncLoad();
                    break;
                case LOAD_DATA_DONE:
                    loadDone();
                    break;
                case START_CANCEL_OR_HIDE_PIC:
                    final boolean isHide = (Boolean) msg.obj;
                    ThreadManager.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            startDoingBack(isHide);
                        }
                    });
                    break;
                case HIDE_FINISH:
                    int isSuccess = (Integer) msg.obj;
                    onPostDo(isSuccess);
                    break;
            }
        }
    };


    private void loadDone() {
        isLoadDone = true;
        mLoadingBar.setVisibility(View.GONE);
        mGridView.setVisibility(View.VISIBLE);
        if (mPhotoAibum != null) {
            mPicturesList = ((PrivacyDataManager) MgrContext.
                    getManager(MgrContext.MGR_PRIVACY_DATA)).getHidePicFile(mPhotoAibum);
            mTtileBar.setToolbarTitle(mPhotoAibum.getName());
            for (PhotoItem item : mPicturesList) {
                mAllListPath.add(item.getPath());
            }
        }

        mImageAdapter = new ImageAdapter();
        mGridView.setAdapter(mImageAdapter);
        mSelectAll.setEnabled(true);
    }

    private void asyncLoad() {
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                if (mActicityMode == SELECT_HIDE_MODE) {
                    mPhotoAibum = ((PrivacyDataManager) MgrContext.
                            getManager(MgrContext.MGR_PRIVACY_DATA)).getAllPicFile().get(
                            mPhotoAibumPos);
                } else {
                    mPhotoAibum = ((PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA)).
                            getHidePicAlbum("").get(mPhotoAibumPos);
                }
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(LOAD_DATA_DONE);
                }
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_grid);
        mGridView = (GridView) findViewById(R.id.image_gallery_image);
        mBottomBar = (LinearLayout) findViewById(R.id.bottom_bar);
        mSelectAll = (Button) findViewById(R.id.select_all);
        mSelectAll.setEnabled(false);
        mHidePicture = (Button) findViewById(R.id.hide_image);
        mTtileBar = (CommonToolbar) findViewById(R.id.layout_title_bar);
        mTtileBar.setToolbarTitle("");

        mLoadingBar = (ProgressBar) findViewById(R.id.pb_loading_pic);
        onInit();
        initImageLoder();
        loadImageList();
        mGridView.setOnItemClickListener(this);
        mSelectAll.setOnClickListener(this);
        mHidePicture.setOnClickListener(this);
    }

    private void updateRightButton() {
        if (mClickList.size() > 0) {
            if (mActicityMode == SELECT_HIDE_MODE) {
                mHidePicture.setText(getString(R.string.app_hide_image) + "("
                        + mClickList.size() + ")");
            } else if (mActicityMode == CANCEL_HIDE_MODE) {
                mHidePicture.setText(getString(R.string.app_cancel_hide_image)
                        + "(" + mClickList.size() + ")");
            }
            mHidePicture.setEnabled(true);
        } else {
            if (mActicityMode == SELECT_HIDE_MODE) {
                mHidePicture.setText(getString(R.string.app_hide_image));
            } else if (mActicityMode == CANCEL_HIDE_MODE) {
                mHidePicture.setText(getString(R.string.app_cancel_hide_image));
            }
            mHidePicture.setEnabled(false);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        if (!mPicGuide) {
            cancelGuide();
        }
        if (mActicityMode == CANCEL_HIDE_MODE && mIsEditmode) {
            cancelEditMode();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LeoLog.d("teststartResult", "onActivityResult");
        if (resultCode == RESULT_OK) {
            mAllListPath = data.getStringArrayListExtra("list");
            LeoLog.d("teststartResult", "mAllListPath size : " + mAllListPath.size());
            for (Iterator iterator = mPicturesList.iterator(); iterator
                    .hasNext(); ) {
                PhotoItem item = (PhotoItem) iterator.next();
                if (!mAllListPath.contains(item.getPath())) {
                    iterator.remove();
                }
            }
            if (mImageAdapter != null) {
                mImageAdapter.notifyDataSetChanged();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initImageLoder() {
        mOptions = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.photo_bg_loding)
                .showImageForEmptyUri(R.drawable.photo_bg_loding)
                .showImageOnFail(R.drawable.photo_bg_loding)
                .displayer(new FadeInBitmapDisplayer(500))
                .cacheInMemory(true).cacheOnDisk(true).considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565).build();

        mImageLoader = ImageLoader.getInstance();
    }

    private void onInit() {
        Intent intent = getIntent();
        mActicityMode = intent.getIntExtra("mode", SELECT_HIDE_MODE);
        mIsFromIntruderMore = intent.getBooleanExtra("fromIntruderMore", false);
        mPhotoAibum = (PhotoAibum) intent.getExtras().get("data");
        if (mPhotoAibum == null) {
            mPhotoAibumPos = (int) intent.getIntExtra("pos", 0);
        }

        if (mPhotoAibum != null) {
            isLoadDone = true;
            mPicturesList = ((PrivacyDataManager) MgrContext.
                    getManager(MgrContext.MGR_PRIVACY_DATA)).getHidePicFile(mPhotoAibum);
            mTtileBar.setToolbarTitle(mPhotoAibum.getName());
            mTtileBar.setOptionMenuVisible(false);
            for (PhotoItem item : mPicturesList) {
                mAllListPath.add(item.getPath());
            }
        }
        if (mActicityMode == SELECT_HIDE_MODE) {
            mHidePicture.setText(R.string.app_hide_image);
        } else if (mActicityMode == CANCEL_HIDE_MODE) {
            mHidePicture.setText(R.string.app_cancel_hide_image);
            Drawable topDrawable = getResources().getDrawable(
                    R.drawable.unhide_picture_selector);
            topDrawable.setBounds(0, 0, topDrawable.getMinimumWidth(),
                    topDrawable.getMinimumHeight());
            mHidePicture.setCompoundDrawables(null, topDrawable, null, null);
            mTtileBar.setOptionMenuVisible(true);
            mTtileBar.setOptionImageResource(R.drawable.edit_mode_name);
            mTtileBar.setOptionClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (mActicityMode == CANCEL_HIDE_MODE && isLoadDone) {
                        mIsEditmode = !mIsEditmode;
                        if (!mIsEditmode) {
                            cancelEditMode();
                        } else {
                            mBottomBar.setVisibility(View.VISIBLE);
                            mTtileBar.setOptionImageResource(R.drawable.mode_done);
                        }
                        mSelectAll.setText(R.string.app_select_all);
                        mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                                getResources().getDrawable(R.drawable.select_all_selector), null,
                                null);
                        mImageAdapter.notifyDataSetChanged();
                    }
                    if (!mPicGuide) {
                        cancelGuide();
                    }
                }
            });
            mBottomBar.setVisibility(View.GONE);
            PreferenceTable pre = PreferenceTable.getInstance();
            mPicGuide = pre.getBoolean(PrefConst.KEY_PIC_EDIT_GUIDE, false);
            if (!mPicGuide) {
                mGuideFragment = (GuideFragment) getSupportFragmentManager().findFragmentById(R.id.pic_guide);
                mGuideFragment.setEnable(true, GuideFragment.GUIDE_TYPE.PIC_GUIDE);
                mTtileBar.setNavigationClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelGuide();
                        finish();
                    }
                });
            }
        }
    }

    private void cancelGuide() {
        if (mGuideFragment != null) {
            mGuideFragment.setEnable(false, GuideFragment.GUIDE_TYPE.PIC_GUIDE);
            PreferenceTable pre = PreferenceTable.getInstance();
            pre.putBoolean(PrefConst.KEY_PIC_EDIT_GUIDE, true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (mImageLoader != null) {
            mImageLoader.stop();
            mImageLoader.clearMemoryCache();
        }

    }

    @Override
    public void finish() {
        super.finish();
        if (mImageLoader != null) {
            mImageLoader.stop();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View conView, int position, long l) {
        if (mActicityMode == CANCEL_HIDE_MODE && !mIsEditmode
                && mAllListPath.size() > 0) {
            Intent intent = new Intent(ImageGridActivity.this,
                    PictureViewPager.class);
            if (mIsFromIntruderMore) {
                intent.putExtra("fromIntruderMore", true);
            }
            intent.putStringArrayListExtra("list", mAllListPath);
            LeoLog.d("teststartResult", "ready start Activity , list size : " + mAllListPath.size());
            intent.putExtra("pos", position);
            startActivityForResult(intent, 0);
            SDKWrapper.addEvent(ImageGridActivity.this, SDKWrapper.P1,
                    "hide_pic_operation",
                    "pic_viw_cnts");
        } else {
            ImageView cView = (ImageView) conView
                    .findViewById(R.id.photo_select);
            if (!mClickList.contains(mPicturesList.get(position))) {
                cView.setImageResource(R.drawable.ic_check_checked);
                mClickList.add(mPicturesList.get(position));
                mClickPosList.add((Integer) position);
            } else {
                cView.setImageResource(R.drawable.ic_check_normal_n);//pic_choose_normal
                mClickList.remove(mPicturesList.get(position));
                mClickPosList.remove((Integer) position);
            }
            if (mClickList.size() < mPicturesList.size()) {
                mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                        getResources().getDrawable(R.drawable.select_all_selector), null,
                        null);
                mSelectAll.setText(R.string.app_select_all);
            } else {
                mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                        getResources().getDrawable(R.drawable.no_select_all_selector),
                        null,
                        null);
                mSelectAll.setText(R.string.app_select_none);
            }
            updateRightButton();
        }
    }

    public class ImageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mPhotoAibum == null)
                return 0;
            return mPhotoAibum.getBitList().size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.item_grid_image,
                        parent, false);
                holder = new ViewHolder();
                holder.imageView = (ImageView) view.findViewById(R.id.image);
                holder.clickView = (ImageView) view
                        .findViewById(R.id.photo_select);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            if (position < mPicturesList.size()) {
                PhotoItem item = mPicturesList.get(position);
                String path = item.getPath();
                if (mActicityMode == CANCEL_HIDE_MODE && !mIsEditmode) {
                    holder.clickView.setVisibility(View.GONE);
                } else {
                    holder.clickView.setVisibility(View.VISIBLE);
                    if (mClickList.contains(item)) {
                        holder.clickView
                                .setImageResource(R.drawable.ic_check_checked);
                    } else {
                        holder.clickView
                                .setImageResource(R.drawable.ic_check_normal_n);
                    }
                }
                mImageLoader.displayImage("file://" + path, holder.imageView,
                        mOptions);
            }
            return view;
        }

        class ViewHolder {
            ImageView imageView;
            ImageView clickView;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.select_all:
                if (mClickList.size() < mPicturesList.size()) {
                    mClickList.clear();
                    mClickList.addAll(mPicturesList);
                    mSelectAll.setText(R.string.app_select_none);
                    mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.no_select_all_selector), null,
                            null);
                } else {
                    mClickList.clear();
                    mSelectAll.setText(R.string.app_select_all);
                    mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.select_all_selector), null,
                            null);
                }
                updateRightButton();
                mImageAdapter.notifyDataSetChanged();
                break;
            case R.id.hide_image:
                showAlarmDialog();
                break;
            default:
                break;
        }
    }

    private void cancelEditMode() {
        mIsEditmode = false;
        mClickList.clear();
        mImageAdapter.notifyDataSetChanged();
        mBottomBar.setVisibility(View.GONE);
        mTtileBar.setOptionImageResource(R.drawable.edit_mode_name);
        updateRightButton();
    }

//    private class BackgoundTask extends AsyncTask<Boolean, Integer, Integer> {
//        private Context context;
//
//        BackgoundTask(Context context) {
//            this.context = context;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            mIsBackgoundRunning = true;
//        }
//
//        @Override
//        protected Integer doInBackground(Boolean... params) {
//            long a = System.currentTimeMillis();
//            LeoLog.d("testPicLoadTime", "doInBackground");
//
//            String newFileName;
//            int isSuccess = 3;
//            boolean isHide = params[0];
//            try {
//                if (mClickList != null && mClickList.size() > 0) {
//                    ArrayList<PhotoItem> list = (ArrayList<PhotoItem>) mClickList.clone();
//                    Iterator<PhotoItem> iterator = list.iterator();
//                    PhotoItem item;
//                    ArrayList<PhotoItem> deleteList = new ArrayList<PhotoItem>();
//                    ((PrivacyDataManager) MgrContext.getManager
//                            (MgrContext.MGR_PRIVACY_DATA)).unregisterMediaListener();
//                    if (isHide) {
//                        while (iterator.hasNext()) {
//                            item = iterator.next();
//                            if (!mIsBackgoundRunning)
//                                break;
//
//                            String newPath = ((PrivacyDataManager) MgrContext.
//                                    getManager(MgrContext.MGR_PRIVACY_DATA)).
//                                    onHidePic(item.getPath(), "");
//
////                            newFileName = FileOperationUtil
////                                    .getNameFromFilepath(item.getPath());
////                            newFileName = newFileName + ".leotmi";
////                            String path = item.getPath();
////                            String newPath = FileOperationUtil.hideImageFile(context,
////                                    path, newFileName, mTotalSize);
//
//                            if (newPath != null) {
//                                if ("-2".equals(newPath)) {
//                                    isSuccess = -2;
////                                    Log.d("com.leo.appmaster.imagehide.ImageGridActivity",
////                                            "Hide rename image fail!");
//                                } else if ("0".equals(newPath)) {
//                                    isSuccess = 0;
//                                    // mPicturesList.remove(item);
//                                    // mAllListPath.remove(item.getPath());
//                                    deleteList.add(item);
//                                } else if ("-1".equals(newPath)) {
//                                    isSuccess = -1;
////                                    Log.d("com.leo.appmaster.imagehide.ImageGridActivity",
////                                            "Copy image fail!");
//                                } else if ("4".equals(newPath)) {
//                                    isSuccess = 4;
//                                    break;
//                                } else {
//                                    isSuccess = 3;
//                                    FileOperationUtil.saveFileMediaEntry(newPath,
//                                            context);
//                                    File file = new File(item.getPath());
//                                    if (!file.exists()) {
//                                        FileOperationUtil.deleteImageMediaEntry(item.getPath(), context);
//                                    }
//                                    // mPicturesList.remove(item);
//                                    // mAllListPath.remove(item.getPath());
//                                    deleteList.add(item);
//                                }
//                            } else {
//                                isSuccess = 2;
//                            }
//                        }
//                        if (deleteList.size() > 0) {
//                            mPicturesList.removeAll(deleteList);
//                            for (PhotoItem photoItem : deleteList) {
//                                mAllListPath.remove(photoItem.getPath());
//                            }
//                        }
//                    } else {
//                        long b = System.currentTimeMillis();
//                        LeoLog.d("testPicLoadTime", "ready cancel hide : " + (b - a));
//                        while (iterator.hasNext()) {
//                            long a1 = System.currentTimeMillis();
//                            LeoLog.d("testPicLoadTime", "ready operation");
//                            item = iterator.next();
//                            if (!mIsBackgoundRunning)
//                                break;
//
//                            String filepath = item.getPath();
//                            String newPaht = ((PrivacyDataManager) MgrContext.getManager
//                                    (MgrContext.MGR_PRIVACY_DATA)).cancelHidePic(filepath);
//
//                            long a2 = System.currentTimeMillis();
//                            LeoLog.d("testPicLoadTime", "operation1:" + (a2 - a1));
//
//                            if (newPaht == null) {
//                                isSuccess = 2;
//                            } else if ("-1".equals(newPaht) || "-2".equals(newPaht)) {
//                                //Copy Hide image fail!
//                                isSuccess = 2;
//                            } else if ("0".equals(newPaht)) {
//                                isSuccess = 3;
//                                ContentValues values = new ContentValues();
//                                values.put("image_path", filepath);
//                                getContentResolver().insert(Constants.IMAGE_HIDE_URI, values);
//                                // mPicturesList.remove(item);
//                                // mAllListPath.remove(item.getPath());
//                                deleteList.add(item);
//                            } else if ("4".equals(newPaht)) {
//                                isSuccess = 4;
//                                break;
//                            } else {
//                                isSuccess = 3;
//                                FileOperationUtil.saveImageMediaEntry(newPaht, context);
//                                FileOperationUtil.deleteFileMediaEntry(filepath, context);
//                                // mPicturesList.remove(item);
//                                // mAllListPath.remove(item.getPath());
//                                deleteList.add(item);
//                            }
//                            long a3 = System.currentTimeMillis();
//                            LeoLog.d("testPicLoadTime", "operation2:" + (a3 - a2));
//                        }
//                        long c = System.currentTimeMillis();
//                        LeoLog.d("testPicLoadTime", "finish operation : " + (c - b));
//                        if (deleteList.size() > 0) {
//                            mPicturesList.removeAll(deleteList);
//                            for (PhotoItem photoItem : deleteList) {
//                                mAllListPath.remove(photoItem.getPath());
//                            }
//
//                        }
//                    }
//                    ((PrivacyDataManager) MgrContext.getManager
//                            (MgrContext.MGR_PRIVACY_DATA)).registerMediaListener();
//                    //refresh by itself
//                    PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
//                    pdm.notifySecurityChange();
//                }
//                long d = System.currentTimeMillis();
//                LeoLog.d("testPicLoadTime", "total cost : " + (d - a));
//            } catch (Exception e) {
//            }
//            return isSuccess;
//        }
//
//        @Override
//        protected void onPostExecute(final Integer isSuccess) {
//            mClickList.clear();
//            if (isSuccess == 0) {
//                String title = getString(R.string.no_image_hide_dialog_title);
//                String content = getString(R.string.no_image_hide_dialog_content);
//                String rightBtn = getString(R.string.no_image_hide_dialog_button);
//                float width = getResources().getDimension(R.dimen.memery_dialog_button_width);
//                float height = getResources().getDimension(R.dimen.memery_dialog_button_height);
//                showMemeryAlarmDialog(title, content, null, rightBtn, false, true, width, height);
//            } else if (isSuccess == -1 || isSuccess == -2) {
////                Log.d("com.leo.appmaster.imagehide.ImageGridActivity", "Copy Hide  image fail!");
//            } else if (isSuccess == 2) {
////                Log.d("com.leo.appmaster.imagehide.ImageGridActivity", "Hide  image fail!");
//            } else if (isSuccess == 4) {
//                if (mActicityMode == SELECT_HIDE_MODE) {
//                    String title = getString(R.string.image_hide_memery_insuficient_dialog_title);
//                    String content = getString(R.string.image_hide_memery_insuficient_dialog_content);
//                    String rightBtn = getString(R.string.image_hide_memery_insuficient_dialog_button);
//                    float width = getResources().getDimension(
//                            R.dimen.memery_dialog_button_width);
//                    float height = getResources().getDimension(
//                            R.dimen.memery_dialog_button_height);
//                    showMemeryAlarmDialog(title, content, null, rightBtn, false, true,
//                            width, height);
//                } else if (mActicityMode == CANCEL_HIDE_MODE) {
//                    String title = getString(R.string.image_hide_memery_insuficient_dialog_title);
//                    String content = getString(R.string.image_unhide_memery_insuficient_dialog_content);
//                    String rightBtn = getString(R.string.image_hide_memery_insuficient_dialog_button);
//                    float width = getResources().getDimension(
//                            R.dimen.memery_dialog_button_width);
//                    float height = getResources().getDimension(
//                            R.dimen.memery_dialog_button_height);
//                    showMemeryAlarmDialog(title, content, null, rightBtn, false, true,
//                            width, height);
//                }
//            }
//            dismissProgressDialog();
//            if (mPicturesList.size() > 0) {
//                animateReorder();
//                updateRightButton();
//            } else {
//                if (isSuccess == 0) {
//                    if (mImageAdapter != null) {
//                        mImageAdapter.notifyDataSetChanged();
//                    }
//                } else {
//                    finish();
//                }
//            }
//        }
//    }

    private void animateReorder() {
        int length = mClickPosList.size();
        List<Animator> resultList = new LinkedList<Animator>();
        int fistVisblePos = mGridView.getFirstVisiblePosition();
        int lastVisblePos = mGridView.getLastVisiblePosition();
        int pos;
        final List<Integer> viewList = new LinkedList<Integer>();
        for (int i = 0; i < length; i++) {
            pos = mClickPosList.get(i);
            if (pos >= fistVisblePos && pos <= lastVisblePos) {
                View view = mGridView.getChildAt(pos - fistVisblePos);
                viewList.add((Integer) (pos - fistVisblePos));
                resultList.add(createZoomAnimations(view));
            }
        }

        AnimatorSet resultSet = new AnimatorSet();
        resultSet.playTogether(resultList);
        resultSet.setDuration(500);
        resultSet.setInterpolator(new AccelerateDecelerateInterpolator());
        resultSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mImageAdapter.notifyDataSetChanged();
                for (Integer view : viewList) {
                    View child = mGridView.getChildAt(view);
                    if (child != null) {
                        child.setAlpha(1);
                        child.setScaleX(1);
                        child.setScaleY(1);
                    }
                }
                mClickPosList.clear();
            }
        });
        resultSet.start();
    }

    private Animator createZoomAnimations(View view) {
        ObjectAnimator scaleX = ObjectAnimator
                .ofFloat(view, "scaleX", 1f, 0.5f);
        ObjectAnimator scaleY = ObjectAnimator
                .ofFloat(view, "scaleY", 1f, 0.5f);
        ObjectAnimator zoomIn = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        AnimatorSet animZoom = new AnimatorSet();
        animZoom.playTogether(scaleX, scaleY, zoomIn);
        return animZoom;
    }

    private AnimatorSet createTranslationAnimations(View view, float startX,
                                                    float endX, float startY, float endY) {
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX",
                startX, endX);
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY",
                startY, endY);
        AnimatorSet animSetXY = new AnimatorSet();
        animSetXY.playTogether(animX, animY);
        return animSetXY;
    }

    private void showProgressDialog(String title, String message,
                                    boolean indeterminate, boolean cancelable) {
        if (mProgressDialog == null) {
            mProgressDialog = new LEOCircleProgressDialog(this);
            mProgressDialog.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    mIsBackgoundRunning = false;
                    mSelectAll.setText(R.string.app_select_all);
                    mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.select_all_selector), null,
                            null);
                    SDKWrapper.addEvent(ImageGridActivity.this,
                            SDKWrapper.P1, "hide_pic_operation", "pic_ccl_cnts");
                }
            });
        }
        mProgressDialog.setCancelable(cancelable);
        mProgressDialog.setButtonVisiable(cancelable);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setIndeterminate(indeterminate);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        // AM-737
        try {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        } catch (Exception e) {

        }

    }

    private void showAlarmDialog() {
        if (mDialog == null) {
            mDialog = new LEOAlarmDialog(this);
        }
        mDialog.setOnClickListener(new OnDiaogClickListener() {
            @Override
            public void onClick(int which) {
                if (which == 1) {
                    int size = mClickList.size();
                    if (size > 0) {
                        if (mActicityMode == SELECT_HIDE_MODE) {
                            showProgressDialog(getString(R.string.tips),
                                    getString(R.string.app_hide_image) + "...",
                                    true, true);
//                            BackgoundTask task = new BackgoundTask(
//                                    ImageGridActivity.this);
//                            task.execute(true);
                            doingBackGround(true);
                            SDKWrapper.addEvent(ImageGridActivity.this,
                                    SDKWrapper.P1, "hide_pic", "used");
                            SDKWrapper.addEvent(ImageGridActivity.this,
                                    SDKWrapper.P1, "hide_pic_operation", "pic_add_cnts");
                            SDKWrapper.addEvent(ImageGridActivity.this,
                                    SDKWrapper.P1, "hide_pic_operation", "pic_add_pics_" + size);
                        } else if (mActicityMode == CANCEL_HIDE_MODE) {
                            showProgressDialog(getString(R.string.tips),
                                    getString(R.string.app_cancel_hide_image)
                                            + "...", true, true);
//                            BackgoundTask task = new BackgoundTask(
//                                    ImageGridActivity.this);
//                            task.execute(false);
                            doingBackGround(false);
                            SDKWrapper.addEvent(ImageGridActivity.this,
                                    SDKWrapper.P1, "hide_pic_operation", "pic_ccl_pics_" + size);
                        }
                    }
                }

            }
        });
        mDialog.setCanceledOnTouchOutside(false);
        if (mActicityMode == SELECT_HIDE_MODE) {
            mDialog.setTitle(R.string.app_hide_image);
            mDialog.setContent(getString(R.string.app_hide_pictures_dialog_content));
        } else if (mActicityMode == CANCEL_HIDE_MODE) {
            mDialog.setTitle(R.string.app_cancel_hide_image);
            mDialog.setContent(getString(R.string.app_unhide_pictures_dialog_content));
        }
        mDialog.show();
    }

    private void startDoingBack(boolean isHide) {
        LeoLog.d("testnewLoad", "isHide:" + isHide);
        PrivacyDataManager pdm = (PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA);
        int isSuccess = 3;
        try {
            if (mClickList != null && mClickList.size() > 0) {
                ArrayList<PhotoItem> list = (ArrayList<PhotoItem>) mClickList.clone();
                Iterator<PhotoItem> iterator = list.iterator();
                PhotoItem item;
                ArrayList<PhotoItem> deleteList = new ArrayList<PhotoItem>();
                pdm.unregisterMediaListener();
                if (isHide) {
                    while (iterator.hasNext()) {
                        item = iterator.next();
                        if (!mIsBackgoundRunning)
                            break;
                        LeoLog.d("testHidePic", "path : " + item.getPath());
                        String newPath = ((PrivacyDataManager) MgrContext.
                                getManager(MgrContext.MGR_PRIVACY_DATA)).
                                onHidePic(item.getPath(), "");
                        LeoLog.d("testHidePic", "result : " + newPath);
                        LeoLog.d("testHidePic", "---------------------------------------");
                        if (newPath != null) {
                            if ("-2".equals(newPath)) {
                                isSuccess = -2;
                            } else if ("0".equals(newPath)) {
                                isSuccess = 0;
                                deleteList.add(item);
                            } else if ("-1".equals(newPath)) {
                                isSuccess = -1;
                            } else if ("4".equals(newPath)) {
                                isSuccess = 4;
                                break;
                            } else {
                                isSuccess = 3;
                                FileOperationUtil.saveFileMediaEntry(newPath,
                                        this);
                                File file = new File(item.getPath());
                                if (!file.exists()) {
                                    FileOperationUtil.deleteImageMediaEntry(item.getPath(), this);
                                }
                                deleteList.add(item);
                            }
                        } else {
                            isSuccess = 2;
                        }
                    }
                    if (deleteList.size() > 0) {
                        mPicturesList.removeAll(deleteList);
                        for (PhotoItem photoItem : deleteList) {
                            mAllListPath.remove(photoItem.getPath());
                        }
                    }
                } else {
                    while (iterator.hasNext()) {
                        item = iterator.next();
                        if (!mIsBackgoundRunning)
                            break;

                        String filepath = item.getPath();
                        LeoLog.d("testHidePic", "filepath : " + filepath);
                        String newPaht = ((PrivacyDataManager) MgrContext.getManager
                                (MgrContext.MGR_PRIVACY_DATA)).cancelHidePic(filepath);
                        LeoLog.d("testHidePic", "result : " + newPaht);
                        LeoLog.d("testHidePic", "---------------------------------------");
                        if (newPaht == null) {
                            isSuccess = 2;
                        } else if ("-1".equals(newPaht) || "-2".equals(newPaht)) {
                            isSuccess = 2;
                        } else if ("0".equals(newPaht)) {
                            isSuccess = 3;
                            ContentValues values = new ContentValues();
                            values.put("image_path", filepath);
                            getContentResolver().insert(Constants.IMAGE_HIDE_URI, values);
                            deleteList.add(item);
                        } else if ("4".equals(newPaht)) {
                            isSuccess = 4;
                            break;
                        } else {
                            isSuccess = 3;
                            FileOperationUtil.saveImageMediaEntry(newPaht, this);
                            FileOperationUtil.deleteFileMediaEntry(filepath, this);
                            deleteList.add(item);
                        }
                    }
                    if (deleteList.size() > 0) {
                        mPicturesList.removeAll(deleteList);
                        for (PhotoItem photoItem : deleteList) {
                            mAllListPath.remove(photoItem.getPath());
                        }

                    }
                }

                pdm.registerMediaListener();
                pdm.notifySecurityChange();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        readyDoingDone(isSuccess);
    }

    private void readyDoingDone(int isSuccess) {
        if (mHandler != null) {
            Message msg = new Message();
            msg.obj = isSuccess;
            msg.what = HIDE_FINISH;
            mHandler.sendMessage(msg);
        }
    }

    private void onPostDo(int isSuccess) {
        LeoLog.d("testnewLoad", "onPostDo isSuccess:" + isSuccess);
        mClickList.clear();
        if (isSuccess == 0) {
            String title = getString(R.string.no_image_hide_dialog_title);
            String content = getString(R.string.no_image_hide_dialog_content);
            String rightBtn = getString(R.string.no_image_hide_dialog_button);
            float width = getResources().getDimension(R.dimen.memery_dialog_button_width);
            float height = getResources().getDimension(R.dimen.memery_dialog_button_height);
            showMemeryAlarmDialog(title, content, null, rightBtn, false, true, width, height);
        } else if (isSuccess == -1 || isSuccess == -2) {
        } else if (isSuccess == 2) {
        } else if (isSuccess == 4) {
            if (mActicityMode == SELECT_HIDE_MODE) {
                String title = getString(R.string.image_hide_memery_insuficient_dialog_title);
                String content = getString(R.string.image_hide_memery_insuficient_dialog_content);
                String rightBtn = getString(R.string.image_hide_memery_insuficient_dialog_button);
                float width = getResources().getDimension(
                        R.dimen.memery_dialog_button_width);
                float height = getResources().getDimension(
                        R.dimen.memery_dialog_button_height);
                showMemeryAlarmDialog(title, content, null, rightBtn, false, true,
                        width, height);
            } else if (mActicityMode == CANCEL_HIDE_MODE) {
                String title = getString(R.string.image_hide_memery_insuficient_dialog_title);
                String content = getString(R.string.image_unhide_memery_insuficient_dialog_content);
                String rightBtn = getString(R.string.image_hide_memery_insuficient_dialog_button);
                float width = getResources().getDimension(
                        R.dimen.memery_dialog_button_width);
                float height = getResources().getDimension(
                        R.dimen.memery_dialog_button_height);
                showMemeryAlarmDialog(title, content, null, rightBtn, false, true,
                        width, height);
            }
        }
        dismissProgressDialog();
        if (mPicturesList.size() > 0) {
            animateReorder();
            updateRightButton();
        } else {
            if (isSuccess == 0) {
                if (mImageAdapter != null) {
                    mImageAdapter.notifyDataSetChanged();
                }
            } else {
                finish();
            }
        }
    }

    private void doingBackGround(boolean isHidePic) {
        onPreDo();
        readyDoingBack(isHidePic);
    }

    private void readyDoingBack(boolean isHidePic) {
        if (mHandler != null) {
            Message msg = new Message();
            msg.obj = isHidePic;
            msg.what = START_CANCEL_OR_HIDE_PIC;
            mHandler.sendMessage(msg);
        }
    }

    private void onPreDo() {
        mIsBackgoundRunning = true;
    }

    private void loadImageList() {
        if (mPhotoAibum == null) {
            mLoadingBar.setVisibility(View.VISIBLE);
            mGridView.setVisibility(View.GONE);
            mHandler.sendEmptyMessage(INIT_UI_DONE);
        } else {
            mLoadingBar.setVisibility(View.GONE);
            mGridView.setVisibility(View.VISIBLE);
            mImageAdapter = new ImageAdapter();
            mGridView.setAdapter(mImageAdapter);
            mSelectAll.setEnabled(true);
        }
    }

    private void showMemeryAlarmDialog(String title, String content, String leftBtn,
                                       String rightBtn, boolean isLeft, boolean isRight, float width, float height) {
        if (memeryDialog == null) {
            memeryDialog = new OneButtonDialog(this);
        }
        memeryDialog.setRightBtnListener(new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 1) {

                    mIsBackgoundRunning = false;
                    mSelectAll.setText(R.string.app_select_all);
                    mSelectAll.setCompoundDrawablesWithIntrinsicBounds(null,
                            getResources().getDrawable(R.drawable.select_all_selector), null,
                            null);

//                    mSelectAll.setText(R.string.app_select_all);
                    if (mPicturesList.size() <= 0) {
                        finish();
                    }
                }
                memeryDialog.dismiss();
            }
        });

        memeryDialog.setCanceledOnTouchOutside(false);
        memeryDialog.setTitle(title);
        memeryDialog.setText(content);
        memeryDialog.setBtnText(rightBtn);
        memeryDialog.show();
    }

}
