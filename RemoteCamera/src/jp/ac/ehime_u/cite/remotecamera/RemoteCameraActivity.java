package jp.ac.ehime_u.cite.remotecamera;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewDebug.FlagToString;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class RemoteCameraActivity extends Activity{
	
	private static final int MENU_AUTOFOCUS = Menu.FIRST + 1;
	
	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDefaultPictureName;
	private EditText editTextPictureSizeWidth;
	private EditText editTextPictureSizeHeight;
	private CheckBox checkBoxWatching;
	protected static String calling_address;
	private String calling_file_name;
	
	// インテント制御用変数
	private static int prev_receive_intent_id = -1;
	private static String prev_receive_intent_package_name = null;
	protected static int send_intent_id = 0;
	
	// インテントによる多重起動を制御するための変数
	// 起動中のActivity数をカウント
	private static int remote_camera_count = 0;
	protected static int image_viewer_count = 0;
	protected static int auto_focus_count = 0;
	
	// ファイル関連
	protected static String file_name;
	protected static String file_path;
	
	// 古いファイルの削除
	protected static int DELETE_TIME = 90 * 1000; // [ms]
	
	// 子Activityに渡すコンテキスト
	protected static Context context;
	
	/***** ImageViewerActivityのメンバ変数(一部)
	 * インテントの代行処理を親Activityで行うために必要 
	 *****/
	protected static int select_image_no = 0;
	protected static ArrayList<String> image_name_list = new ArrayList<String>();
	protected static ArrayList<String> address_list = new ArrayList<String>();
	protected static boolean draw_switch;
	
	/***** AutoFocusのメンバ変数(一部) *****/
	protected static boolean loop_flag;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		// 起動個数++
		remote_camera_count++;
		
		context = this;
		
		// 暗黙的Intentの回収処理
		Intent receive_intent = getIntent();
		
		String package_name = receive_intent.getStringExtra("PACKAGE");
		int intent_id = receive_intent.getIntExtra("ID", 0);
		
		// 同インテントの多重処理防止 前回とパッケージ名またはIDが異なっていれば受理
		if( (package_name != prev_receive_intent_package_name)
				|| intent_id != prev_receive_intent_id){
			
			// 直前のパッケージ名,IDとして記録
			prev_receive_intent_package_name = package_name;
			prev_receive_intent_id = intent_id;
		
			// 起動方法のチェック 暗黙的インテント:CALLであり
			// 画像ヴューが起動していなければ
			if(Intent.ACTION_CALL.equals(receive_intent.getAction())
					&& image_viewer_count == 0){
				final Uri uri = receive_intent.getData();
				
				// schemeが"CameraCapture"なら
				if("CameraCapture".equals(uri.getScheme())){
					
					// "CameraCapture:"以降を取得
					String call_data = uri.getEncodedSchemeSpecificPart();
					
					// STOP命令ならフラグを切り替えて終了
					if("STOP".equals(call_data)){
						loop_flag = false;
					}
					else{
						// 先頭から順に切り出し
						String[] call_data_list = call_data.split("_");
					
						// [0]:フラグ
						// [1]:要求横サイズ
						// [2]:要求縦サイズ
						// [3]:送信元(返信用)
						calling_address = call_data_list[3].toString();
						
						file_name = getPreDefaultFileName();
						
						// カメラが起動中でなければ起動
						if(auto_focus_count == 0){
				            Intent intent = new Intent();
				            intent.setClassName(
				                    "jp.ac.ehime_u.cite.remotecamera",
				                    "jp.ac.ehime_u.cite.remotecamera.AutoFocus");
				            
				            intent.putExtra("FLAG", call_data_list[0]);
				            intent.putExtra("SIZE_X", Integer.parseInt(call_data_list[1]));
				            intent.putExtra("SIZE_Y", Integer.parseInt(call_data_list[2]));
				            
				            startActivity(intent);
						}
					}
				}
			}
			
			// 起動方法のチェック アクションがVIEWであり
			// カメラが起動していなければ
			// ImageViewerActivityを用いて画像の表示
			if(Intent.ACTION_VIEW.equals(receive_intent.getAction())
					&& auto_focus_count == 0){
				final Uri uri = receive_intent.getData();
				
				// schemeが"Files"なら
				if("Files".equals(uri.getScheme())){
					// Files:以降の文字列を取得
					calling_file_name = uri.getEncodedSchemeSpecificPart();
					String calling_address = receive_intent.getStringExtra("SOURCE_ADDRESS");
					
					Log.d("JpegFile",calling_file_name);
					
					deleteOldFile();
					moveFile();
					//deleteAodvFile(calling_file_name);
					
					// インテントの処理代行
					if( (calling_file_name != null) && (calling_address != null) ){
						
						int index_result_file;
						int index_result_add;
						
						// 既にリストに無いかチェック
						if((index_result_file = searchFile(calling_file_name)) == -1){	// ファイルが無ければ
							if((index_result_add = searchAddress(calling_address)) == -1){	// アドレスが無ければ
								// 両方に追加
								image_name_list.add(calling_file_name);
								address_list.add(calling_address);
							}
							else{
								// アドレスが一致したシーケンス番号のファイル名を変更
								image_name_list.set(index_result_add, calling_file_name);
								
								// 選択中の画像ならば
								if( index_result_add == select_image_no){
									// 開き直し
									draw_switch = true;
								}
							}
						}
						else{	// 既存のファイルと重複
							// 別アドレスからのファイル名重複なら
							if(calling_address.equals(address_list.get(index_result_file)) != true){
								// アドレスを*上書き*
								// （同アドレスが複数箇所できる可能性を無視）
								address_list.set(index_result_file, calling_address);
							}
							
							// 選択中の画像ならば
							if( index_result_file == select_image_no){
								// 開き直し
								draw_switch = true;
							}
						}
					}
					
					// ImageViewerActivityが起動中ならインテントは投げない
					if(image_viewer_count < 1){
						
						// 画像ビュー起動
			            Intent intent = new Intent();
			            intent.setClassName(
			                    "jp.ac.ehime_u.cite.remotecamera",
			                    "jp.ac.ehime_u.cite.remotecamera.ImageViewerActivity");
			            startActivity(intent);
			            
					}
					
				}
			}
		}
		
		// 多重起動なら終了処理
		if(remote_camera_count > 1){
			Log.d("RemoteCameraActivity_onCreate()","if(count>1) finish()");
			finish();
		}
		
		// 起動時にソフトキーボードの立ち上がりを防ぐ
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// アクティビティにビューグループを追加する
		setContentView(R.layout.main);
		
		// IDに対応するViewを取得、型が異なるのでキャスト
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextDefaultPictureName = (EditText) findViewById(R.id.editTextDefaultFileName);
		editTextPictureSizeWidth = (EditText) findViewById(R.id.editTextPictureSizeWidth);
		editTextPictureSizeHeight = (EditText) findViewById(R.id.editTextPictureSizeHeight);
		checkBoxWatching = (CheckBox) findViewById(R.id.checkBoxWatching);
		
		
		
		// 自身のアドレスを取得
		try {
			editTextSrc.setText(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		// デフォルト写真ファイル名を取得
		// 過去に設定した名前があるならそれを継続利用 *ローカルファイルを利用
		editTextDefaultPictureName.setText(getPreDefaultFileName(editTextSrc.getText().toString()));
		
		
		// ファイル名を設定時にローカルファイルに保存するイベントを登録
		editTextDefaultPictureName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				setPreDefaultFileName(editTextDefaultPictureName.getText().toString());
			}
		});
		
		// 要求写真サイズに自身のサイズを設定(初期化)
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		
		// 横向きを基本に設定 X>Y
		if(disp.getHeight() > disp.getWidth()){
			// 縦横逆にセット
			editTextPictureSizeHeight.setText(String.valueOf(disp.getWidth()));
			editTextPictureSizeWidth.setText(String.valueOf(disp.getHeight()));
		}
		else{	// 横幅のほうが広いディスプレイならそのまま
			editTextPictureSizeHeight.setText(String.valueOf(disp.getHeight()));
			editTextPictureSizeWidth.setText(String.valueOf(disp.getWidth()));
		}
		
		// 送信Button、同様にIDから取得
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// クリック処理を登録
		// AODVに接続要求・写真要求を投げる
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// 宛先の文字列を取得
				String destination_s = editTextDest.getText().toString();
				
				// 写真サイズを取得
				String size_width = editTextPictureSizeWidth.getText().toString();
				String size_height= editTextPictureSizeHeight.getText().toString();
				
				// 暗黙的インテントを投げる
	            Intent intent = new Intent();
	            intent.setAction(Intent.ACTION_SENDTO);
	            intent.setData(Uri.parse("connect:"+destination_s));
	            intent.putExtra("TASK", "TASK:CameraCapture:"+getWatchingLoopCheck()+"_"
	            		+size_width+"_"+size_height);
	            intent.putExtra("PACKAGE","jp.ac.ehime_u.cite.remotecamera");
	            intent.putExtra("ID", send_intent_id);
	            
	            startActivity(intent);
			}
		});

    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// 起動数をデクリメント
		remote_camera_count --;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_AUTOFOCUS, Menu.NONE, "camera_wake");
		menu.add(Menu.NONE, MENU_AUTOFOCUS+1, Menu.NONE, "image_view");
		menu.add(Menu.NONE, MENU_AUTOFOCUS+2, Menu.NONE, "END");
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
			// カメラ起動
			// ファイル名に日付を使用
