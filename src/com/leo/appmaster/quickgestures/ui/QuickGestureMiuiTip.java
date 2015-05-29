
package com.leo.appmaster.quickgestures.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;

import com.leo.appmaster.AppMasterPreference;
import com.leo.appmaster.R;
import com.leo.appmaster.sdk.BaseActivity;

/**
 * QuickGestureMiuiTip
 * 
 * @author run
 */
public class QuickGestureMiuiTip extends BaseActivity implements OnClickListener {
    private RelativeLayout mMiuiTipRL;
    private boolean checkHuaWeiSys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = this.getIntent();
        String sysName = intent.getStringExtra("sys_name");
        if ("huawei".equals(sysName)) {
            checkHuaWeiSys = true;
        }
        setContentView(R.layout.activity_miui_open_float_window_tip);
        mMiuiTipRL = (RelativeLayout) findViewById(R.id.miui_tipRL);
        mMiuiTipRL.setOnClickListener(this);
    }

    @Override
    public void onClick(View arg0) {
        AppMasterPreference pref = AppMasterPreference.getInstance(QuickGestureMiuiTip.this);
        boolean miuiSetFirst = pref.getQuickGestureMiuiSettingFirstDialogTip();
        if (!miuiSetFirst) {
            AppMasterPreference.getInstance(QuickGestureMiuiTip.this)
                    .setQuickGestureMiuiSettingFirstDialogTip(true);
        }
        if (!checkHuaWeiSys) {
            mMiuiTipRL.postDelayed(new Runnable() {
                @Override
                public void run() {
                    System.exit((int) (0));
                }
            }, 3000);
        }
        QuickGestureMiuiTip.this.finish();
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
    }
}
