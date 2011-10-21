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
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

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

public class AODV_Activity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;

	// スレッド
	private Thread udpListenerThread; // 受信スレッド
	private Thread routeManagerThread; // ルート監視スレッド

	// ルートテーブル
	public static ArrayList<RouteTable> routeTable = new ArrayList<RouteTable>();

	// PATH_DISCOVERY_TIMEの間に受信したRREQの送信元とIDを記録
	public static ArrayList<PastData> receiveRREQ_List = new ArrayList<PastData>();
	
	// とりあえずPATH_DISCOVERY_TIME の間に受信したメッセージ0の送信元とIDを記録
	public static ArrayList<PastData> receiveMessageList = new ArrayList<PastData>();

	// マルチスレッドの排他制御用オブジェクト
	public static Object routeLock = new Object();
	public static Object pastDataLock = new Object();
	
	
	// その他変数
	public static int RREQ_ID = 0;
	public static int seqNum = 0;
	public static boolean do_BroadCast = false; // 一定時間内に何かﾌﾞﾛｰﾄﾞｷｬｽﾄしたかどうか
	public static int message_ID = 0;

	// 様々なパラメータのデフォルト値を宣言
	public static final int ACTIVE_ROUTE_TIMEOUT = 3000; // [ms]
	public static final int ALLOWED_HELLO_LOSS = 2;
	public static final int HELLO_INTERVAL = 1000; // [ms]
	public static final int DELETE_PERIOD = (ACTIVE_ROUTE_TIMEOUT >= HELLO_INTERVAL) ? 5 * ACTIVE_ROUTE_TIMEOUT
			: 5 * HELLO_INTERVAL;
	public static final int LOCAL_ADD_TTL = 2;
	public static final int MY_ROUTE_TIMEOUT = 2 * ACTIVE_ROUTE_TIMEOUT;
	public static final int NET_DIAMETER = 35;
	public static final int MAX_REPAIR_TTL = (int) (0.3 * NET_DIAMETER);
	public static int MIN_REPAIR_TTL = -1; // 宛先ノードへ知られている最新のホップ数
	public static final int NODE_TRAVERSAL_TIME = 40; // [ms]
	public static final int NET_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
			* NET_DIAMETER;
	public static final int NEXT_HOP_WAIT = NODE_TRAVERSAL_TIME + 10;
	public static final int PATH_DISCOVERY_TIME = 2 * NET_TRAVERSAL_TIME;
	public static final int PERR_RATELIMIT = 10;
	public static final int RREQ_RETRIES = 2;
	public static final int RREQ_RATELIMIT = 10;
	public static final int TIMEOUT_BUFFER = 2;
	public static final int TTL_START = 1;
	public static final int TTL_INCREMENT = 2;
	public static final int TTL_THRESHOLD = 7;
	public static int TTL_VALUE = 1; // IPヘッダ内の"TTL"フィールドの値 getTimeToLive()？
	public static int RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
			* (TTL_VALUE + TIMEOUT_BUFFER);

	/** Called when the activity is first created. */
	@Override
	// オーバーライド(親･子クラスでメソッドが競合したとき子クラスの宣言で上書き)
	// ボタンなどが表示される前の初期化処理など
	// onCreateメソッドをオーバーライドとして記述していく
	public void onCreate(Bundle savedInstanceState) {
		// onCreateをオーバーライドする場合、スーパークラスのメソッドを呼び出す必要がある
		super.onCreate(savedInstanceState);

		// 起動時にソフトキーボードの立ち上がりを防ぐ
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// アクティビティにビューグループを追加する
		setContentView(R.layout.main);

		// IDに対応するViewを取得、型が異なるのでキャスト
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);

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
			UdpListener udp_listener = new UdpListener(new Handler(),
					text_view_received, 12345, 100);
			// スレッドを取得
			udpListenerThread = new Thread(udp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		// 受信スレッドrun()
		udpListenerThread.start();

		// 経路監視スレッドのインスタンスを作成
		try {
			RouteManager route_manager = new RouteManager(new Handler(),
					editTextDestPort,text_view_received);
			// スレッドを取得
			routeManagerThread = new Thread(route_manager);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		// 監視スレッドrun()
		routeManagerThread.start();

		// 送信Button、同様にIDから取得
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// クリック処理、匿名クラス(その場限りの名前の無いクラス)を利用
		// ボタン毎に、ビューを意識せずに処理を記述できる
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// editTextから送信先IP(String)、送信元(String)、port(int)の取得
				String destination_address = editTextDest.getText().toString();
				String source_address = editTextSrc.getText().toString();
				final int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());

				// 送信先,送信元IPアドレスのbyte[]化
				final byte[] destination_address_b = new RREQ()
						.getByteAddress(destination_address);
				
				final byte[] source_address_b = new RREQ()
				.getByteAddress(source_address);

				// 送信先への経路が存在するかチェック
				final int index = searchToAdd(routeTable, destination_address_b);

				// 経路が存在する場合、有効かどうかチェック
				boolean enableRoute = false; // 初期化

				if (index != -1) {
					if (getRoute(index).stateFlag == 1
							&& (getRoute(index).lifeTime > new Date().getTime())) {
						enableRoute = true;
					}
				}

				// ********* 経路が既に存在する場合 *******
				if (enableRoute) {
					// メッセージの送信
					sendMessage(getRoute(index).nextIpAdd, destination_port
							, destination_address_b, source_address_b);
					
					// 送信したことを表示
					text_view_received.append(editTextToBeSent.getText().toString()
							+ "-->" + destination_address+"\n");
				}

				// *********** 経路が存在しない場合 ***********
				else {
					text_view_received.append("Try Connect...\n");

					// 自身のシーケンス番号をインクリメント
					seqNum++;

					// TTLを初期値または過去のホップ数+TTL_ｲﾝｸﾘﾒﾝﾄにセット
					// 宛先シーケンス番号(+未知フラグ)もまとめてセット
					final boolean flagU;
					final int seqValue;

					// 無効経路が存在するなら、その情報を流用
					if (index != -1) {
						TTL_VALUE = getRoute(index).hopCount + TTL_INCREMENT;
						flagU = false;
						seqValue = getRoute(index).toSeqNum;
					}
					else{
						TTL_VALUE = TTL_START;
						flagU = true;
						seqValue = 0;
					}

					// ExpandingRingSearch
					// タイマーの初期化、インスタンスの生成
					final Timer mTimer;
					mTimer = new Timer(true);
					final Handler mHandler = new Handler();
					
					mTimer.scheduleAtFixedRate( new TimerTask(){
						@Override
						public void run(){
							
							// 描画処理があるのでhandlerに投げる
							mHandler.post(new Runnable(){
								public void run(){
									
									// 以下、定期処理の内容						
									// 経路が完成した場合、ループを抜ける
									if (searchToAdd(routeTable, destination_address_b) != -1) {
										text_view_received.append("Route_Create_Success!!\n");
										mTimer.cancel();
										
										// メッセージの送信
										sendMessage(getRoute(index).nextIpAdd, destination_port
												, destination_address_b, source_address_b);
										
										text_view_received.append(editTextToBeSent.getText().toString()
												+ "-->" + editTextDest.getText().toString() +"\n");
									}
									
									// TTLが上限値ならその送信を行った後、ループを抜ける
									if (TTL_VALUE == TTL_THRESHOLD){
										text_view_received.append("Failed\n");
										mTimer.cancel();
									}

									// TTLの微調整
									// 例えばINCREMENT2,THRESHOLD7のとき,TTLの変化は2->4->6->7(not 8)
									if (TTL_VALUE > TTL_THRESHOLD) {
										TTL_VALUE = TTL_THRESHOLD;
									}

									// RREQ_IDをインクリメント
									RREQ_ID++;

									// 自分が送信したパケットを受信しないようにIDを登録
									byte[] myAdd = source_address_b;
									
									newPastRReq(RREQ_ID, myAdd);

									// RREQの送信
									do_BroadCast = true;

									try {
										new RREQ().send(destination_address_b, myAdd,
												false, false, false, false, flagU,
												seqValue, seqNum, RREQ_ID, TTL_VALUE,
												destination_port);
									} catch (Exception e) {
										e.printStackTrace();
									}

									// ちょっと強引な待機(本来はRREPが戻ってくれば待たなくていい時間も待っている)
									// 待ち時間をVALUEに合わせて更新
									RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
											* (TTL_VALUE + TIMEOUT_BUFFER);
									
									TTL_VALUE += TTL_INCREMENT;
								}
							});
						}
					},0,RING_TRAVERSAL_TIME);
				}
			}
		});

		// 送信Clear、同様にIDから取得
		Button buttonClear = (Button) findViewById(R.id.buttonClear);

		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				text_view_received.setText("");
			}
		});
		
		// ルートテーブル表示ボタン
		Button buttonShowRouteTable = (Button) findViewById(R.id.buttonShowRouteTable);
		
		buttonShowRouteTable.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// ルートテーブルの表示
				if(AODV_Activity.routeTable.isEmpty()){
					text_view_received.append("Route_NotFound\n");
				}
				else{
					RouteTable route;
					
					text_view_received.append("ToIp,NextIp,Hop,Enable\n");
					for(int i=0;i<AODV_Activity.routeTable.size();i++){
						// i番目の経路を取得
						route = AODV_Activity.getRoute(i);
						
						text_view_received.append( getStringByByteAddress(route.toIpAdd) +",");
						text_view_received.append( getStringByByteAddress(route.nextIpAdd) +",");
						text_view_received.append( route.hopCount +",");
						
						if(route.stateFlag == 1)
							text_view_received.append("OK\n");
						else
							text_view_received.append("NG\n");
					}
					
					text_view_received.append(AODV_Activity.routeTable.size()+" RouteFound\n");
					
					text_view_received.setSelection(text_view_received.getText().length());
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
		text_view_received.setHeight(display_height - received_top
				- clear_height - 50);
	}
	
	

	// 送信先IPをローカルファイルに保存
	private void save_ip_to_local(String s) {

		try {
			OutputStream out = openFileOutput("ip.txt", MODE_APPEND
					| MODE_PRIVATE);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,
					"UTF-8"));
			writer.append(s + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// ルートテーブル中のi番目の要素を返す、排他制御
	public static RouteTable getRoute(int index) {
		synchronized (routeLock) {
			return routeTable.get(index);
		}
	}

	// ルートテーブルに要素を追加する、排他制御
	public static void addRoute(RouteTable route) {
		synchronized (routeLock) {
			routeTable.add(route);
		}
	}

	// ルートテーブルの要素を削除する、排他制御
	public static void removeRoute(int index) {
		synchronized (routeLock) {
			routeTable.remove(index);
		}
	}

	// ルートテーブルの要素を上書きする、排他制御
	public static void setRoute(int index, RouteTable route) {
		synchronized (routeLock) {
			routeTable.set(index, route);
		}
	}

	// RouteTable(list)に宛先アドレス(Add)が含まれていないか検索する
	// 戻り値：リスト内で発見した位置、インデックス
	// 見つからない場合 -1を返す
	public static int searchToAdd(ArrayList<RouteTable> list, byte[] Add) {

		synchronized (routeLock) {
			for (int i = 0; i < list.size(); i++) {
				if (Arrays.equals((list.get(i).toIpAdd), Add)) {
					return i;
				}
			}
		}

		return -1;
	}

	// 短い間のRREQ受信履歴中に、引数のID,アドレスのものが無いか検索
	public static boolean RREQ_ContainCheck(int ID, byte[] Add) {

		synchronized (pastDataLock) {
			for (int i = 0; i < receiveRREQ_List.size(); i++) {
				if ((ID == receiveRREQ_List.get(i).RREQ_ID)
						&& Arrays.equals(Add, receiveRREQ_List.get(i).IpAdd)) {
					return true;
				}
			}
		}
		return false;
	}

	// 同時に参照が起こらないよう、リストに追加するメソッド
	public static void newPastRReq(int IDnum, byte[] FromIpAdd) {

		synchronized (pastDataLock) {
			receiveRREQ_List.add(new PastData(IDnum, FromIpAdd, new Date()
					.getTime() + PATH_DISCOVERY_TIME));
		}
	}

	// 送信textの先頭に、AODVとは無関係であるメッセージタイプ0を挿入する
	// 以下はフォーマット、[数字]は配列内の位置を示す
	// [0]		:メッセージタイプ0
	// [1-4]	:宛先アドレス
	// [5-9]	:送信元アドレス
	// [10-?]	:データ
	private byte[] addMessageType(byte[] message,byte[] toIPAddress,byte[] my_address) {
		byte[] new_message = new byte[message.length + 1 + 4 + 4];

		new_message[0] = 0; // メッセージタイプ0
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 1+4, 4);
		//System.arraycopy(new RREQ().intToByte(message_ID), 0, new_message, 1+4+4, 4);	// intのbyte[]化
		System.arraycopy(message, 0, new_message, 1+4+4+4, message.length);

		return new_message;
	}
	private void sendMessage(byte[] destination_next_hop_address_b, int destination_port
			, byte[] destination_address_b, byte[] source_address_b){
		// 次ホップをルートテーブルから参照
		InetAddress next_hop_Inet = null;
		try {
			next_hop_Inet = InetAddress
					.getByAddress(destination_next_hop_address_b);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		// 送信先情報
		InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				next_hop_Inet, destination_port);

		// 送信する文字列の取得
		String string_to_be_sent = editTextToBeSent.getText()
				.toString() + "\r\n";

		// ### デバッグ用 ###
		// Date date1 = new Date(new Date().getTime());
		// string_to_be_sent = date1.toString()+"\n\r";

		// Byte列化
		byte[] buffer = string_to_be_sent.getBytes();
		
		// メッセージIDをインクリメント
		message_ID++;

		// メッセージタイプ0,宛先アドレス,送信元アドレス,メッセージIDを先頭に付加
		buffer = addMessageType(buffer, destination_address_b, source_address_b);

		// 送信try
		try {
			// LogCatに送信情報を出力
			Log.d("AODV", "sending " + string_to_be_sent);
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

		// 送信先IPをローカルファイルに保存
		// save_ip_to_local(destination_address);
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
	
	// IPアドレス(byte配列)から文字列(例:"127.0.0.1")へ変換
	public static String getStringByByteAddress(byte[] ip_address){
		
		if(ip_address.length != 4){
			return "Erorr_RouteIpAddress_is_not_correct";
		}
		
		// byteを符号無し整数に変換
		// 負なら+256
		int[] unsigned_b = new int[4];
		for(int i=0;i<4;i++){
			if(ip_address[i] >= 0){
				// 0以上ならそのまま
				unsigned_b[i] = ip_address[i];
			}
			else{
				unsigned_b[i] = ip_address[i]+256;
			}
		}
		return unsigned_b[0]+"."+unsigned_b[1]+"."+unsigned_b[2]+"."+unsigned_b[3];
	}
}