//			Date date = new Date();
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy'_'MMdd'_'HHmmss");
//			
//			file_name = sdf.format(date) + ".jpg";
//			
//			calling_address = "11.1.1.1";
//			
//            Intent intent = new Intent();
//            intent.setClassName(
//                    "jp.ac.ehime_u.cite.remotecamera",
//                    "jp.ac.ehime_u.cite.remotecamera.AutoFocus");
//            startActivity(intent);
			// ファイル移動
//			File pre_path = new File("/data/data/jp.ac.ehime_u.cite.udptest/files/", "2011_1220_151329.jpg");
//			File next_path= new File("/data/data/jp.ac.ehime_u.cite.remotecamera/files/");
//			
//			boolean a = pre_path.renameTo(new File(next_path, "2011_1220_151329.jpg"));
			
			calling_file_name = "test.jpg";
			deleteAodvFile(calling_file_name);
			
			break;
			
		case MENU_AUTOFOCUS+1:
			
            Intent intent1 = new Intent();
            intent1.setClassName(
                    "jp.ac.ehime_u.cite.remotecamera",
                    "jp.ac.ehime_u.cite.remotecamera.ImageViewerActivity");
            intent1.putExtra("FILE_NAME", "test.jpg");
            startActivity(intent1);
			
			
			break;
		case MENU_AUTOFOCUS+2:
			Context c1 = null;
			try {
				c1 =  createPackageContext("jp.ac.ehime_u.cite.image",0);
			} catch (NameNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			
			try {
				OutputStream o = c1.openFileOutput("test.jpg",MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE);
				o.write(1);
				o.close();
				
				c1.openFileInput("test.jpg");
				Toast.makeText(this, "みつかる", Toast.LENGTH_LONG).show();
				
			} catch (FileNotFoundException e) {
				// TODO 自動生成された catch ブロック
				
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			
            //finish();
		default:
			rc = super.onOptionsItemSelected(item);
			break;
		}
		return rc;
	}
    
	// 自身のIPアドレスを取得
	private String getIPAddress() throws IOException{
	    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        
	    while(interfaces.hasMoreElements()){
	        NetworkInterface network = interfaces.nextElement();
	        Enumeration<InetAddress> addresses = network.getInetAddresses();
	            
	        while(addresses.hasMoreElements()){
	            String address = addresses.nextElement().getHostAddress();
	                
	            //127.0.0.1と0.0.0.0以外のアドレスが見つかったらそれを返す
	            if(!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address)){
	                return address;
	            }
	        }
	    }
	        
	    return "127.0.0.1";
	}
	
	// ファイル削除
	private void deleteOldFile(){
		
		// ファイル一覧の取得
		File[] files = new File("/data/data/jp.ac.ehime_u.cite.remotecamera/files/").listFiles();
		
		// 現在時刻
		long now_time = new Date().getTime();
		
		for(int i=0; i<files.length; i++){
			
			// 画像Viewerに登録済みなら削除しない
			if( searchFile(files[i].getName()) == -1){
				// ディレクトリではなく、寿命であるファイルを検索
				if(!files[i].isDirectory()){
					long last_update_time = files[i].lastModified();
					
					// 最終更新日時から削除時間以上の時間が過ぎていれば
					if( (now_time - last_update_time) > DELETE_TIME){
						deleteFile(files[i].getName());
					}
				}
			}
		}
	}
	
	// ファイル移動 *udptest->remotecamera 用*
	private void moveFile(){
		
		// 変数宣言
		Context aodv_c = null;
		
		try {	
			// 読み込み元のコンテキスト作成
			aodv_c = createPackageContext("jp.ac.ehime_u.cite.udptest",0);
			// 読み込み元のファイルストリームを作成し、チャネルを取得
			FileChannel in_channel = aodv_c.openFileInput(calling_file_name).getChannel();
			
			// 同様に出力先
			FileChannel out_channel= context.openFileOutput(calling_file_name, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE)
								.getChannel();
			
			// 転送
			in_channel.transferTo(0, in_channel.size(), out_channel);
			
			// クローズ
			in_channel.close();
			out_channel.close();
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// ファイル削除 *udptest内*
	private void deleteAodvFile(String file_name){
		
		// 削除命令インテントを発行
		// AODVに明示的インテントを投げる
        Intent intent = new Intent();
        intent.setClassName(
                "jp.ac.ehime_u.cite.udptest",
                "jp.ac.ehime_u.cite.udptest.AODV_Activity");
        intent.setAction(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("path:"+file_name));
        intent.putExtra("PACKAGE","jp.ac.ehime_u.cite.remotecamera");
        intent.putExtra("ID", send_intent_id);
        startActivity(intent);
        
        send_intent_id++;
	}
	
	// 以前のデフォルトネームを返す
	// 無ければ自身のアドレスを利用
    private CharSequence getPreDefaultFileName(String ip_address) {
    	
    	try {
			InputStream in = openFileInput("fileName.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String s = reader.readLine();
			
			reader.close();
			in.close();
			return s;
			
		} catch (Exception e) {	// IOException+FileNotFoundException+...
			return ip_address.replaceAll("\\.", "_");
		}
	}
    private String getPreDefaultFileName() {
    	
    	try {
			InputStream in = openFileInput("fileName.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String s = reader.readLine();
			
			reader.close();
			in.close();
			return s;
			
		} catch (Exception e) {	// IOException+FileNotFoundException+...
			try {
				return getIPAddress().replaceAll("\\.", "_");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
    
    // デフォルトネームとしてローカルファイルに保持
    private void setPreDefaultFileName(String name){
		try {
			OutputStream out = openFileOutput("fileName.txt",MODE_PRIVATE);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
			writer.write(name);
			
			writer.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    // WatchingLoopのチェック取得
    // 戻り値：String型で"1"(オン),"0"(オフ)
    private String getWatchingLoopCheck(){
    	if(checkBoxWatching.isChecked()){
    		return "1";
    	}
    	return "0";
    }
    
    // 画像Viewer用 リストに既に存在するかチェック
	private int searchFile(String name){
		for(int i=0;i<image_name_list.size();i++){
			if(image_name_list.get(i).equals(name)){
				return i;
			}
		}
		return -1;
	}
	
	// 画像Viewer用 リストに既に存在するかチェック
	private int searchAddress(String address){
		for(int i=0;i<address_list.size();i++){
			if(address_list.get(i).equals(address)){
				return i;
			}
		}
		return -1;
	}
    
}