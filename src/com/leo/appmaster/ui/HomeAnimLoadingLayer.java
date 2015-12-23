package com.leo.appmaster.ui;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.leo.appmaster.R;

/**
 * Created by Jasper on 2015/10/23.
 */
public class HomeAnimLoadingLayer extends AnimLayer {
    private static final int ROTATE_INTERVAL = 4;

    public static final int LOAD_NONE = -1;
    public static final int LOAD_LOCK_APP = 1;
    public static final int LOAD_HIDE_PIC = 2;
    public static final int LOAD_HIDE_VID = 3;

    private Drawable mLockIcon;
    private Drawable mHideIcon;

    private int mLoadingSize;
    private Rect mLoadingBounds;
    private int mBaseLine;

    private int mRotateAngle;

    private Paint mPaint;
    private Paint mTextPaint;

    private int mLoadType = LOAD_LOCK_APP;

    private int mRiseHeight;
    private int mMaxRiseHeight;

    HomeAnimLoadingLayer(View view) {
        super(view);

        Resources res = view.getResources();
        mLockIcon = res.getDrawable(R.drawable.ic_privacy_locking);
        mHideIcon = res.getDrawable(R.drawable.ic_privacy_hiding);

        mMaxRiseHeight = res.getDimensionPixelSize(R.dimen.home_loading_rise);
        mLoadingSize = res.getDimensionPixelSize(R.dimen.home_privacy_loading);
        mLoadingBounds = new Rect();

        int stroke = res.getDimensionPixelSize(R.dimen.home_loading_stroke);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(res.getColor(R.color.white));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(stroke);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(res.getColor(R.color.white));
        mTextPaint.setTextSize(res.getDimensionPixelSize(R.dimen.home_loading));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged() {
        super.onSizeChanged();
        int iconTop = (int) (((float)getHeight()) * 0.6f);

        int iconW = mLockIcon.getIntrinsicWidth();
        int iconH = mLockIcon.getIntrinsicHeight();

        int iconL = (getWidth() - iconW) / 2;
        mLockIcon.setBounds(iconL, iconTop, iconL + iconW, iconTop + iconH);
        mHideIcon.setBounds(iconL, iconTop, iconL + iconW, iconTop + iconH);

        int loadingL = iconL - (mLoadingSize - iconW) / 2;
        int loadingT = iconTop - (mLoadingSize - iconH) / 2;

        mLoadingBounds.set(loadingL, loadingT, loadingL + mLoadingSize, loadingT + mLoadingSize);

        int margin = mParent.getResources().getDimensionPixelSize(R.dimen.loading_baseline_margin);
        mBaseLine = mLoadingBounds.bottom + margin;
    }

    @Override
    protected void draw(Canvas canvas) {
        float centerX = mLoadingBounds.centerX();
        float centerY = mLoadingBounds.centerY();

        int riseHeight = mRiseHeight;
        float riseRatio = ((float)riseHeight) / ((float)mMaxRiseHeight);
        canvas.save();
        if (riseHeight > 0) {
            canvas.translate(0, -riseHeight);
            float scale = 1f - riseRatio / 2f;
            canvas.scale(scale, scale, centerX, centerY);
        }
        Drawable drawable = getLoadingDrawable();
        if (drawable != null) {
            drawable.draw(canvas);
        }

        int rotate = mRotateAngle;
        canvas.rotate(rotate, centerX, centerY);

        int sweep = rotate * 2;
        if (rotate > 180) {
            sweep = 360 - (rotate - 180) * 2;
        }

        RectF r = new RectF(mLoadingBounds.left, mLoadingBounds.top, mLoadingBounds.right, mLoadingBounds.bottom);
        if (riseHeight == 0) {
            canvas.drawArc(r, rotate, sweep, false, mPaint);
        } else {
            canvas.drawArc(r, 0, 360, false, mPaint);
        }

        mRotateAngle += ROTATE_INTERVAL;
        if (mRotateAngle > 360) {
            mRotateAngle = 0;
        }
        canvas.restore();

        if (riseHeight > 0) {
            int alpha = (int) (255f * (1f - riseRatio));
            mTextPaint.setAlpha(alpha);
        }
        canvas.drawText(getLoadingTip(), centerX, mBaseLine, mTextPaint);
        mTextPaint.setAlpha(255);

        mParent.invalidate();
    }

    public void setLoadType(int loadType) {
        mLoadType = loadType;
        mRiseHeight = 0;
        mParent.invalidate();
    }

    /**
     * 设置上升距离
     * @param riseHeight
     */
    public void setRiseHeight(int riseHeight) {
        mRiseHeight = riseHeight;
        mParent.invalidate();
    }

    private Drawable getLoadingDrawable() {
        Drawable drawable = null;
        if (mLoadType == LOAD_LOCK_APP) {
            drawable = mLockIcon;
        } else if (mLoadType == LOAD_HIDE_PIC || mLoadType == LOAD_HIDE_VID) {
            drawable = mHideIcon;
        }

        return drawable;
    }

    private String getLoadingTip() {
        String tips = "";
        int id = 0;
        if (mLoadType == LOAD_LOCK_APP) {
            id = R.string.loading_locking;
        } else if (mLoadType == LOAD_HIDE_PIC) {
            id = R.string.loading_hiding_pic;
        } else if (mLoadType == LOAD_HIDE_VID) {
            id = R.string.loading_hiding_vid;
        }

        if (id != 0) {
            tips = mParent.getResources().getString(id);
        }

        return tips;
    }
}
