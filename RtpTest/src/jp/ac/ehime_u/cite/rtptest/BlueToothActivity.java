package jp.ac.ehime_u.cite.rtptest;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.VideoView;

public class BlueToothActivity extends Activity {

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
	
	private BluetoothAdapter Bt;
	
	Context context = this;

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
		
		// BlueToothサーバースレッドを作成
		Thread serverThread = null;
		BlueToothServerThread server = new BlueToothServerThread(context, "myNum", BluetoothAdapter.getDefaultAdapter()
																, new Handler(), text_view_received);
		// スレッドを取得
		serverThread = new Thread(server);
		serverThread.start();
		
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
			}
		});
		
		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "clear_clicked");
				// 表示ログの削除
				text_view_received.setText("");
			}
		});
		
		// BlueTooth
		Bt = BluetoothAdapter.getDefaultAdapter();
		if(Bt.equals(null)){
			text_view_received.setText("BlueToothに対応していません");
		}else{
			text_view_received.setText("BlueToothに対応しています");
			
			boolean btEnable = Bt.isEnabled();
			if(btEnable==false){	// BlueToothの許可が無い
				Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(btOn, 1);
			}
		}
		
		buttonMedia.setText("BlueTooth");
		buttonMedia.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "bluetooth_clicked");
				
				if(Bt.isEnabled()){
					// 接続可能デバイス一覧表示
			        Intent intent = new Intent(context, BlueToothList.class);
			        int requestCode = 123;
			        startActivityForResult(intent, requestCode);
				}
			}
		});
	}
	
	// ActivityIntentの結果を受け取る
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
			case 123:
				if (resultCode == RESULT_OK) {
					// 接続の開始(Client)
					BluetoothDevice device = data.getParcelableExtra("DEVICE");
					BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
					EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
					Handler handler = new Handler();
					
		            if(btAdapter.isDiscovering()){
		            	//検索中の場合は検出をキャンセルする
		            	btAdapter.cancelDiscovery();
		            }
					
					Thread clientThread = null;
					BlueToothClientThread client = new BlueToothClientThread(context, "Mynum", device, btAdapter, handler, text_view_received);
					// スレッドを取得
					clientThread = new Thread(client);
					clientThread.start();
					
					
				}else if (resultCode == RESULT_CANCELED) {
					
				}
			break;
			default:
			break;
		}
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
		
		byte buffer[] = null;
		// 送信先情報
		final InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				destination_address, destination_port);
		
		// 送信パケットの生成
		DatagramPacket packet_to_be_sent = null;
		try {
			packet_to_be_sent = new DatagramPacket(
					buffer, buffer.length,
					destination_inet_socket_address);
			
			// 送信用のクラスを生成、送信、クローズ
			DatagramSocket datagram_socket;
			datagram_socket = new DatagramSocket();
			datagram_socket.send(packet_to_be_sent);
			datagram_socket.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
