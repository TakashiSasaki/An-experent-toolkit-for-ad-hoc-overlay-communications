package jp.ac.ehime_u.cite.udptest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewDebug.IntToString;
import android.widget.Button;
import android.widget.EditText;

public class AODV_Activity extends Activity {

	// メンバ変数
	// EditTextクラス(ip入力やボタン押下などユーザーの入力を処理するため)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	static EditText editTextDest;
	private EditText editTextDestPort;
	private static EditText editTextToBeSent;
	
	public static Context context;
	
	// スレッド
	private static Thread udpListenerThread; // 受信スレッド
	private static Thread routeManagerThread; // ルート監視スレッド
	public static boolean timer_stop = false;	//ExpandingRingSerchを終了するためのもの
	
	// ルートテーブル
	protected static ArrayList<RouteTable> routeTable = new ArrayList<RouteTable>();

	// PATH_DISCOVERY_TIMEの間に受信したRREQの送信元とIDを記録
	public static ArrayList<PastData> receiveRREQ_List = new ArrayList<PastData>();
	

	// マルチスレッドの排他制御用オブジェクト
	public static Object routeLock = new Object();
	public static Object pastDataLock = new Object();
	public static Object fileManagerLock = new Object();
	public static Object fileReceivedManagerLock = new Object();

	// その他変数
	public static int RREQ_ID = 0;
	public static int seqNum = 0;
	public static boolean do_BroadCast = false; // 一定時間内に何かﾌﾞﾛｰﾄﾞｷｬｽﾄしたかどうか
	
	// ファイル送信
	public static ArrayList<FileManager> file_manager = new ArrayList<FileManager>();
	
	// インテントの多重処理制御
	private static int aodv_count = 0;
	private static String prev_receive_package_name = null;
	private static int prev_receive_intent_id = -1;

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
	public static final int BLACKLIST_TIMEOUT = RREQ_RETRIES * NET_TRAVERSAL_TIME;
	public static final int TIMEOUT_BUFFER = 2;
	public static final int TTL_START = 1;
	public static final int TTL_INCREMENT = 2;
	public static final int TTL_THRESHOLD = 7;
	public static int TTL_VALUE = 1; // IPヘッダ内の"TTL"フィールドの値
	public static int RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
			* (TTL_VALUE + TIMEOUT_BUFFER);
	public static int MAX_SEND_FILE_SIZE = 63*1024;
	public static int MAX_RESEND = 5;
	public static String BLOAD_CAST_ADDRESS = "255.255.255.255";

