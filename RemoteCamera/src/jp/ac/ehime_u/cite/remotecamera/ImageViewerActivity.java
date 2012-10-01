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
import android.net.Uri;
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
	
	// メンバ変数
	// 親ActivityであるRemoteCameraActivityが
	// 多重起動時のインテントを処理する必要があるため
	// インテント処理時に扱う変数,関数はアクセス権限に注意 *親のメンバ変数に移したほうがいいかも？*
	// ->移しました。
	private static ImageView view;
	private static Bitmap bmp;
	private InputStream in;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		// 起動数++
		RemoteCameraActivity.image_viewer_count ++ ;
		
		if(view == null){
			view = new ImageView(this);
			setContentView(view);
		}
		RemoteCameraActivity.draw_switch = true;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		// 表示の更新
		if( RemoteCameraActivity.draw_switch){
			RemoteCameraActivity.draw_switch = false;
			doDraw();
			setTitle(RemoteCameraActivity.image_name_list.get(RemoteCameraActivity.select_image_no));
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// 起動数--
		RemoteCameraActivity.image_viewer_count -- ;
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event){
    	
    	if( event.getAction() == MotionEvent.ACTION_DOWN){
    		// 画面サイズ取得
    		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
    		Display disp = wm.getDefaultDisplay();
    		
    		Log.d("event","X:"+event.getX()+",windowX:"+disp.getWidth());
    		
    		// 左半分をタッチしたなら
    		if( event.getX() < disp.getWidth()/2 ){
    			// 選択ナンバーをデクリメント
    			if(RemoteCameraActivity.select_image_no == 0){
    				RemoteCameraActivity.select_image_no = RemoteCameraActivity.image_name_list.size()-1;
    			}
    			else{
    				RemoteCameraActivity.select_image_no--;
    			}
    		}
    		// 右半分をタッチしたなら
    		else{
    			// 選択ナンバーをインクリメント
    			RemoteCameraActivity.select_image_no++;
    			if(RemoteCameraActivity.select_image_no >= RemoteCameraActivity.image_name_list.size()){
    				RemoteCameraActivity.select_image_no = 0;
    			}
    		}
    		// 表示更新
    		doDraw();
    	}
    	
    	return true;
	}
	
	// viewの更新処理
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
	
	// メニューの追加
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST, Menu.NONE, "LOOP_STOP")
				.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		menu.add(0, Menu.FIRST+1, Menu.NONE, "exit");

		return ret;
	}

	// メニューが押されたとき
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case Menu.FIRST:
			// AODVにインテントを投げる
			Intent intent = new Intent();
			intent.setClassName("jp.ac.ehime_u.cite.udptest",
					"jp.ac.ehime_u.cite.udptest.RouteActivity");
			intent.setAction(Intent.ACTION_SENDTO);
			intent.setData(Uri.parse("connect:" + RemoteCameraActivity.calling_address));
			intent.putExtra("TASK", "TASK:CameraCapture:STOP");
			intent.putExtra("PACKAGE", "jp.ac.ehime_u.cite.remotecamera");
			intent.putExtra("ID", RemoteCameraActivity.send_intent_id);
			startActivity(intent);

			RemoteCameraActivity.send_intent_id++;
			return true;
		case Menu.FIRST+1:
			finish();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	

}
