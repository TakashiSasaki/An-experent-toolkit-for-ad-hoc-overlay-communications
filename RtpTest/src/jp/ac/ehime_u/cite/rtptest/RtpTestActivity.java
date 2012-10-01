package jp.ac.ehime_u.cite.rtptest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.VideoView;

public class RtpTestActivity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	
	private Button buttonSend;
	private Button buttonMedia;
	private Button buttonClear;
	
	static VideoView video;
	
	// 受信スレッドを作成しない
	private Thread rtpListenerThread;
	
	byte[] buffer = new byte[1003];
	byte a=1,b=1;
	final int BYTE_PER_SEC = 8000;	// a,bの上限用 …なんでBYTE_PER_SECなんて名前?
	Timer m_Timer;
	FileInputStream fi = null;
	BufferedInputStream in = null;
	Context context = this;
	
	final String FILE_NAME = "test_back.mp4";

	/** Called when the activity is first created. */
	@Override	// オーバーライド(親･子クラスでメソッドが競合したとき子クラスの宣言で上書き)

	// ボタンなどが表示される前の初期化処理など
	// onCreateメソッドをオーバーライドとして記述していく
	public void onCreate(Bundle savedInstanceState) {
		// onCreateをオーバーライドする場合、スーパークラスのメソッドを呼び出す必要がある
		super.onCreate(savedInstanceState);
		
		// 起動時にソフトキーボードの立ち上がりを防ぐ
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// アクティビティにビューグループを追加する
		setContentView(R.layout.send);

		// IDに対応するViewを取得、型が異なるのでキャスト
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);
		
		buttonSend = (Button) findViewById(R.id.buttonSend);
		buttonClear = (Button) findViewById(R.id.buttonClear);
		buttonMedia = (Button) findViewById(R.id.Media);
		
		video = (VideoView) findViewById(R.id.videoView1);
		video.setVisibility(View.VISIBLE);
		video.setVideoPath("/data/data/" + context.getPackageName() + "/files/test_front.mp4");

		try {
			editTextSrc.setText(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		editTextDest.setText("133.71.3.1");
		
		// 受信ログ用のTextView、同様にIDから取得
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		try {
			// 受信スレッドのインスタンスを作成
			RtpListener rtp_listener = new RtpListener(new Handler(),
					text_view_received, 12345, 100, this);
			// スレッドを取得
			rtpListenerThread = new Thread(rtp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		// 受信スレッドrun()
		rtpListenerThread.start();
		
		// クリック処理、匿名クラス(その場限りの名前の無いクラス)を利用
		// ボタン毎に、ビューを意識せずに処理を記述できる
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Log.d(getClass().getName(), "send_clicked");
				
				// editTextから送信先IP(String)、port(int)の取得
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				
				// 送信処理
				streaming(destination_address, destination_port);
			}
		});
		
		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "clear_clicked");
				// 表示ログの削除
				text_view_received.setText("");
			}
		});
		
		buttonMedia.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "media_clicked");
				// メディア表示,ボタン隠し
				video.setVisibility(View.VISIBLE);
				
				//video.setVideoPath("/data/data/" + context.getPackageName() + "/files/" +FILE_NAME);
				video.start();
			}
		});
		
		// 動画タップ時に元に戻す
		video.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO 自動生成されたメソッド・スタブ
				video.setVisibility(View.INVISIBLE);
				return false;
			}
		});
		
		// 動画の自動停止
		video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
        	@Override
        	public boolean onError(MediaPlayer mp,int what,int extra) {
        		Log.d("rtptest","onError");
        		if(mp.isPlaying()){
        			Log.d("rtptest","isPlaying");
        			mp.pause();
        		}
//        		if(mp!=null){
//        			mp.release();
//        			mp = null;
//        		}
//				return false;
        		return true;
			}
        });
		
        //ビデオの再生が完了したらOnCompletionイベントが発生
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        	@Override
        	public void onCompletion(MediaPlayer mp) {
        		Log.d("rtptest","onCompletion");
        		mp.stop();
        		mp.release();
        		mp = null;
        	}
        });
		
		// ファイルオープン、同様にIDから取得しクリックイベントを追加
		Button buttonFileOpen = (Button) findViewById(R.id.buttonFileOpen);
		
		buttonFileOpen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SelectFile();
			}
		});

	}

	
	public static	SelectFileDialog	_dlgSelectFile;

	
	private	void	SelectFile()
	{
		//ここで画面回転を固定すべき（画面が固定されていないなら）
		_dlgSelectFile = new SelectFileDialog(context,new Handler(),editTextToBeSent);
		_dlgSelectFile.Show("/data/data/jp.ac.ehime_u.cite.rtptest/files/");
	}
	
	@Override
	public void onPause()
	{
		if(_dlgSelectFile != null)
			_dlgSelectFile.onPause();
		
		super.onPause();
		finish();
	}
	
//	// ログの表示用EditTextのサイズを画面サイズに合わせて動的に決定
//	// OnCreate()ではまだViewがレイアウトが初期化されていないため？
//	// Viewサイズなどの取得が不可
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {  
//		super.onWindowFocusChanged(hasFocus);
//		
//		// receivedのY座標を取得 * タイトルバー,ステータスバーの下が0扱い *
//		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
//		int received_top = text_view_received.getTop();
//		
//		// Clearのサイズを取得
//		final Button clear_button = (Button) findViewById(R.id.buttonClear);
//		int clear_height = clear_button.getHeight();
//		
//		// 画面サイズを取得 *タイトルバー,ステータスバー含む*
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE); 
//		Display display = wm.getDefaultDisplay(); 
//		int display_height = display.getHeight();
//		
//		// タイトル+ステータスバーのサイズを50と仮定、不完全な動的決定
//		text_view_received.setHeight(display_height-received_top-clear_height-50);
//	} 
	
	// 自身のIPアドレスを取得
	public static String getIPAddress() throws IOException{
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
	
	// 送信ループ
	protected void streaming(String destination_address,int destination_port){
		
		// 送信先情報
		final InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				destination_address, destination_port);
		
		// ファイルオープン
		try {
			fi = openFileInput(FILE_NAME);
		} catch (FileNotFoundException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		in = new BufferedInputStream(fi);
		
		m_Timer = new Timer();
		long delay  = 0;	// delay ミリ秒後に開始
		long period = 50;	// periodミリ秒毎に実行
		
		Handler m_handler = new Handler();
		
		a=1;
		b=1;
		
		// 再起処理
		m_Timer.scheduleAtFixedRate(new TimerTask(){
			
			@Override
			public void run() {
				
				if(1000*b > BYTE_PER_SEC){
					a++;
					b=1;
				}
				buffer[0] = a;
				buffer[1] = b;
				
				// 送信try
				try {
					if(in.read(buffer, 2, 1000) == -1){
						// 送信終了
						in.close();
						fi.close();
						m_Timer.cancel();
						m_Timer = null;
					}
					else{
						// 送信パケットの生成
						DatagramPacket packet_to_be_sent = new DatagramPacket(
								buffer, buffer.length,
								destination_inet_socket_address);
						
						// 送信用のクラスを生成、送信、クローズ
						DatagramSocket datagram_socket = new DatagramSocket();
						datagram_socket.send(packet_to_be_sent);
						datagram_socket.close();
					}
					
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				b++;
			}
			
		}, delay, period);
	}
}