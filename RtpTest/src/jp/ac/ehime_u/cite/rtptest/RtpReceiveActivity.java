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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RtpReceiveActivity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	// 受信スレッド
	private Thread rtpListenerThread;
	
	protected static MediaPlayer mp = null;
	protected static SurfaceHolder s_holder = null;
	//final String DATA_PATH ;//= "/data/data/" + this.getPackageName() + "/files/test.mp4";
	
	Context context = this;

	/** Called when the activity is first created. */
	@Override	// オーバーライド(親･子クラスでメソッドが競合したとき子クラスの宣言で上書き)

	// ボタンなどが表示される前の初期化処理など
	// onCreateメソッドをオーバーライドとして記述していく
	public void onCreate(Bundle savedInstanceState) {
		// onCreateをオーバーライドする場合、スーパークラスのメソッドを呼び出す必要がある
		super.onCreate(savedInstanceState);
		
//		// 起動時にソフトキーボードの立ち上がりを防ぐ
//		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// アクティビティにビューグループを追加する
		setContentView(R.layout.receive);

//		// IDに対応するViewを取得、型が異なるのでキャスト
//		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
//		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
//
		try {
			//editTextSrc.setText(getIPAddress());
			setTitle(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
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
		setContentView(new SurfaceTestView(this));
		
		// MediaPlayer 管理
		mp = new MediaPlayer();
		mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
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
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        	@Override
        	public void onCompletion(MediaPlayer mp) {
        		Log.d("rtptest","onCompletion");
        		mp.stop();
        		mp.release();
        		mp = null;
        	}
        });
	}
	
	class SurfaceTestView extends SurfaceView implements SurfaceHolder.Callback {

		public SurfaceTestView(Context context) {
			super(context);

			SurfaceHolder holder = getHolder();
			holder.addCallback(this);
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			// TODO 自動生成されたメソッド・スタブ
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			s_holder = holder;
			try {
				mp.setDataSource("/data/data/" + context.getPackageName() + "/files/test.mp3");
			} catch (IllegalArgumentException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setDisplay(RtpReceiveActivity.s_holder);
			try {
				mp.prepare();
			} catch (IllegalStateException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
			mp.start();
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mp != null) {
				mp.release();
				mp = null;
			}
		};
	}

	
	
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
	
}