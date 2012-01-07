package jp.ac.ehime_u.cite.remotecamera;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class ImageViewerActivity extends Activity {
	
	// �����o�ϐ�
	// �eActivity�ł���RemoteCameraActivity��
	// ���d�N�����̃C���e���g����������K�v�����邽��
	// �C���e���g�������Ɉ����ϐ�,�֐��̓A�N�Z�X�����ɒ��� *�e�̃����o�ϐ��Ɉڂ����ق������������H*
	// ->�ڂ��܂����B
	private static ImageView view;
	private static Bitmap bmp;
	private InputStream in;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// �N����++
		RemoteCameraActivity.image_viewer_count ++ ;
		
		if(view == null){
			view = new ImageView(this);
		}
		
		RemoteCameraActivity.draw_switch = true;
		
		 //Toast.makeText(this, "received:"+new_file_name, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// �\���̍X�V
		if( RemoteCameraActivity.draw_switch){
			RemoteCameraActivity.draw_switch = false;
			doDraw();
			setTitle(RemoteCameraActivity.image_name_list.get(RemoteCameraActivity.select_image_no));
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// �N����--
		RemoteCameraActivity.image_viewer_count -- ;
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event){
    	
    	if( event.getAction() == MotionEvent.ACTION_DOWN){
    		// ��ʃT�C�Y�擾
    		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    		Display disp = wm.getDefaultDisplay();
    		
    		Log.d("event","X:"+event.getX()+",windowX:"+disp.getWidth());
    		
    		// ���������^�b�`�����Ȃ�
    		if( event.getX() < disp.getWidth()/2 ){
    			// �I���i���o�[���f�N�������g
    			if(RemoteCameraActivity.select_image_no == 0){
    				RemoteCameraActivity.select_image_no = RemoteCameraActivity.image_name_list.size()-1;
    			}
    			else{
    				RemoteCameraActivity.select_image_no--;
    			}
    		}
    		// �E�������^�b�`�����Ȃ�
    		else{
    			// �I���i���o�[���C���N�������g
    			RemoteCameraActivity.select_image_no++;
    			if(RemoteCameraActivity.select_image_no >= RemoteCameraActivity.image_name_list.size()){
    				RemoteCameraActivity.select_image_no = 0;
    			}
    		}
    		// �\���X�V
    		doDraw();
    	}
    	
    	return true;
	}
	
	// view�̍X�V����
	private void doDraw(){
		
		if(bmp != null){
			bmp.recycle();
			bmp = null;
		}
		
		try {
			in = openFileInput(RemoteCameraActivity.image_name_list.get(RemoteCameraActivity.select_image_no));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		bmp = BitmapFactory.decodeStream(in);
		view.setImageBitmap(bmp);
		setContentView(view);
		
		try {
			in.close();
			in = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Toast.makeText(this, image_name_list.get(select_image_no), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "close");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rc = true;
		switch (item.getItemId()) {
		case Menu.FIRST:
			finish();
			break;
		default:
			rc = super.onOptionsItemSelected(item);
			break;
		}
		return rc;
	}
	

}
