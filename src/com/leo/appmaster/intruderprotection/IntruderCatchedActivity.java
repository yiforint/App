
package com.leo.appmaster.intruderprotection;


import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.leo.appmaster.Constants;
import com.leo.appmaster.R;
import com.leo.appmaster.ThreadManager;
import com.leo.appmaster.applocker.IntruderPhotoInfo;
import com.leo.appmaster.applocker.LockScreenActivity;
import com.leo.appmaster.applocker.manager.MobvistaEngine;
import com.leo.appmaster.db.PreferenceTable;
import com.leo.appmaster.home.HomeActivity;
import com.leo.appmaster.imagehide.ImageGridActivity;
import com.leo.appmaster.imagehide.ImageHideMainActivity;
import com.leo.appmaster.imagehide.PhotoAibum;
import com.leo.appmaster.mgr.IntrudeSecurityManager;
import com.leo.appmaster.mgr.MgrContext;
import com.leo.appmaster.mgr.PrivacyDataManager;
import com.leo.appmaster.sdk.BaseActivity;
import com.leo.appmaster.sdk.SDKWrapper;
import com.leo.appmaster.ui.RippleView;
import com.leo.appmaster.ui.RippleView.OnRippleCompleteListener;
import com.leo.appmaster.ui.dialog.LEOChoiceDialog;
import com.leo.appmaster.utils.AppUtil;
import com.leo.appmaster.utils.DipPixelUtil;
import com.leo.appmaster.utils.LeoLog;
import com.leo.appmaster.utils.PrefConst;
import com.leo.appmaster.utils.Utilities;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.core.FailReason;
import com.leo.imageloader.core.ImageLoadingListener;

public class IntruderCatchedActivity extends BaseActivity {
    private List<PhotoAibum> mAlbumList = null;
    private Button mBtFiveStars;
    private ImageLoader mImageLoader;
    private String mPkgName;
    private ListView mLvMain;
    private ArrayList<IntruderPhotoInfo> mSrcInfos;
    private ArrayList<IntruderPhotoInfo> mInfosSorted;
    private IntrudeSecurityManager mISManager;
    private final int NEWEST_PHOTO_NUMBER = 3;
    private Button mBtMore;
    private BottomCropImage mIvNewestPhoto;
    private LinearLayout mLlMainMask;
    private TextView mTvNewestCatchTip;
    private ImageView mIvAppIntruded;
    private ScrollView mSvMain;
    private TextView mTvTimesToCatch;
    private Button mChangeTimes;
    private TextView mTvTotalTimes;
    private ImageView mClose;
    private MobvistaEngine mAdEngine;
    private RelativeLayout mADLayout;
    private LinearLayout mLlFiveStars;
    private RelativeLayout mRlNopic;
    private RippleView mRvClose;
    private TextView mTvOthers;
    private LEOChoiceDialog mDialog;
    private RelativeLayout mRvHeader;
    private RippleView mRvRating;
    private int[] mTimes = {1 , 2 , 3 , 5};
    private List<Bitmap> mBitmaps = new ArrayList<Bitmap>();
    private DisplayImageOptions mImageOptions;
    private RippleView mRvChange;
    private RippleView mRvMore;
    private RelativeLayout mRlNewest;
    private PreferenceTable mPt;
    private static final int TIMES_TO_CATCH_1 = 1;
    private static final int TIMES_TO_CATCH_2 = 2;
    private static final int TIMES_TO_CATCH_3 = 3;
    private static final int TIMES_TO_CATCH_4 = 5;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catch_intruder);
        Intent intent = getIntent();
        mPt = PreferenceTable.getInstance();
        mISManager = (IntrudeSecurityManager) MgrContext.getManager(MgrContext.MGR_INTRUDE_SECURITY);
        mPkgName = intent.getStringExtra("pkgname");
        mLockManager.filterPackage(mPkgName, 5000);
        init();
//        toLoad();
    }
    
//    private void toLoad() {
//        int showADorEvaluate = mISManager.getShowADorEvaluate();
//        if (showADorEvaluate == 1) {
//            loadAD();
//        }
//    }
    
    @Override
    protected void onResume() {
        super.onResume();
        SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                "intruder", "intruder_capture");
        mPt.putBoolean(PrefConst.KEY_IS_DELAY_TO_SHOW_CATCH , false);
