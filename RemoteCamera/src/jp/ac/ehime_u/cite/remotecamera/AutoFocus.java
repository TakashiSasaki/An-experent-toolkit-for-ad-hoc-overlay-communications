package jp.ac.ehime_u.cite.remotecamera;

import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class AutoFocus extends Activity {
	private static final int MENU_AUTOFOCUS = Menu.FIRST + 1;
	private static CameraPreview view = null;
	private Handler handler;
	private Context context;
	private static boolean loop_flag;
	protected static int order_size_x;
	protected static int order_size_y;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 起動数++
		RemoteCameraActivity.auto_focus_count++;
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		// オーダー初期化
		loop_flag = false;
		
		Intent it = getIntent();
		String flag = it.getStringExtra("FLAG");
		if(flag != null){
			loop_flag = flag.charAt(0)=='1';	// 0文字目を取得し'1'ならオン
		}
		order_size_x = it.getIntExtra("SIZE_X", 0);
		order_size_y = it.getIntExtra("SIZE_Y", 0);
		
		// カメラ起動
		if(view == null){
			view = new CameraPreview(this);
			setContentView(view);
		
			handler = new Handler();
			context = this;
			
			try {
				new Thread(new Runnable() {
					@Override
					public void run() {
	//					handler.post(new Runnable() {
	//							@Override
	//							public void run(){
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
						
						view.autoFocus();
						
						try {
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							// TODO 自動生成された catch ブロック
							e.printStackTrace();
						}
						
						RemoteCameraActivity.do_capture = true;
						view.close();
						view = null;
						finish();
	//							}
	//					});
					}
				}).start();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_AUTOFOCUS, Menu.NONE, "close");
		menu.add(Menu.NONE, MENU_AUTOFOCUS+1, Menu.NONE, "autofocus");
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
		case MENU_AUTOFOCUS:
			view.close();
			break;
		case MENU_AUTOFOCUS+1:
			view.autoFocus();
			break;
		default:
			rc = super.onOptionsItemSelected(item);
			break;
		}
		return rc;
	}
}
