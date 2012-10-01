package jp.ac.ehime_u.cite.remotecamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class AutoFocus extends Activity {
	private static final int MENU_AUTOFOCUS = Menu.FIRST + 1;
	private static AutoFocusPreview view = null;
	private Handler handler;
	private Context context;
	protected static int order_size_x;
	protected static int order_size_y;
	protected static boolean do_capture;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 起動数++
		RemoteCameraActivity.auto_focus_count++;
		
		do_capture = false;
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// オーダー初期化
		RemoteCameraActivity.loop_flag = false;
		
		Intent it = getIntent();
		String flag = it.getStringExtra("FLAG");
		if(flag != null){
			RemoteCameraActivity.loop_flag = flag.charAt(0)=='1';	// 0文字目を取得し'1'ならオン
		}
		order_size_x = it.getIntExtra("SIZE_X", 0);
		order_size_y = it.getIntExtra("SIZE_Y", 0);
		
		// カメラ起動
		if(view == null){
			view = new AutoFocusPreview(this);
			setContentView(view);
		
			handler = new Handler();
			context = this;
			
			try {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
						view.autoFocus();
					}
				}).start();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
//	@Override
//	protected void onResume(){
//		super.onResume();
//		
//		// 写真撮影後、戻ってきたときのみ
//		if(do_capture){
//			if(RemoteCameraActivity.loop_flag){
//				//view.camera.startPreview();
//				view.autoFocus();
//			}
//			else{
//				finish();
//			}
//		}
//	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// 起動数--
		RemoteCameraActivity.auto_focus_count--;
	}
	
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		super.onWindowFocusChanged(hasFocus);
//		
//		view.autoFocus();
//		
//		finish();
//	}
	
	// メニューの追加
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);
		menu.add(0, Menu.FIRST, Menu.NONE, "exit");

		return ret;
	}

	// メニューが押されたとき
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case Menu.FIRST:
			finish();
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
