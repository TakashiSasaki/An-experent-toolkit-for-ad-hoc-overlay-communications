package jp.ks.quality;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class BallSurfaceView 
	extends SurfaceView 
	implements SurfaceHolder.Callback, Runnable {
	// 表示範囲
	private Rect mRect;
	
	// ボールの画像
	private Drawable mDrawable;
	
	// ボール
	private Ball mBall;

	// 傾きセンサー用の変数
	private SensorManager mSensorManager;
	private float mPitch;
	private float mRoll;

	// SurfaceView用の変数
	private SurfaceHolder mHolder;
	private Thread mThread;

    public BallSurfaceView(Context context) {
	    super(context);
	    initialize(context);
    }

    public BallSurfaceView(
    	Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	    initialize(context);
    }

    public BallSurfaceView(Context context, AttributeSet attrs) {
	    super(context, attrs);
	    initialize(context);
    }

    private void initialize(Context context) {
    	// ボールの画像を取得
		mDrawable = context.getResources().getDrawable(
			R.drawable.ball);
		
		// 傾きを初期化
    	mPitch = 0;
    	mRoll = 0;
    	
		// SensorManagerを取得
		mSensorManager = (SensorManager)context.getSystemService(
				Context.SENSOR_SERVICE);
		
		// イベントハンドラを登録
		mSensorManager.registerListener(
			new SensorEventListener() {
				@Override
				public void onAccuracyChanged(
					Sensor sensor, int accuracy) {
				}

				@Override
				public void onSensorChanged(
					SensorEvent event) {
					// 傾きを更新
					mPitch = event.values[SensorManager.DATA_Y];
					mRoll = event.values[SensorManager.DATA_Z];
				}
			}, 
			mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), 
			SensorManager.SENSOR_DELAY_GAME);

		// SurfaceHolderを作成
		mHolder = getHolder();
	    mHolder.addCallback(this); 
	    mHolder.setFixedSize(getWidth(), getHeight());

	}

	@Override
	public void surfaceChanged(
			SurfaceHolder holder, int format, int width, int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	    mThread = new Thread(this);
	    mThread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	    mThread = null;
	}

	@Override
	public void run() {
	    Canvas canvas = null;
	    Paint p = new Paint();
	    p.setColor(Color.WHITE);
	    
	    while (mThread != null) {
	    	// 0.01秒のwaitを入れる
	    	try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
	    	
	        try {
	        	// canvasを取得
		        canvas = mHolder.lockCanvas();
		        
		        if (canvas != null) {
		        	// canvasを塗りつぶす
			        canvas.drawRect(0, 0, getWidth(), getHeight(), p);
	
			        // 初回描画時にボールを作成
					if (mBall == null) {
				    	// 表示範囲を取得
				    	mRect = new Rect(0, 0, getWidth(), getHeight());
				    	
						// ボールを作成
						mBall = new Ball(mDrawable, mRect);
					}

					// ボールを移動・描画
					mBall.move(mPitch, mRoll);
		            mBall.draw(canvas);
		        }
	        } finally {
		        if (canvas != null) {
		        	mHolder.unlockCanvasAndPost(canvas);
		        }
	        }
    	}
	}
}
