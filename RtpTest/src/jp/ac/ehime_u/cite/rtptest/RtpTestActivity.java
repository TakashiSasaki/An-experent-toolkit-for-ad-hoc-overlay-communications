package jp.ac.ehime_u.cite.rtptest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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

import jp.ac.ehime_u.cite.udptest.RREQ;
import jp.ac.ehime_u.cite.udptest.RouteTable;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class RtpTestActivity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	// 受信スレッド
	private Thread rtpListenerThread;

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
		setContentView(R.layout.main);

		// IDに対応するViewを取得、型が異なるのでキャスト
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);
		
		Button buttonClear = (Button) findViewById(R.id.buttonClear);

		try {
			editTextSrc.setText(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		editTextDest.setText("133.71.232.13");
		
		// 受信ログ用のTextView、同様にIDから取得
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		try {
			// 受信スレッドのインスタンスを作成
			RtpListener rtp_listener = new RtpListener(new Handler(),
					text_view_received, 12345, 100);
			// スレッドを取得
			rtpListenerThread = new Thread(rtp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		// 受信スレッドrun()
		rtpListenerThread.start();

		// 送信Button、同様にIDから取得
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// クリック処理、匿名クラス(その場限りの名前の無いクラス)を利用
		// ボタン毎に、ビューを意識せずに処理を記述できる
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Log.d("UdpTest", "send_clicked");
				
				// editTextから送信先IP(String)、port(int)の取得
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());

				streaming(destination_address, destination_port);
			}
		});

	}
	
	// ログの表示用EditTextのサイズを画面サイズに合わせて動的に決定
	// OnCreate()ではまだViewがレイアウトが初期化されていないため？
	// Viewサイズなどの取得が不可
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {  
		super.onWindowFocusChanged(hasFocus);
		
		// receivedのY座標を取得 * タイトルバー,ステータスバーの下が0扱い *
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		int received_top = text_view_received.getTop();
		
		// Clearのサイズを取得
		final Button clear_button = (Button) findViewById(R.id.buttonClear);
		int clear_height = clear_button.getHeight();
		
		// 画面サイズを取得 *タイトルバー,ステータスバー含む*
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE); 
		Display display = wm.getDefaultDisplay(); 
		int display_height = display.getHeight();
		
		// タイトル+ステータスバーのサイズを50と仮定、不完全な動的決定
		text_view_received.setHeight(display_height-received_top-clear_height-50);
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
	
	// 送信ループ
	protected void streaming(String destination_address,int destination_port){
		
		// 送信先情報
		final InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				destination_address, destination_port);
		
		Timer m_Timer = new Timer();
		long delay  = 0;	// delay ミリ秒後に開始
		long period = 50;	// periodミリ秒毎に実行
		
		Handler m_handler = new Handler();
		
		// 再起処理
		m_Timer.scheduleAtFixedRate(new TimerTask(){

			@Override
			public void run() {
				
				// 送信try
				try {
					// 送信パケットの生成
					DatagramPacket packet_to_be_sent = new DatagramPacket(
							buffer, buffer.length,
							destination_inet_socket_address);
					
					// 送信用のクラスを生成、送信、クローズ
					DatagramSocket datagram_socket = new DatagramSocket();
					datagram_socket.send(packet_to_be_sent);
					datagram_socket.close();
					
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}, delay, period);
	}
}