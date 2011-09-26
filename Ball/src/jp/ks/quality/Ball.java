package jp.ks.quality;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class Ball {
	// ボール画像
	private Drawable mDrawable;
	private int mWidth;
	private int mHeight;
	private Rect mRect;
	
	// 表示範囲
	private Rect mViewRect;
	
	// 移動量
	private double mDx;
	private double mDy;
	
	// 加速量
	private final double mAcceleration = 1.5;
	
	// 減衰量
	private final double mAttenuator = 0.02;
	
	// 壁のはね返り量
	private final double mBound = 0.5;
	
	public Ball(Drawable drawable, Rect viewRect) {
		// 画像を設定
		mDrawable = drawable;
	    mWidth = drawable.getIntrinsicWidth() / 2;
	    mHeight = drawable.getIntrinsicHeight() / 2;
	    mRect = new Rect(0, 0, mWidth, mHeight);
	    
	    // 表示範囲を設定
	    mViewRect = viewRect;
	    
	    // 初期位置を中央に設定
	    mRect.offset(
	    	(mViewRect.right - mViewRect.left - mWidth) / 2, 
	    	(mViewRect.bottom - mViewRect.top - mHeight) / 2);
	    
	    // 移動量を初期化
	    mDx = 0;
	    mDy = 0;
	}
	
	public void move(float mPitch, float mRoll) {
		// 傾きから移動量の変動幅を計算
		double dx = - Math.sin(Math.toRadians(mRoll))
			* mAcceleration;
		double dy = - Math.sin(Math.toRadians(mPitch))
			* mAcceleration;
		
		// 傾きによって移動量を修正
		mDx += mDx * -mAttenuator + dx;
		mDy += mDy * -mAttenuator + dy;
		
		// ボールを移動
	    mRect.offset((int)mDx, (int)mDy);
	    
	    // 表示範囲の枠に当たったらはね返す
	    if (mRect.left < 0) {
	        mDx = -mDx * mBound;
	        mRect.left = 0;
	        mRect.right = mRect.left + mWidth;
	    } else if (mRect.top < 0) {
	        mDy = -mDy * mBound;
	        mRect.top = 0;
	        mRect.bottom = mRect.top + mHeight;
	    } else if (mRect.bottom > mViewRect.bottom) {
	        mDy = -mDy * mBound;
	        mRect.bottom = mViewRect.bottom;
	        mRect.top = mRect.bottom - mHeight;
	    } else if (mRect.right > mViewRect.right) {
	        mDx = -mDx * mBound;
	        mRect.right = mViewRect.right;
	        mRect.left = mRect.right - mWidth;
	    }
	}
	  
	public void draw(Canvas canvas) {
		mDrawable.setBounds(mRect);
		mDrawable.draw(canvas); 
	}
}
