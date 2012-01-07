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

    // ��ʃT�C�Y
    private static int displayWidth;
    private static int displayHeight;
    static Context context;
    static SurfaceView surface;

    public PreImageView(Context context) {
        super(context);
        // SurfaceView�`��ɗp����R�[���o�b�N��o�^����
        getHolder().addCallback(this);
        this.context = context;
        surface = this;
    }

    public static void doDraw() {
        // Canvas�N���X�擾
        Canvas canvas = surface.getHolder().lockCanvas();
        
        // ��ʃT�C�Y�擾
        displayWidth = surface.getWidth();
        displayHeight = surface.getHeight();

        // ��Ԃ�ۑ� 
        canvas.save();
        
        // �`�揈��
      
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

    	
		// Bitmap�ɓǂݍ���
//			Bitmap bitmap = BitmapFactory.decodeStream(in, new Rect(0, 0, displayWidth, displayHeight)
//				, opt);
		
		Bitmap bitmap = BitmapFactory.decodeStream(in);
		
		// ���T�C�Y�̕���������Ή�]����
		if(bitmap.getWidth() > bitmap.getHeight()){
			Matrix matrix = new Matrix();
			matrix.postRotate(90.0f);
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), 
					matrix, true);
		}
		
		// ���T�C�Y�p�ɉ摜�T�C�Y�A��ʃT�C�Y��Rect�𐶐�
		Rect image_rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		Rect view_rect = new Rect(0, 0, displayWidth, displayHeight);
		
		canvas.drawBitmap(bitmap, image_rect, view_rect, new Paint());
		
		bitmap.recycle();
        
        
        // ��ʍX�V
        canvas.restore();
        // �`����I��
        surface.getHolder().unlockCanvasAndPost(canvas);
    }
    
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		doDraw();
		return true;
	}
    
    // SurfaceView���ŏ��ɐ������ꂽ�Ƃ��ɌĂяo�����
    public void surfaceCreated(SurfaceHolder holder) {
        //�`��
        doDraw();
    }
    
    // SurfaceView�̃T�C�Y�Ȃǂ��ύX���ꂽ�Ƃ��ɌĂяo�����
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }
    // SurfaceView���j�������Ƃ��ɌĂяo�����
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
}