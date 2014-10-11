package com.leo.appmaster.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TimeView extends View {

	private float mCurDegree;
	private Paint mPaint;

	private RectF mRect;

	public TimeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		init();
		super.onFinishInflate();
	}

	private void init() {
		mCurDegree = 360;
		mPaint = new Paint();
		mPaint.setColor(0xaaFFFFFF);
		mPaint.setStyle(Paint.Style.FILL);
	}

	@Override
	protected void onDraw(Canvas canvas) {

		if (mRect == null) {
			mRect = new RectF();

			int width = this.getMeasuredWidth();
			int height = this.getMeasuredHeight();

			mRect.left = width * 0.115f;
			mRect.top = height * 0.115f;
			mRect.right = width * 0.885f;
			mRect.bottom = height * 0.885f;
		}

		canvas.drawArc(mRect, -90f, mCurDegree, true, mPaint);

		super.onDraw(canvas);
	}

	public void updateDegree(float d) {
		mCurDegree = d;
		postInvalidate();
	}
}