	/** Called when the activity is first created. */
	@Override
	// オーバーライド(親･子クラスでメソッドが競合したとき子クラスの宣言で上書き)
	// ボタンなどが表示される前の初期化処理など
	// onCreateメソッドをオーバーライドとして記述していく
	public void onCreate(Bundle savedInstanceState) {
		// onCreateをオーバーライドする場合、スーパークラスのメソッドを呼び出す必要がある
		super.onCreate(savedInstanceState);
		
		// 起動数++
		aodv_count++;
		
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
		
		// 暗黙的Intentの回収
		Intent receive_intent = getIntent();
		
		// 受信ログ用のTextView、同様にIDから取得
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		// Intentからパッケージ名,ID取得
		String package_name = receive_intent.getStringExtra("PACKAGE");
		int intent_id = receive_intent.getIntExtra("ID", 0);
		
		Log.d("intent",package_name+"-"+intent_id);
		
		// 同インテントの多重処理防止 パッケージ名またはIDが異なっていれば受理
		if( (package_name != prev_receive_package_name)
				|| intent_id != prev_receive_intent_id){
			
			// 直前のパッケージ,IDとして記録
			prev_receive_package_name = package_name;
			prev_receive_intent_id = intent_id;
			
			// 起動方法のチェック 暗黙的インテント:SENDTOで起動されていれば
			if(Intent.ACTION_SENDTO.equals(receive_intent.getAction())){
				final Uri uri = receive_intent.getData();
				String task = receive_intent.getStringExtra("TASK");
				
				// schemeが"connect"なら
				if("connect".equals(uri.getScheme())){
					// 変数内の値が消えている場合がある？
					//editTextDest = (EditText)findViewById(R.id.editTextDest);
					//editTextToBeSent = (EditText)findViewById(R.id.editTextToBeSent);
					
					editTextDest.setText(uri.getEncodedSchemeSpecificPart());
					editTextToBeSent.setText(task);
					
					// 自動送信を試みる
					// editTextから送信先IP(String)、送信元(String)、port(int)の取得
					String destination_address = editTextDest.getText().toString();	Log.d("eeeee",destination_address);
					String source_address = editTextSrc.getText().toString();
					final int destination_port = Integer.parseInt(editTextDestPort
							.getText().toString());
					
					byte[] destination_address_b = new RREQ().getByteAddress(destination_address);
					byte[] source_address_b	= new RREQ().getByteAddress(source_address);
					
					// UIの出力先を取得
					//final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
					
					// 送信先への経路が存在するかチェック
					final int index = searchToAdd(destination_address_b);

					// 経路が存在する場合、有効かどうかチェック
					boolean enableRoute = false; // 初期化
					
					if (index != -1) {
						if ( getRoute(index).stateFlag == 1 && 
								(getRoute(index).lifeTime > new Date().getTime())) {
							enableRoute = true;
						}
					}
					
					Context etc_context = context;
					// ファイルオープン用に他パッケージアプリのコンテキストを取得
					if( package_name != null ){
						try {
							etc_context = createPackageContext(package_name,0);
						} catch (NameNotFoundException e) {
							e.printStackTrace();
						}
					}
					
					// ********* 経路が既に存在する場合 *******
					if (enableRoute) {
						// メッセージの送信
						sendMessage(getRoute(index).nextIpAdd, getRoute(index).hopCount, destination_port
								, destination_address_b, source_address_b, etc_context);
						
						// 送信したことを表示
						text_view_received.append(editTextToBeSent.getText().toString()
								+ "-->" + destination_address+"\n");
					}
					// *********** 経路が存在しない場合 ***********
					else {
						text_view_received.append("Try Connect...\n");
						
						// 経路作成
						routeCreate(destination_address, source_address, destination_port, index
								, text_view_received, etc_context);
					}
				}
			}
			
			// 起動方法のチェック 暗黙的インテント:DELETEで起動されていれば
			if(Intent.ACTION_DELETE.equals(receive_intent.getAction())){
				final Uri uri = receive_intent.getData();
				
				if("path".equals(uri.getScheme())){
					deleteFile(uri.getEncodedSchemeSpecificPart());
				}
			}
		}
		
		// AODVが多重起動されたならここで終了
		if( aodv_count > 1){
			Log.d("AODV","double_start_to_finish");
			finish();
		}
		
		// スレッドが起動中でなければ
		if( udpListenerThread == null ){
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
		}
		
		if( routeManagerThread == null){
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
		}
		
		
		// ファイルオープン、同様にIDから取得しクリックイベントを追加
		Button buttonFileOpen = (Button) findViewById(R.id.buttonFileOpen);
		
		buttonFileOpen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SelectFile();
			}
		});
		
		context = this;
		

		// 送信Button、同様にIDから取得
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// クリック処理、匿名クラス(その場限りの名前の無いクラス)を利用
		// ボタン毎に、ビューを意識せずに処理を記述できる
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// editTextから送信先IP(String)、送信元(String)、port(int)の取得
				String destination_address = editTextDest.getText().toString();	Log.d("eeeee",destination_address);
				String source_address = editTextSrc.getText().toString();
				final int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				
				byte[] destination_address_b = new RREQ().getByteAddress(destination_address);
				byte[] source_address_b	= new RREQ().getByteAddress(source_address);
				
				// 送信先への経路が存在するかチェック
				final int index = searchToAdd(destination_address_b);

				// 経路が存在する場合、有効かどうかチェック
				boolean enableRoute = false; // 初期化
				
				if (index != -1) {
					if ( getRoute(index).stateFlag == 1 && 
							(getRoute(index).lifeTime > new Date().getTime())) {
						enableRoute = true;
					}
				}
				
				// ********* 経路が既に存在する場合 *******
				if (enableRoute) {
					// メッセージの送信
					sendMessage(getRoute(index).nextIpAdd, getRoute(index).hopCount, destination_port
							, destination_address_b, source_address_b, context);
					
					// 送信したことを表示
					text_view_received.append(editTextToBeSent.getText().toString()
							+ "-->" + destination_address+"\n");
				}
				// *********** 経路が存在しない場合 ***********
				else {
					text_view_received.append("Try Connect...\n");
					
					// 経路作成
					routeCreate(destination_address, source_address, destination_port, index
							, text_view_received, context);
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
	
//	@Override
//	protected void onResume() {
//		super.onResume();
//		
//
//	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.d("ondes","onDestroy()");
		// 起動数--
		aodv_count--;
	}
	
	// ルート作成＋メッセージ送信
	public static void routeCreate(String destination_address, String source_address, final int destination_port
			, int search_result, EditText output_view, final Context context_){

		Log.d("debug", "Send__Start");

		// 送信先,送信元IPアドレスのbyte[]化
		final byte[] destination_address_b = new RREQ()
				.getByteAddress(destination_address);
		
		final byte[] source_address_b = new RREQ()
		.getByteAddress(source_address);
		
		// 検索結果を利用
		int index = search_result;
		
		// 画面出力先
		final EditText text_view_received = output_view;
		
		// 自身のシーケンス番号をインクリメント
		seqNum++;
		
		// もし宛先がブロードキャストアドレスならExpandingRingSearchを行わない
		if( BLOAD_CAST_ADDRESS.equals(destination_address)){
			
			// メッセージ抽出
			String text = editTextToBeSent.getText().toString();
			
			// RREQ_IDをインクリメント
			RREQ_ID++;

			// 自分が送信したパケットを受信しないようにIDを登録
			newPastRReq(RREQ_ID, source_address_b);

			// RREQの送信
			do_BroadCast = true;

			try {
				new RREQ().send(destination_address_b,
								source_address_b,
								false,
								false,
								true,
								false,
								true,
								0,
								seqNum,
								RREQ_ID,
								NET_DIAMETER,
								destination_port,
								text);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// 宛先が通常のIPアドレスなら
		else{
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
			timer_stop = false;		//timer_stopを初期化
			final Handler mHandler = new Handler();
			
			RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME * (TTL_VALUE + TIMEOUT_BUFFER);
			
			try {
				new Thread(new Runnable() {
					public void run() {
						timer: while (true) {
	
							mHandler.post(new Runnable() {
								public void run() {
									int index_new;
									byte[] myAdd = source_address_b;
									
									// Threadは必ずぴったり停止するとは限らないので、停止しなくても中の処理は実行されないようにする
									if (!timer_stop) {
	
										// 以下、定期処理の内容
										// 経路が完成した場合、ループを抜ける
										if ( (index_new =searchToAdd(destination_address_b)) != -1) {
											text_view_received
													.append("Route_Create_Success!!\n");
											
											timer_stop = true;
											
											// メッセージの送信
											RouteTable rt = getRoute(index_new);
											sendMessage(rt.nextIpAdd, rt.hopCount, 
													destination_port, rt.toIpAdd, myAdd, context_);
											
											// 送信したことを表示
											text_view_received.append(editTextToBeSent.getText().toString()
													+ "-->" + getStringByByteAddress(rt.toIpAdd)+"\n");
										}
	
										// TTLが上限値なRREQを送信済みならループを抜ける
										else if (TTL_VALUE == (TTL_THRESHOLD + TTL_INCREMENT)) {
											text_view_received
													.append("Failed\n");
											timer_stop = true;
										}
	
										// TTLの微調整
										// 例えばINCREMENT2,THRESHOLD7のとき,TTLの変化は2->4->6->7(not
										// 8)
										if (TTL_VALUE > TTL_THRESHOLD) {
											TTL_VALUE = TTL_THRESHOLD;
										}
	
										// RREQ_IDをインクリメント
										RREQ_ID++;
	
										// 自分が送信したパケットを受信しないようにIDを登録
										newPastRReq(RREQ_ID, myAdd);
	
										// RREQの送信
										do_BroadCast = true;
	
										try {
											new RREQ().send(destination_address_b,
															myAdd,
															false,
															false,
															false,
															false,
															flagU,
															seqValue,
															seqNum,
															RREQ_ID,
															TTL_VALUE,
															destination_port,
															null);
										} catch (Exception e) {
											e.printStackTrace();
										}
	
										// ちょっと強引な待機(本来はRREPが戻ってくれば待たなくていい時間も待っている)
										// 待ち時間をVALUEに合わせて更新
										RING_TRAVERSAL_TIME = 2
												* NODE_TRAVERSAL_TIME
												* (TTL_VALUE + TIMEOUT_BUFFER);
	
										TTL_VALUE += TTL_INCREMENT;
									}
	
								}
	
							});
							// 指定の時間停止する
							try {
								Thread.sleep(RING_TRAVERSAL_TIME);
							} catch (InterruptedException e) {
							}
	
							// ループを抜ける処理
							if (timer_stop) {
								break timer;
							}
						}
					}
				}).start();
	
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
	}
	
	public static	SelectFileDialog	_dlgSelectFile;

	
	private	void	SelectFile()
	{
		//ここで画面回転を固定すべき（画面が固定されていないなら）

		_dlgSelectFile = new SelectFileDialog(context,new Handler(),editTextToBeSent);
		_dlgSelectFile.Show("/data/data/jp.ac.ehime_u.cite.udptest/files/");
	}
	
	@Override
	public void onPause()
	{
		if(_dlgSelectFile != null)
			_dlgSelectFile.onPause();
		
		super.onPause();
	}

	
	
	// メニューの追加
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0 , Menu.FIRST , Menu.NONE 
				, getString(R.string.menu_next)).setIcon(android.R.drawable.ic_menu_crop);
		menu.add(0 , Menu.FIRST + 1 ,Menu.NONE 
				, getString(R.string.menu_finish)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		return ret;
	}
	
	// メニューが押されたとき
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        // ルートテーブルメニューが押されたとき
        case Menu.FIRST:
            //別のActivityを起動させる
            Intent intent = new Intent();
            intent.setClassName(
                    "jp.ac.ehime_u.cite.udptest",
                    "jp.ac.ehime_u.cite.udptest.RouteActivity");
            startActivity(intent);
            
        	return true;
        // 終了メニューが押されたとき
        case Menu.FIRST + 1:
        	
//            Intent intent1 = new Intent();
//            intent1.setAction(Intent.ACTION_CALL);
//            intent1.setData(Uri.parse("CameraCapture:0_300_200_122.11.1.1"));	// TASK:〇の部分をセット
//            intent1.putExtra("PACKAGE","jp.ac.ehime_u.cite.udptest");
//            intent1.putExtra("ID", 1);
//            AODV_Activity.context.startActivity(intent1);
            //Activity終了
            //this.moveTaskToBack(true);
        	
        	//udpListenerThread.destroy();
        	finish();
        	
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
	
	
	// ログの表示用EditTextのサイズを画面サイズに合わせて動的に決定
	// OnCreate()ではまだViewがレイアウトが初期化されていないため？
	// Viewサイズなどの取得が不可
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		super.onWindowFocusChanged(hasFocus);
//		
//		// receivedのY座標を取得 * タイトルバー,ステータスバーの下が0扱い *
//		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
//		final int received_top = text_view_received.getTop();
//
//		// Clearのサイズを取得
//		final Button clear_button = (Button) findViewById(R.id.buttonClear);
//		final int clear_height = clear_button.getHeight();
//
//		// 画面サイズを取得 *タイトルバー,ステータスバー含む*
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//		Display display = wm.getDefaultDisplay();
//		final int display_height = display.getHeight();
//		
//		Log.d("gamen_size","height:"+display_height+",wifth"+display.getWidth());
//
//		// タイトル+ステータスバーのサイズを50と仮定、不完全な動的決定
//		text_view_received.setHeight(display_height - received_top
//				- clear_height - 50);
//	}
	
	

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
//		if(RunRouteActivity){
//			RouteActivity.addRoute_sql(route.toIpAdd, route.hopCount
//					, route.lifeTime - new Date().getTime(), route.stateFlag);
//		}
	}

	// ルートテーブルの要素を削除する、排他制御
	public static void removeRoute(int index) {
//		if(RunRouteActivity){
//			RouteActivity.removeRoute_sql(getRoute(index).toIpAdd);
//		}
		synchronized (routeLock) {
			routeTable.remove(index);
		}

	}

	// ルートテーブルの要素を上書きする、排他制御
	public static void setRoute(int index, RouteTable route) {
		synchronized (routeLock) {
			routeTable.set(index, route);
		}
//		if(RunRouteActivity){
//			RouteActivity.setRoute_sql();
//		}
	}

	// RouteTable(list)に宛先アドレス(Add)が含まれていないか検索する
	// 戻り値：リスト内で発見した位置、インデックス
	// 見つからない場合 -1を返す
	public static int searchToAdd(byte[] Add) {

		synchronized (routeLock) {
			for (int i = 0; i < routeTable.size(); i++) {
				if (Arrays.equals((routeTable.get(i).toIpAdd), Add)) {
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
	// [5-8]	:送信元アドレス
	// [9-?]	:データ
	private static byte[] addMessageTypeString(byte[] message,byte[] toIPAddress,byte[] my_address) {
		byte[] new_message = new byte[1 + 4 + 4 + message.length];

		new_message[0] = 0; // メッセージタイプ0
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 1+4, 4);
		System.arraycopy(message, 0, new_message, 1+4+4, message.length);

		return new_message;
	}
	
	// ファイル送信用タイプ10やその他制御データを付加する
	// 以下はフォーマット、[数字]は配列内の位置を示す
	// [0]		: メッセージタイプ10
	// [1-4]	: 宛先アドレス
	// [5-8]	: 送信元アドレス
	// [9-12]	: 分割した何番目のデータか	* このメソッドでは代入しない *
	// [13-16]	: いくつに分割したか
	// [17-20]	: ファイル名のサイズ
	// [21-??]	: ファイル名(可変長)
	// [??-??]	: ファイルデータ(可変長,最大63K[byte]) * このメソッドでは代入しない *
	private byte[] addMessageTypeFile(int fileSize,byte[] toIPAddress,byte[] my_address,
			String fileName,int step){
		
		byte[] fileName_b = fileName.getBytes();	// ファイル名をbyte化
		byte[] fileName_size_b = intToByte(fileName_b.length);	// byte型ファイル名のサイズをbyte化
		byte[] step_b = intToByte(step);			// 分割数をbyte化
		
		byte[] new_message = new byte[21 + fileName_b.length + fileSize];
		
		new_message[0] = 10;	// メッセージタイプ10
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 5, 4);
		// [9-12] 分割した何番目のデータか を代入しない
		System.arraycopy(step_b, 0, new_message, 13, 4);
		System.arraycopy(fileName_size_b, 0, new_message, 17, 4);
		System.arraycopy(fileName_b, 0, new_message, 21, fileName_b.length);
		// [??-??] ファイルデータを代入しない
		
		return new_message;
	}
	
	public static void sendMessage(byte[] destination_next_hop_address_b, int hop_count, int destination_port
			, byte[] destination_address_b, byte[] source_address_b, Context context_){
		
		//editTextToBeSent = (EditText)findViewById(R.id.editTextToBeSent);
		final String text = editTextToBeSent.getText().toString();
		int index;
		
		try{
			// 古すぎる送信データを削除
			while( (index=searchLifeTimeEmpty()) != -1){
				try {
					AODV_Activity.file_manager.get(index).file_in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					AODV_Activity.file_manager.get(index).file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				AODV_Activity.file_manager.remove(index);
			}
			// ファイルが送信中なら送信中止
			if(searchProgress(text, destination_address_b) != null){
				Log.d("FILE_SEND","this_file_sending_now");
			}
			else{
				// ファイルオープン
				FileManager	files = new FileManager(text, destination_address_b,
							source_address_b, context_);
				
				// Log.d 開始時間
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("MMdd'_'HHmmss");
				
				String log = "time:"+sdf.format(date);
				Log.d("SEND_T",log);
				
				// 分割パケットの最初の１つを送信
				files.fileSend(source_address_b, destination_next_hop_address_b, destination_port);
				
				// 過程を保持 *分割後,パケットが1つのみでも再送が有りうるので必要*
				files.add();
				
				// タイムアウトを起動
				final int time = 2 * NODE_TRAVERSAL_TIME * (hop_count + TIMEOUT_BUFFER);
				final int step = files.file_next_no;
				final byte[] data = files.buffer;
				final byte[] dest_next_hop_add = destination_next_hop_address_b;
				final int port_ = destination_port;
				final String name = files.file_name;
				final byte[] dest_add = files.destination_address;
				
				final Handler mHandler = new Handler();
				
				try {
					new Thread(new Runnable() {
						
						int wait_time = time;
						int resend_count = 0;
						int prev_step = step;
						byte[] buffer = data;
						byte[] destination_next_hop_address_b = dest_next_hop_add;
						int port = port_;
						String file_name = name;
						byte[] destination_address = dest_add;
						
						// 再送処理
						public void run() {
							timer: while (true) {
								
								mHandler.post(new Runnable() {
									public void run() {
										
										// 送信try
										try {
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
													next_hop_Inet.getHostAddress(), port);
											
											// 送信パケットの生成
											DatagramPacket packet_to_be_sent = new DatagramPacket(
													buffer, buffer.length,
													destination_inet_socket_address);
											// 送信用のクラスを生成、送信、クローズ
											DatagramSocket datagram_socket = new DatagramSocket();
											datagram_socket.send(packet_to_be_sent);
											datagram_socket.close();
										} catch (SocketException e1) {
											e1.printStackTrace();
										} catch (IOException e1) {
											e1.printStackTrace();
										}
										
									}
									
								});
								// 指定の時間停止する
								try {
									Thread.sleep(wait_time);
								} catch (InterruptedException e) {
								}
								
								resend_count++;
								
								// ループを抜ける処理
								if (resend_count == MAX_RESEND) {
									break timer;
								}
								FileManager files = searchProgress(file_name, destination_address);
								if(files == null){
									break timer;
								}
								else{
									if( files.file_next_no != prev_step){
										break timer;
									}
								}
								
							}
						}
					}).start();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e){
			// ファイルが開けない場合
			// **********テキストメッセージ:type0として送信*********
			
			// メッセージタイプ0,宛先アドレス,送信元アドレス,メッセージIDを先頭に付加
			byte[] buffer = addMessageTypeString(text.getBytes(),
					destination_address_b, source_address_b);
			
			// 送信try
			try {
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
						next_hop_Inet.getHostAddress(), destination_port);
				
				// 送信パケットの生成
				DatagramPacket packet_to_be_sent = new DatagramPacket(
						buffer, buffer.length,
						destination_inet_socket_address);
				// 送信用のクラスを生成、送信、クローズ
				DatagramSocket datagram_socket = new DatagramSocket();
				datagram_socket.send(packet_to_be_sent);
				datagram_socket.close();
			} catch (SocketException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		}
		

	}
	
	// ファイル名,宛先が等しい経過を返す
	// 存在しない場合はnullを返す
	public static FileManager searchProgress(String name,byte[] dest_add){
		synchronized(AODV_Activity.fileManagerLock){
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( name.equals(AODV_Activity.file_manager.get(i).file_name) 
						&& Arrays.equals(dest_add, AODV_Activity.file_manager.get(i).destination_address)){
					return AODV_Activity.file_manager.get(i);
				}
			}
		
			return null;
		}
	}
	
	// 生存時間が寿命であるindexを返す
	// 存在しない場合は-1
	public static int searchLifeTimeEmpty(){
		synchronized(AODV_Activity.fileManagerLock){
			long now = new Date().getTime();
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( AODV_Activity.file_manager.get(i).life_time < now ){
					return i;
				}
			}
			return -1;
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
	
	
	// int型をbyte[]型へ変換
	private byte[] intToByte(int num){
		
		// バイト配列への出力を行うストリーム
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// バイト配列への出力を行うストリームをDataOutputStreamと連結する
		DataOutputStream out = new DataOutputStream(bout);
			
		try{	// 数値を出力
			out.writeInt(num);
		}catch(Exception e){
				System.out.println(e);
		}
		
		// バイト配列をバイトストリームから取り出す
		byte[] bytes = bout.toByteArray();
		return bytes;
	}
}