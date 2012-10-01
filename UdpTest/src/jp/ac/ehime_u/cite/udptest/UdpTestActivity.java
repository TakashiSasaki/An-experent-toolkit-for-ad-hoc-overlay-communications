package jp.ac.ehime_u.cite.udptest;

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

public class UdpTestActivity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	// 受信スレッド
	private Thread udpListenerThread;

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
			//editTextDest.setText("133.71.232.13");
			editTextDest.setText("224.0.0.1");
		
		// 受信ログ用のTextView、同様にIDから取得
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		try {
			// 受信スレッドのインスタンスを作成
			UdpListener udp_listener = new UdpListener(new Handler(),
					text_view_received, 12345, 100);
			// スレッドを取得
			udpListenerThread = new Thread(udp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		// 受信スレッドrun()
		udpListenerThread.start();

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
				// 送信先情報
				//InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				//		destination_address, destination_port);
				// ** Multi **
				MulticastSocket socket = null;
				try {
					socket = new MulticastSocket();
				} catch (IOException e2) {
					// TODO 自動生成された catch ブロック
					e2.printStackTrace();
				}
				try {
					socket.setTTL((byte) 1);
				} catch (IOException e1) {
					// TODO 自動生成された catch ブロック
					e1.printStackTrace();
				}
				
				
				
				// 送信する文字列の取得
				String string_to_be_sent = editTextToBeSent.getText()
						.toString() + "\r\n";
				// ### デバッグ用 ###
				Date date1 = new Date(new Date().getTime());
				string_to_be_sent = date1.toString()+"\n\r";

				// Byte列化
				byte[] buffer = string_to_be_sent.getBytes();

				// 送信try
				try {
					// LogCatに送信情報を出力
					Log.d("UdpTest", "sending " + string_to_be_sent);
					// 送信パケットの生成
					InetAddress address = InetAddress.getByName(destination_address);
					DatagramPacket packet_to_be_sent = new DatagramPacket(
							buffer, buffer.length,
					//		destination_inet_socket_address);
							address ,destination_port);
						
					socket.send(packet_to_be_sent);
					// 送信用のクラスを生成、送信、クローズ
					//DatagramSocket datagram_socket = new DatagramSocket();
					//datagram_socket.send(packet_to_be_sent);
					//datagram_socket.close();
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
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
}