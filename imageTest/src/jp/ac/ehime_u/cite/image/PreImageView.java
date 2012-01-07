package jp.ac.ehime_u.cite.image;

import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

public class PreImageView extends SurfaceView implements SurfaceHolder.Callback {

    // 画面サイズ
    private static int displayWidth;
    private static int displayHeight;
    static Context context;
    static SurfaceView surface;

    public PreImageView(Context context) {
        super(context);
        // SurfaceView描画に用いるコールバックを登録する
        getHolder().addCallback(this);
        this.context = context;
        surface = this;
    }

    public static void doDraw() {
        // Canvasクラス取得
        Canvas canvas = surface.getHolder().lockCanvas();
        
        // 画面サイズ取得
        displayWidth = surface.getWidth();
        displayHeight = surface.getHeight();

        // 状態を保存 
        canvas.save();
        
        // 描画処理
      
    	InputStream in = null;
    	try {
			in = context.openFileInput("test.jpg");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
//			BitmapFactory.Options opt = new BitmapFactory.Options();
//			int zoomx = (int) Math.floor((double)opt.outWidth / displayWidth);
//			int zoomy = (int) Math.floor((double)opt.outHeight / displayHeight);
//			opt.inJustDecodeBounds = false;
//			opt.inSampleSize = Math.min(zoomx, zoomy);

    	
		// Bitmapに読み込み
//			Bitmap bitmap = BitmapFactory.decodeStream(in, new Rect(0, 0, displayWidth, displayHeight)
//				, opt);
		
		Bitmap bitmap = BitmapFactory.decodeStream(in);
		
		// 横サイズの方が長ければ回転処理
		if(bitmap.getWidth() > bitmap.getHeight()){
			Matrix matrix = new Matrix();
			matrix.postRotate(90.0f);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), 
					matrix, true);
		}
		
		// リサイズ用に画像サイズ、画面サイズのRectを生成
		Rect image_rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect view_rect = new Rect(0, 0, displayWidth, displayHeight);
		
		canvas.drawBitmap(bitmap, image_rect, view_rect, new Paint());
		
		bitmap.recycle();
        
        
        // 画面更新
        canvas.restore();
        // 描画を終了
        surface.getHolder().unlockCanvasAndPost(canvas);
    }
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		doDraw();
		return true;
	}
    
    // SurfaceViewが最初に生成されたときに呼び出される
    public void surfaceCreated(SurfaceHolder holder) {
        //描画
        doDraw();
    }
    
    // SurfaceViewのサイズなどが変更されたときに呼び出される
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    // SurfaceViewが破棄されるときに呼び出される
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}