//        if(getIntent().getBooleanExtra("isClear", false)){
//            this.finish();
//        }
        updateData();// 重新查询数据库，做与数据相关的UI更新
        updateAll();// 更新与数据库无关的UI
    }
    
    @Override
    public void onBackPressed() {
        if(getPackageName().equals(mPkgName)) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            mLockManager.filterPackage(mPkgName, 2000);
        }
        finish();
    }

    /**
     * 每次进入界面需要重新刷新的操作(与数据库无关)
     */
    private void updateAll() {
        //更新总的抓拍次数
        String times1 = getString(R.string.intruder_times_of_catch);
        String times2 = String.format(times1, mISManager.getCatchTimes());
        mTvTotalTimes.setText(Html.fromHtml(times2));
        //更新抓拍所需的解锁失败次数
        updateTimesToCatch();
    }

    /**
     * 每次进入界面后需要查询数据库然后更新的操作
     */
    private void updateData() {
        ThreadManager.executeOnAsyncThread(new Runnable() {
            @Override
            public void run() {
                mSrcInfos = mISManager.getPhotoInfoList();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onQueryFinished();
                    }
                });
            }
        });
    }
    

//   先别删 如果以后需要广告 直接打开注释就可以了
//    private void loadAD() {
//        LeoLog.e("poha", "loading ad...");
//        mAdEngine = MobvistaEngine.getInstance(this);
//        mAdEngine.loadMobvista(Constants.UNIT_ID_58, new MobvistaListener() {
//            @Override
//            public void onMobvistaFinished(int code, Campaign campaign, String msg) {
//                if (code == MobvistaEngine.ERR_OK && campaign != null) {
//                    ImageView admain = (ImageView) mADLayout.findViewById(R.id.iv_ad_mainpic);
//                    mImageLoader.displayImage(campaign.getImageUrl(), admain);
//                    ImageView adicon = (ImageView) mADLayout.findViewById(R.id.iv_ad_iconpic);
//                    mImageLoader.displayImage(campaign.getIconUrl(), adicon);
//                    TextView appname = (TextView) mADLayout.findViewById(R.id.tv_appname);
//                    appname.setText(campaign.getAppName());
//                    TextView appdesc = (TextView) mADLayout.findViewById(R.id.tv_appdesc);
//                    appdesc.setText(campaign.getAppDesc());
//                    TextView appcall = (TextView) mADLayout.findViewById(R.id.tv_ad_call);
//                    appcall.setText(campaign.getAdCall());
//                    mAdEngine.registerView(Constants.UNIT_ID_58, appcall);
//                    mLlFiveStars.setVisibility(View.GONE);
//                    mADLayout.setVisibility(View.VISIBLE);
//                }
//            }
//            @Override
//            public void onMobvistaClick(Campaign campaign) {
//            }
//        });
//    }.
    private String timeStampToAMPM(String timeStamp){
        SimpleDateFormat sdf = new SimpleDateFormat(
                Constants.INTRUDER_PHOTO_TIMESTAMP_FORMAT);
        Calendar ci = Calendar.getInstance();
        Date date = null;
        int hour = 0;
        int minute = 0;
        int ampm = 0;
        try {
            date = sdf.parse(timeStamp);
            ci.setTime(date);
            hour = ci.get(Calendar.HOUR);
            ampm= ci.get(Calendar.AM_PM);
            minute = ci.get(Calendar.MINUTE);
        } catch (ParseException e1) {
            return timeStamp;
        }
        String strHour = "";
        String strMinute = "";
        if(hour<10){
            strHour = "0"+hour;
        }else{
            strHour = hour+"";
        }
        if(minute<10){
            strMinute = "0"+minute;
        }else{
            strMinute = minute+"";
        }
        String fts;
        if(ampm==Calendar.AM){
            fts = strHour+":"+strMinute+"AM";
        }else if(ampm==Calendar.PM){
            fts = strHour+":"+strMinute+"PM";
        }else{
            fts = strHour+":"+strMinute;
        }
        return fts;
    }

    /**
     * create时的初始化(不需要变化的UI)
     */
    private void init() {
        mRvHeader = (RelativeLayout) findViewById(R.id.rl_header);
        mSvMain = (ScrollView) findViewById(R.id.sv_intrudercatch_main);
        mRlNopic = (RelativeLayout) findViewById(R.id.rl_nopic);
        mLlMainMask = (LinearLayout) findViewById(R.id.ll_main_mask);
        mRvRating = (RippleView) findViewById(R.id.rv_fivestars);
        mRlNewest = (RelativeLayout) findViewById(R.id.rl_newest);
        mTvOthers = (TextView) findViewById(R.id.tv_others);
        mLlFiveStars = (LinearLayout) findViewById(R.id.ll_fivestars_layout);
        mADLayout = (RelativeLayout) findViewById(R.id.rl_ad_layout);
        mClose = (ImageView) findViewById(R.id.iv_close);
        mRvClose = (RippleView) findViewById(R.id.rv_close);
        mRvClose.setOnRippleCompleteListener(new OnRippleCompleteListener() {
            @Override
            public void onRippleComplete(RippleView rippleView) {
                SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                        "intruder", "intruder_capture_quit");
                onBackPressed();
            }
        });
        mTvTotalTimes = (TextView) findViewById(R.id.tv_times_of_catch);
        mChangeTimes = (Button) findViewById(R.id.bt_change_times);
        mRvChange = (RippleView) findViewById(R.id.rv_change_times);
        mRvChange.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeTimesDialog();
            }
        });
        mTvTimesToCatch = (TextView) findViewById(R.id.tv_times_to_catch);
        mBtFiveStars = (Button) findViewById(R.id.bt_beg_fivestars);
        mRvRating.setOnRippleCompleteListener(new OnRippleCompleteListener() {
            @Override
            public void onRippleComplete(RippleView v) {
                SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                        "intruder", "intruder_capture_rank");
                mLockManager.filterSelfOneMinites();
                Utilities.goFiveStar(IntruderCatchedActivity.this);
            }
        });
        //初始化imageloader，需要时可以设置option
        mImageLoader = ImageLoader.getInstance();
        mImageOptions = new DisplayImageOptions.Builder()
        .bitmapConfig(Bitmap.Config.RGB_565)
        .cacheInMemory(true)
        .build();
        
        mIvAppIntruded = (ImageView) findViewById(R.id.iv_app_intruded);
        mTvNewestCatchTip = (TextView) findViewById(R.id.newest_catch_tip);
        mIvNewestPhoto = (BottomCropImage) findViewById(R.id.iv_newest_photo);
        mBtMore = (Button) findViewById(R.id.bt_more);
        mRvMore = (RippleView) findViewById(R.id.rv_more);
        mLvMain = (ListView) findViewById(R.id.lv_mainlist);
    }

    private void showChangeTimesDialog() {
        if(mDialog == null){
            mDialog = new LEOChoiceDialog(IntruderCatchedActivity.this);
        }
        String times = getResources().getString(R.string.times_choose);
        List<String> timesArray = new ArrayList<String>();
        for(int i = 0 ; i < mTimes.length; i++){
            timesArray.add(String.format(times, mTimes[i]));
        }
        
        int currentTimes = mISManager.getTimesForTakePhoto();
        int currentIndex = -1;
        switch (currentTimes) {
            case TIMES_TO_CATCH_1:
                currentIndex = 0;
                break;
            case TIMES_TO_CATCH_2:
                currentIndex = 1;
                break;
            case TIMES_TO_CATCH_3:
                currentIndex = 2;
                break;
            case TIMES_TO_CATCH_4:
                currentIndex = 3;
                break;
            default:
                break;
        }
        mDialog.setTitle(getResources().getString(R.string.ask_for_times_for_catch));
        mDialog.setItemsWithDefaultStyle(timesArray, currentIndex);
        mDialog.getItemsListView().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        mISManager.setTimesForTakePhoto(TIMES_TO_CATCH_1);
                        updateTimesToCatch();
                        break;
                    case 1:
                        mISManager.setTimesForTakePhoto(TIMES_TO_CATCH_2);
                        updateTimesToCatch();
                        break;
                    case 2:
                        mISManager.setTimesForTakePhoto(TIMES_TO_CATCH_3);
                        updateTimesToCatch();
                        break;
                    case 3:
                        mISManager.setTimesForTakePhoto(TIMES_TO_CATCH_4);
                        updateTimesToCatch();
                        break;
                    default:
                        break;
                }
                mDialog.dismiss();
            }
        });
        mDialog.show();
    }

    private void updateTimesToCatch() {
        String string1 = getString(R.string.intruder_to_catch_fail_times_tip);
        String s2 = String.format(string1, mISManager.getTimesForTakePhoto());
        mTvTimesToCatch.setText(Html.fromHtml(s2));
    }

    private void sortInfos() {
        mInfosSorted = mISManager.sortInfosByTimeStamp(mSrcInfos);
    }

    private void onQueryFinished() {
        sortInfos();
        final PackageManager pm = getPackageManager();
        if (mInfosSorted!=null && mInfosSorted.size() != 0 ) {
            mRlNewest.setVisibility(View.VISIBLE);
            mRlNopic.setVisibility(View.INVISIBLE);
            ThreadManager.getUiThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    mImageLoader.loadImage("file:///" + mInfosSorted.get(0).getFilePath(),
                            new ImageLoadingListener() {
                                @Override
                                public void onLoadingStarted(String imageUri, View view) {
                                    LeoLog.i("poha", "starting... 000");
                                }
                                @Override
                                public void onLoadingFailed(String imageUri, View view,
                                        FailReason failReason) {
                                    LeoLog.i("poha", "failed... 000");
                                }
                                @Override
                                public void onLoadingComplete(String imageUri, View view,
                                        Bitmap loadedImage) {
                                    LeoLog.i("poha", "complete... 000");
                                    mIvNewestPhoto.setImageBitmap(loadedImage);
                                    mIvNewestPhoto.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent(IntruderCatchedActivity.this,
                                                    IntruderGalleryActivity.class);
                                            intent.putExtra("current_position", 0);
                                            SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                                                    "intruder", "intruder_view_capture");
                                            startActivity(intent);
                                        }
                                    });
                                    mLlMainMask.setVisibility(view.VISIBLE);
                                    ImageView mainIcon = (ImageView) mLlMainMask.findViewById(R.id.iv_appicon);
                                    Drawable applicationIcon = AppUtil.getAppIcon(pm, mInfosSorted.get(0).getFromAppPackage());
                                    if(applicationIcon != null){
                                        mainIcon.setImageDrawable(applicationIcon);
                                    }
                                    TextView mainTimestamp = (TextView) mLlMainMask.findViewById(R.id.tv_timestamp);
                                    String timeStampToAMPM = timeStampToAMPM(mInfosSorted.get(0).getTimeStamp());
                                    mainTimestamp.setText(timeStampToAMPM);
                                    
                                }
                                @Override
                                public void onLoadingCancelled(String imageUri, View view) {
                                    LeoLog.i("poha", "cancelled... 000");
                                }
                            });
                }
            });
            
         // XX想偷看XXX的文案
            try {
                String packageName = mInfosSorted.get(0).getFromAppPackage();
                Drawable applicationIcon = AppUtil.getAppIcon(pm, packageName);
                mIvAppIntruded.setImageDrawable(applicationIcon);
                CharSequence label = AppUtil.getAppLabel(pm, packageName);
                String newestCatchTipsS = getResources().getString(R.string.newest_catch_tip);
                String newestCatchTipsD = String.format(newestCatchTipsS, label);
                mTvNewestCatchTip.setText(newestCatchTipsD);
            } catch (Exception e) {
            }
        }else{
            mRlNewest.setVisibility(View.INVISIBLE);
            mRlNopic.setVisibility(View.VISIBLE);
        }
        if (mInfosSorted.size() >= 2) {
            mTvOthers.setVisibility(View.VISIBLE);
            mLvMain.setVisibility(View.VISIBLE);
            mLvMain.setAdapter(new BaseAdapter() {
                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    View view = convertView;
                    if(convertView == null) {
                        view = View.inflate(IntruderCatchedActivity.this,
                                R.layout.item_photo_in_catch, null);
                    }
                    final LinearLayout llMask = (LinearLayout) view.findViewById(R.id.ll_mask);
                    final BottomCropImage ivv = (BottomCropImage) view.findViewById(R.id.iv_intruder_more);
                    final String filePath = mInfosSorted.get(position+1).getFilePath();
                    ThreadManager.executeOnAsyncThread(new Runnable() {
                        @Override
                        public void run() {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.RGB_565;
                            final Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ivv.setImageBitmap(bitmap);
                                    ivv.setOnClickListener(new OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Intent intent = new Intent(IntruderCatchedActivity.this,
                                                    IntruderGalleryActivity.class);
                                            intent.putExtra("current_position", position+1);
                                            SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                                                    "intruder", "intruder_view_capture");
                                            startActivity(intent);
                                        }
                                    });
//                                    mRvHeader.setFocusable(true);
//                                    mRvHeader.setFocusableInTouchMode(true);
//                                    mRvHeader.requestFocus();
                                }
                            });
                        }
                    });
                    
                    PackageManager pm = getPackageManager();
                    try {
                        Drawable applicationIcon = AppUtil.getAppIcon(pm, mInfosSorted.get(position+1).getFromAppPackage());
                        ImageView iv2 = (ImageView) (llMask.findViewById(R.id.iv_appicon));
                        iv2.setImageDrawable(applicationIcon);
//                        TextView tv2 = (TextView) (llMask.findViewById(R.id.tv_timestamp));
//                        tv2.setText(mInfosSorted.get(position+1).getTimeStamp());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return view;
                }

                @Override
                public long getItemId(int position) {
                    return 0;
                }

                @Override
                public Object getItem(int position) {
                    return null;
                }

                @Override
                public int getCount() {
                    if (mInfosSorted == null)
                        return 0;
                    return Math.min(NEWEST_PHOTO_NUMBER, mInfosSorted.size() - 1);
                }
            });
        }else{
            mLvMain.setVisibility(View.GONE);
            mTvOthers.setVisibility(View.GONE);
        }
        ListAdapter listAdapter = mLvMain.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int totalHeight = DipPixelUtil.dip2px(IntruderCatchedActivity.this, 140)*listAdapter.getCount();
        ViewGroup.LayoutParams params = mLvMain.getLayoutParams();
        int count = listAdapter.getCount() - 1;
        params.height = totalHeight + (mLvMain.getDividerHeight() * count); 
        mLvMain.setLayoutParams(params);

        if (mInfosSorted.size() > 4) {
            LeoLog.i("poha", "gone or visiable ? mInfosSorted.size = " + mInfosSorted.size());
            mRvMore.setVisibility(View.VISIBLE);
            mRvMore.setOnRippleCompleteListener(new OnRippleCompleteListener() {
                @Override
                public void onRippleComplete(RippleView v) {
                    SDKWrapper.addEvent(IntruderCatchedActivity.this, SDKWrapper.P1,
                            "intruder", "intruder_capture_more");
                    long cc1 = System.currentTimeMillis();
                    File file = new File(mInfosSorted.get(0).getFilePath());
                    String parent = file.getParent();
                    int index = 0;
                    mAlbumList = ((PrivacyDataManager) MgrContext.getManager(MgrContext.MGR_PRIVACY_DATA)).
                            getHidePicAlbum("");
                    for(int i = 0 ; i<mAlbumList.size() ; i++){
                        String dirPath = mAlbumList.get(i).getDirPath();
                        if(parent.equals(dirPath)){
                            index = i;
                            break;
                        }
                    }
                    long cc2 = System.currentTimeMillis();
                    LeoLog.i("catch_poha", "cc2 -cc1 :"+(cc2-cc1));
                    try {
                        Intent intent = new Intent(IntruderCatchedActivity.this,
                                ImageGridActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("data", mAlbumList.get(index));
                        intent.putExtras(bundle);
                        intent.putExtra("fromIntruderMore", true);
                        intent.putExtra("mode", ImageGridActivity.CANCEL_HIDE_MODE);
                        startActivity(intent);
//                        IntruderCatchedActivity.this.finish();
                        LeoLog.i("catch_poha", "cc3 -cc2 :"+(System.currentTimeMillis()-cc2));
                    } catch (Throwable t) {
                        Intent intent = new Intent(IntruderCatchedActivity.this,
                               ImageHideMainActivity .class);
                        startActivity(intent);
                    }
                }
            });
        }else{
            mRvMore.setVisibility(View.GONE);
        }
        mRvHeader.setFocusable(true);
        mRvHeader.setFocusableInTouchMode(true);
        mRvHeader.requestFocus();
    }


    @Override
    protected void onDestroy() {
//        LeoEventBus.getDefaultBus().post(
//                new AppUnlockEvent(mPkgName, AppUnlockEvent.RESULT_UNLOCK_SUCCESSFULLY));
        super.onDestroy();
        if(mImageLoader != null) {
            mImageLoader.clearMemoryCache();
        }
    }
}