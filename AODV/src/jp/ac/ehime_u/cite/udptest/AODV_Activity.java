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

	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	static EditText editTextDest;
	private EditText editTextDestPort;
	private static EditText editTextToBeSent;
	
	public static Context context;
	
	// �X���b�h
	private static Thread udpListenerThread; // ��M�X���b�h
	private static Thread routeManagerThread; // ���[�g�Ď��X���b�h
	public static boolean timer_stop = false;	//ExpandingRingSerch���I�����邽�߂̂���
	
	// ���[�g�e�[�u��
	protected static ArrayList<RouteTable> routeTable = new ArrayList<RouteTable>();

	// PATH_DISCOVERY_TIME�̊ԂɎ�M����RREQ�̑��M����ID���L�^
	public static ArrayList<PastData> receiveRREQ_List = new ArrayList<PastData>();
	

	// �}���`�X���b�h�̔r������p�I�u�W�F�N�g
	public static Object routeLock = new Object();
	public static Object pastDataLock = new Object();
	public static Object fileManagerLock = new Object();
	public static Object fileReceivedManagerLock = new Object();

	// ���̑��ϐ�
	public static int RREQ_ID = 0;
	public static int seqNum = 0;
	public static boolean do_BroadCast = false; // ��莞�ԓ��ɉ�����۰�޷��Ă������ǂ���
	
	// �t�@�C�����M
	public static ArrayList<FileManager> file_manager = new ArrayList<FileManager>();
	
	// �C���e���g�̑��d��������
	private static int aodv_count = 0;
	private static String prev_receive_package_name = null;
	private static int prev_receive_intent_id = -1;

	// �l�X�ȃp�����[�^�̃f�t�H���g�l��錾
	public static final int ACTIVE_ROUTE_TIMEOUT = 3000; // [ms]
	public static final int ALLOWED_HELLO_LOSS = 2;
	public static final int HELLO_INTERVAL = 1000; // [ms]
	public static final int DELETE_PERIOD = (ACTIVE_ROUTE_TIMEOUT >= HELLO_INTERVAL) ? 5 * ACTIVE_ROUTE_TIMEOUT
			: 5 * HELLO_INTERVAL;
	public static final int LOCAL_ADD_TTL = 2;
	public static final int MY_ROUTE_TIMEOUT = 2 * ACTIVE_ROUTE_TIMEOUT;
	public static final int NET_DIAMETER = 35;
	public static final int MAX_REPAIR_TTL = (int) (0.3 * NET_DIAMETER);
	public static int MIN_REPAIR_TTL = -1; // ����m�[�h�֒m���Ă���ŐV�̃z�b�v��
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
	public static int TTL_VALUE = 1; // IP�w�b�_����"TTL"�t�B�[���h�̒l
	public static int RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
			* (TTL_VALUE + TIMEOUT_BUFFER);
	public static int MAX_SEND_FILE_SIZE = 63*1024;
	public static int MAX_RESEND = 5;
	public static String BLOAD_CAST_ADDRESS = "255.255.255.255";

	/** Called when the activity is first created. */
	@Override
	// �I�[�o�[���C�h(�e��q�N���X�Ń��\�b�h�����������Ƃ��q�N���X�̐錾�ŏ㏑��)
	// �{�^���Ȃǂ��\�������O�̏����������Ȃ�
	// onCreate���\�b�h���I�[�o�[���C�h�Ƃ��ċL�q���Ă���
	public void onCreate(Bundle savedInstanceState) {
		// onCreate���I�[�o�[���C�h����ꍇ�A�X�[�p�[�N���X�̃��\�b�h���Ăяo���K�v������
		super.onCreate(savedInstanceState);
		
		// �N����++
		aodv_count++;
		
		// �N�����Ƀ\�t�g�L�[�{�[�h�̗����オ���h��
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// �A�N�e�B�r�e�B�Ƀr���[�O���[�v��ǉ�����
		setContentView(R.layout.main);

		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
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
		
		// �ÖٓIIntent�̉��
		Intent receive_intent = getIntent();
		
		// ��M���O�p��TextView�A���l��ID����擾
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		// Intent����p�b�P�[�W��,ID�擾
		String package_name = receive_intent.getStringExtra("PACKAGE");
		int intent_id = receive_intent.getIntExtra("ID", 0);
		
		Log.d("intent",package_name+"-"+intent_id);
		
		// ���C���e���g�̑��d�����h�~ �p�b�P�[�W���܂���ID���قȂ��Ă���Ύ�
		if( (package_name != prev_receive_package_name)
				|| intent_id != prev_receive_intent_id){
			
			// ���O�̃p�b�P�[�W,ID�Ƃ��ċL�^
			prev_receive_package_name = package_name;
			prev_receive_intent_id = intent_id;
			
			// �N�����@�̃`�F�b�N �ÖٓI�C���e���g:SENDTO�ŋN������Ă����
			if(Intent.ACTION_SENDTO.equals(receive_intent.getAction())){
				final Uri uri = receive_intent.getData();
				String task = receive_intent.getStringExtra("TASK");
				
				// scheme��"connect"�Ȃ�
				if("connect".equals(uri.getScheme())){
					// �ϐ����̒l�������Ă���ꍇ������H
					//editTextDest = (EditText)findViewById(R.id.editTextDest);
					//editTextToBeSent = (EditText)findViewById(R.id.editTextToBeSent);
					
					editTextDest.setText(uri.getEncodedSchemeSpecificPart());
					editTextToBeSent.setText(task);
					
					// �������M�����݂�
					// editText���瑗�M��IP(String)�A���M��(String)�Aport(int)�̎擾
					String destination_address = editTextDest.getText().toString();	Log.d("eeeee",destination_address);
					String source_address = editTextSrc.getText().toString();
					final int destination_port = Integer.parseInt(editTextDestPort
							.getText().toString());
					
					byte[] destination_address_b = new RREQ().getByteAddress(destination_address);
					byte[] source_address_b	= new RREQ().getByteAddress(source_address);
					
					// UI�̏o�͐���擾
					//final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
					
					// ���M��ւ̌o�H�����݂��邩�`�F�b�N
					final int index = searchToAdd(destination_address_b);

					// �o�H�����݂���ꍇ�A�L�����ǂ����`�F�b�N
					boolean enableRoute = false; // ������
					
					if (index != -1) {
						if ( getRoute(index).stateFlag == 1 && 
								(getRoute(index).lifeTime > new Date().getTime())) {
							enableRoute = true;
						}
					}
					
					Context etc_context = context;
					// �t�@�C���I�[�v���p�ɑ��p�b�P�[�W�A�v���̃R���e�L�X�g���擾
					if( package_name != null ){
						try {
							etc_context = createPackageContext(package_name,0);
						} catch (NameNotFoundException e) {
							e.printStackTrace();
						}
					}
					
					// ********* �o�H�����ɑ��݂���ꍇ *******
					if (enableRoute) {
						// ���b�Z�[�W�̑��M
						sendMessage(getRoute(index).nextIpAdd, getRoute(index).hopCount, destination_port
								, destination_address_b, source_address_b, etc_context);
						
						// ���M�������Ƃ�\��
						text_view_received.append(editTextToBeSent.getText().toString()
								+ "-->" + destination_address+"\n");
					}
					// *********** �o�H�����݂��Ȃ��ꍇ ***********
					else {
						text_view_received.append("Try Connect...\n");
						
						// �o�H�쐬
						routeCreate(destination_address, source_address, destination_port, index
								, text_view_received, etc_context);
					}
				}
			}
			
			// �N�����@�̃`�F�b�N �ÖٓI�C���e���g:DELETE�ŋN������Ă����
			if(Intent.ACTION_DELETE.equals(receive_intent.getAction())){
				final Uri uri = receive_intent.getData();
				
				if("path".equals(uri.getScheme())){
					deleteFile(uri.getEncodedSchemeSpecificPart());
				}
			}
		}
		
		// AODV�����d�N�����ꂽ�Ȃ炱���ŏI��
		if( aodv_count > 1){
			Log.d("AODV","double_start_to_finish");
			finish();
		}
		
		// �X���b�h���N�����łȂ����
		if( udpListenerThread == null ){
			try {
				// ��M�X���b�h�̃C���X�^���X���쐬
				UdpListener udp_listener = new UdpListener(new Handler(),
						text_view_received, 12345, 100);
				// �X���b�h���擾
				udpListenerThread = new Thread(udp_listener);
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			// ��M�X���b�hrun()
			udpListenerThread.start();
		}
		
		if( routeManagerThread == null){
			// �o�H�Ď��X���b�h�̃C���X�^���X���쐬
			try {
				RouteManager route_manager = new RouteManager(new Handler(),
						editTextDestPort,text_view_received);
				// �X���b�h���擾
				routeManagerThread = new Thread(route_manager);
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			// �Ď��X���b�hrun()
			routeManagerThread.start();
		}
		
		
		// �t�@�C���I�[�v���A���l��ID����擾���N���b�N�C�x���g��ǉ�
		Button buttonFileOpen = (Button) findViewById(R.id.buttonFileOpen);
		
		buttonFileOpen.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				SelectFile();
			}
		});
		
		context = this;
		

		// ���MButton�A���l��ID����擾
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// �N���b�N�����A�����N���X(���̏����̖��O�̖����N���X)�𗘗p
		// �{�^�����ɁA�r���[���ӎ������ɏ������L�q�ł���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// editText���瑗�M��IP(String)�A���M��(String)�Aport(int)�̎擾
				String destination_address = editTextDest.getText().toString();	Log.d("eeeee",destination_address);
				String source_address = editTextSrc.getText().toString();
				final int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				
				byte[] destination_address_b = new RREQ().getByteAddress(destination_address);
				byte[] source_address_b	= new RREQ().getByteAddress(source_address);
				
				// ���M��ւ̌o�H�����݂��邩�`�F�b�N
				final int index = searchToAdd(destination_address_b);

				// �o�H�����݂���ꍇ�A�L�����ǂ����`�F�b�N
				boolean enableRoute = false; // ������
				
				if (index != -1) {
					if ( getRoute(index).stateFlag == 1 && 
							(getRoute(index).lifeTime > new Date().getTime())) {
						enableRoute = true;
					}
				}
				
				// ********* �o�H�����ɑ��݂���ꍇ *******
				if (enableRoute) {
					// ���b�Z�[�W�̑��M
					sendMessage(getRoute(index).nextIpAdd, getRoute(index).hopCount, destination_port
							, destination_address_b, source_address_b, context);
					
					// ���M�������Ƃ�\��
					text_view_received.append(editTextToBeSent.getText().toString()
							+ "-->" + destination_address+"\n");
				}
				// *********** �o�H�����݂��Ȃ��ꍇ ***********
				else {
					text_view_received.append("Try Connect...\n");
					
					// �o�H�쐬
					routeCreate(destination_address, source_address, destination_port, index
							, text_view_received, context);
				}
			}
		});
		
		
		// ���MClear�A���l��ID����擾
		Button buttonClear = (Button) findViewById(R.id.buttonClear);

		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				text_view_received.setText("");
			}
		});
		
		
		// ���[�g�e�[�u���\���{�^��
		Button buttonShowRouteTable = (Button) findViewById(R.id.buttonShowRouteTable);
		
		buttonShowRouteTable.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// ���[�g�e�[�u���̕\��
				if(AODV_Activity.routeTable.isEmpty()){
					text_view_received.append("Route_NotFound\n");
				}
				else{
					RouteTable route;
					
					text_view_received.append("ToIp,NextIp,Hop,Enable\n");
					for(int i=0;i<AODV_Activity.routeTable.size();i++){
						// i�Ԗڂ̌o�H���擾
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
		// �N����--
		aodv_count--;
	}
	
	// ���[�g�쐬�{���b�Z�[�W���M
	public static void routeCreate(String destination_address, String source_address, final int destination_port
			, int search_result, EditText output_view, final Context context_){

		Log.d("debug", "Send__Start");

		// ���M��,���M��IP�A�h���X��byte[]��
		final byte[] destination_address_b = new RREQ()
				.getByteAddress(destination_address);
		
		final byte[] source_address_b = new RREQ()
		.getByteAddress(source_address);
		
		// �������ʂ𗘗p
		int index = search_result;
		
		// ��ʏo�͐�
		final EditText text_view_received = output_view;
		
		// ���g�̃V�[�P���X�ԍ����C���N�������g
		seqNum++;
		
		// �������悪�u���[�h�L���X�g�A�h���X�Ȃ�ExpandingRingSearch���s��Ȃ�
		if( BLOAD_CAST_ADDRESS.equals(destination_address)){
			
			// ���b�Z�[�W���o
			String text = editTextToBeSent.getText().toString();
			
			// RREQ_ID���C���N�������g
			RREQ_ID++;

			// ���������M�����p�P�b�g����M���Ȃ��悤��ID��o�^
			newPastRReq(RREQ_ID, source_address_b);

			// RREQ�̑��M
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
		
		// ���悪�ʏ��IP�A�h���X�Ȃ�
		else{
			// TTL�������l�܂��͉ߋ��̃z�b�v��+TTL_�ݸ���ĂɃZ�b�g
			// ����V�[�P���X�ԍ�(+���m�t���O)���܂Ƃ߂ăZ�b�g
			final boolean flagU;
			final int seqValue;
	
			// �����o�H�����݂���Ȃ�A���̏��𗬗p
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
			timer_stop = false;		//timer_stop��������
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
									
									// Thread�͕K���҂������~����Ƃ͌���Ȃ��̂ŁA��~���Ȃ��Ă����̏����͎��s����Ȃ��悤�ɂ���
									if (!timer_stop) {
	
										// �ȉ��A��������̓��e
										// �o�H�����������ꍇ�A���[�v�𔲂���
										if ( (index_new =searchToAdd(destination_address_b)) != -1) {
											text_view_received
													.append("Route_Create_Success!!\n");
											
											timer_stop = true;
											
											// ���b�Z�[�W�̑��M
											RouteTable rt = getRoute(index_new);
											sendMessage(rt.nextIpAdd, rt.hopCount, 
													destination_port, rt.toIpAdd, myAdd, context_);
											
											// ���M�������Ƃ�\��
											text_view_received.append(editTextToBeSent.getText().toString()
													+ "-->" + getStringByByteAddress(rt.toIpAdd)+"\n");
										}
	
										// TTL������l��RREQ�𑗐M�ς݂Ȃ烋�[�v�𔲂���
										else if (TTL_VALUE == (TTL_THRESHOLD + TTL_INCREMENT)) {
											text_view_received
													.append("Failed\n");
											timer_stop = true;
										}
	
										// TTL�̔�����
										// �Ⴆ��INCREMENT2,THRESHOLD7�̂Ƃ�,TTL�̕ω���2->4->6->7(not
										// 8)
										if (TTL_VALUE > TTL_THRESHOLD) {
											TTL_VALUE = TTL_THRESHOLD;
										}
	
										// RREQ_ID���C���N�������g
										RREQ_ID++;
	
										// ���������M�����p�P�b�g����M���Ȃ��悤��ID��o�^
										newPastRReq(RREQ_ID, myAdd);
	
										// RREQ�̑��M
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
	
										// ������Ƌ����ȑҋ@(�{����RREP���߂��Ă���Α҂��Ȃ��Ă������Ԃ��҂��Ă���)
										// �҂����Ԃ�VALUE�ɍ��킹�čX�V
										RING_TRAVERSAL_TIME = 2
												* NODE_TRAVERSAL_TIME
												* (TTL_VALUE + TIMEOUT_BUFFER);
	
										TTL_VALUE += TTL_INCREMENT;
									}
	
								}
	
							});
							// �w��̎��Ԓ�~����
							try {
								Thread.sleep(RING_TRAVERSAL_TIME);
							} catch (InterruptedException e) {
							}
	
							// ���[�v�𔲂��鏈��
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
		//�����ŉ�ʉ�]���Œ肷�ׂ��i��ʂ��Œ肳��Ă��Ȃ��Ȃ�j

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

	
	
	// ���j���[�̒ǉ�
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0 , Menu.FIRST , Menu.NONE 
				, getString(R.string.menu_next)).setIcon(android.R.drawable.ic_menu_crop);
		menu.add(0 , Menu.FIRST + 1 ,Menu.NONE 
				, getString(R.string.menu_finish)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);

		return ret;
	}
	
	// ���j���[�������ꂽ�Ƃ�
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        // ���[�g�e�[�u�����j���[�������ꂽ�Ƃ�
        case Menu.FIRST:
            //�ʂ�Activity���N��������
            Intent intent = new Intent();
            intent.setClassName(
                    "jp.ac.ehime_u.cite.udptest",
                    "jp.ac.ehime_u.cite.udptest.RouteActivity");
            startActivity(intent);
            
        	return true;
        // �I�����j���[�������ꂽ�Ƃ�
        case Menu.FIRST + 1:
        	
//            Intent intent1 = new Intent();
//            intent1.setAction(Intent.ACTION_CALL);
//            intent1.setData(Uri.parse("CameraCapture:0_300_200_122.11.1.1"));	// TASK:�Z�̕������Z�b�g
//            intent1.putExtra("PACKAGE","jp.ac.ehime_u.cite.udptest");
//            intent1.putExtra("ID", 1);
//            AODV_Activity.context.startActivity(intent1);
            //Activity�I��
            //this.moveTaskToBack(true);
        	
        	//udpListenerThread.destroy();
        	finish();
        	
            return true;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
	
	
	// ���O�̕\���pEditText�̃T�C�Y����ʃT�C�Y�ɍ��킹�ē��I�Ɍ���
	// OnCreate()�ł͂܂�View�����C�A�E�g������������Ă��Ȃ����߁H
	// View�T�C�Y�Ȃǂ̎擾���s��
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		super.onWindowFocusChanged(hasFocus);
//		
//		// received��Y���W���擾 * �^�C�g���o�[,�X�e�[�^�X�o�[�̉���0���� *
//		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
//		final int received_top = text_view_received.getTop();
//
//		// Clear�̃T�C�Y���擾
//		final Button clear_button = (Button) findViewById(R.id.buttonClear);
//		final int clear_height = clear_button.getHeight();
//
//		// ��ʃT�C�Y���擾 *�^�C�g���o�[,�X�e�[�^�X�o�[�܂�*
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
//		Display display = wm.getDefaultDisplay();
//		final int display_height = display.getHeight();
//		
//		Log.d("gamen_size","height:"+display_height+",wifth"+display.getWidth());
//
//		// �^�C�g��+�X�e�[�^�X�o�[�̃T�C�Y��50�Ɖ���A�s���S�ȓ��I����
//		text_view_received.setHeight(display_height - received_top
//				- clear_height - 50);
//	}
	
	

	// ���M��IP�����[�J���t�@�C���ɕۑ�
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

	// ���[�g�e�[�u������i�Ԗڂ̗v�f��Ԃ��A�r������
	public static RouteTable getRoute(int index) {
		synchronized (routeLock) {
			return routeTable.get(index);
		}
	}

	// ���[�g�e�[�u���ɗv�f��ǉ�����A�r������
	public static void addRoute(RouteTable route) {
		synchronized (routeLock) {
			routeTable.add(route);
		}
//		if(RunRouteActivity){
//			RouteActivity.addRoute_sql(route.toIpAdd, route.hopCount
//					, route.lifeTime - new Date().getTime(), route.stateFlag);
//		}
	}

	// ���[�g�e�[�u���̗v�f���폜����A�r������
	public static void removeRoute(int index) {
//		if(RunRouteActivity){
//			RouteActivity.removeRoute_sql(getRoute(index).toIpAdd);
//		}
		synchronized (routeLock) {
			routeTable.remove(index);
		}

	}

	// ���[�g�e�[�u���̗v�f���㏑������A�r������
	public static void setRoute(int index, RouteTable route) {
		synchronized (routeLock) {
			routeTable.set(index, route);
		}
//		if(RunRouteActivity){
//			RouteActivity.setRoute_sql();
//		}
	}

	// RouteTable(list)�Ɉ���A�h���X(Add)���܂܂�Ă��Ȃ�����������
	// �߂�l�F���X�g���Ŕ��������ʒu�A�C���f�b�N�X
	// ������Ȃ��ꍇ -1��Ԃ�
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

	// �Z���Ԃ�RREQ��M���𒆂ɁA������ID,�A�h���X�̂��̂�����������
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

	// �����ɎQ�Ƃ��N����Ȃ��悤�A���X�g�ɒǉ����郁�\�b�h
	public static void newPastRReq(int IDnum, byte[] FromIpAdd) {

		synchronized (pastDataLock) {
			receiveRREQ_List.add(new PastData(IDnum, FromIpAdd, new Date()
					.getTime() + PATH_DISCOVERY_TIME));
		}
	}

	
	// ���Mtext�̐擪�ɁAAODV�Ƃ͖��֌W�ł��郁�b�Z�[�W�^�C�v0��}������
	// �ȉ��̓t�H�[�}�b�g�A[����]�͔z����̈ʒu������
	// [0]		:���b�Z�[�W�^�C�v0
	// [1-4]	:����A�h���X
	// [5-8]	:���M���A�h���X
	// [9-?]	:�f�[�^
	private static byte[] addMessageTypeString(byte[] message,byte[] toIPAddress,byte[] my_address) {
		byte[] new_message = new byte[1 + 4 + 4 + message.length];

		new_message[0] = 0; // ���b�Z�[�W�^�C�v0
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 1+4, 4);
		System.arraycopy(message, 0, new_message, 1+4+4, message.length);

		return new_message;
	}
	
	// �t�@�C�����M�p�^�C�v10�₻�̑�����f�[�^��t������
	// �ȉ��̓t�H�[�}�b�g�A[����]�͔z����̈ʒu������
	// [0]		: ���b�Z�[�W�^�C�v10
	// [1-4]	: ����A�h���X
	// [5-8]	: ���M���A�h���X
	// [9-12]	: �����������Ԗڂ̃f�[�^��	* ���̃��\�b�h�ł͑�����Ȃ� *
	// [13-16]	: �����ɕ���������
	// [17-20]	: �t�@�C�����̃T�C�Y
	// [21-??]	: �t�@�C����(�ϒ�)
	// [??-??]	: �t�@�C���f�[�^(�ϒ�,�ő�63K[byte]) * ���̃��\�b�h�ł͑�����Ȃ� *
	private byte[] addMessageTypeFile(int fileSize,byte[] toIPAddress,byte[] my_address,
			String fileName,int step){
		
		byte[] fileName_b = fileName.getBytes();	// �t�@�C������byte��
		byte[] fileName_size_b = intToByte(fileName_b.length);	// byte�^�t�@�C�����̃T�C�Y��byte��
		byte[] step_b = intToByte(step);			// ��������byte��
		
		byte[] new_message = new byte[21 + fileName_b.length + fileSize];
		
		new_message[0] = 10;	// ���b�Z�[�W�^�C�v10
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 5, 4);
		// [9-12] �����������Ԗڂ̃f�[�^�� �������Ȃ�
		System.arraycopy(step_b, 0, new_message, 13, 4);
		System.arraycopy(fileName_size_b, 0, new_message, 17, 4);
		System.arraycopy(fileName_b, 0, new_message, 21, fileName_b.length);
		// [??-??] �t�@�C���f�[�^�������Ȃ�
		
		return new_message;
	}
	
	public static void sendMessage(byte[] destination_next_hop_address_b, int hop_count, int destination_port
			, byte[] destination_address_b, byte[] source_address_b, Context context_){
		
		//editTextToBeSent = (EditText)findViewById(R.id.editTextToBeSent);
		final String text = editTextToBeSent.getText().toString();
		int index;
		
		try{
			// �Â����鑗�M�f�[�^���폜
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
			// �t�@�C�������M���Ȃ瑗�M���~
			if(searchProgress(text, destination_address_b) != null){
				Log.d("FILE_SEND","this_file_sending_now");
			}
			else{
				// �t�@�C���I�[�v��
				FileManager	files = new FileManager(text, destination_address_b,
							source_address_b, context_);
				
				// Log.d �J�n����
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("MMdd'_'HHmmss");
				
				String log = "time:"+sdf.format(date);
				Log.d("SEND_T",log);
				
				// �����p�P�b�g�̍ŏ��̂P�𑗐M
				files.fileSend(source_address_b, destination_next_hop_address_b, destination_port);
				
				// �ߒ���ێ� *������,�p�P�b�g��1�݂̂ł��đ����L�肤��̂ŕK�v*
				files.add();
				
				// �^�C���A�E�g���N��
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
						
						// �đ�����
						public void run() {
							timer: while (true) {
								
								mHandler.post(new Runnable() {
									public void run() {
										
										// ���Mtry
										try {
											// ���z�b�v�����[�g�e�[�u������Q��
											InetAddress next_hop_Inet = null;
											try {
												next_hop_Inet = InetAddress
														.getByAddress(destination_next_hop_address_b);
											} catch (UnknownHostException e1) {
												e1.printStackTrace();
											}
	
											// ���M����
											InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
													next_hop_Inet.getHostAddress(), port);
											
											// ���M�p�P�b�g�̐���
											DatagramPacket packet_to_be_sent = new DatagramPacket(
													buffer, buffer.length,
													destination_inet_socket_address);
											// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
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
								// �w��̎��Ԓ�~����
								try {
									Thread.sleep(wait_time);
								} catch (InterruptedException e) {
								}
								
								resend_count++;
								
								// ���[�v�𔲂��鏈��
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
			// �t�@�C�����J���Ȃ��ꍇ
			// **********�e�L�X�g���b�Z�[�W:type0�Ƃ��đ��M*********
			
			// ���b�Z�[�W�^�C�v0,����A�h���X,���M���A�h���X,���b�Z�[�WID��擪�ɕt��
			byte[] buffer = addMessageTypeString(text.getBytes(),
					destination_address_b, source_address_b);
			
			// ���Mtry
			try {
				// ���z�b�v�����[�g�e�[�u������Q��
				InetAddress next_hop_Inet = null;
				try {
					next_hop_Inet = InetAddress
							.getByAddress(destination_next_hop_address_b);
				} catch (UnknownHostException e1) {
					e1.printStackTrace();
				}

				// ���M����
				InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
						next_hop_Inet.getHostAddress(), destination_port);
				
				// ���M�p�P�b�g�̐���
				DatagramPacket packet_to_be_sent = new DatagramPacket(
						buffer, buffer.length,
						destination_inet_socket_address);
				// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
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
	
	// �t�@�C����,���悪�������o�߂�Ԃ�
	// ���݂��Ȃ��ꍇ��null��Ԃ�
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
	
	// �������Ԃ������ł���index��Ԃ�
	// ���݂��Ȃ��ꍇ��-1
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
	
	// ���g��IP�A�h���X���擾
	public static String getIPAddress() throws IOException{
	    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        
	    while(interfaces.hasMoreElements()){
	        NetworkInterface network = interfaces.nextElement();
	        Enumeration<InetAddress> addresses = network.getInetAddresses();
	            
	        while(addresses.hasMoreElements()){
	            String address = addresses.nextElement().getHostAddress();
	                
	            //127.0.0.1��0.0.0.0�ȊO�̃A�h���X�����������炻���Ԃ�
	            if(!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address)){
	                return address;
	            }
	        }
	    }
	        
	    return "127.0.0.1";
	}
	
	// IP�A�h���X(byte�z��)���當����(��:"127.0.0.1")�֕ϊ�
	public static String getStringByByteAddress(byte[] ip_address){
		
		if(ip_address.length != 4){
			return "Erorr_RouteIpAddress_is_not_correct";
		}
		
		// byte�𕄍����������ɕϊ�
		// ���Ȃ�+256
		int[] unsigned_b = new int[4];
		for(int i=0;i<4;i++){
			if(ip_address[i] >= 0){
				// 0�ȏ�Ȃ炻�̂܂�
				unsigned_b[i] = ip_address[i];
			}
			else{
				unsigned_b[i] = ip_address[i]+256;
			}
		}
		return unsigned_b[0]+"."+unsigned_b[1]+"."+unsigned_b[2]+"."+unsigned_b[3];
	}
	
	
	// int�^��byte[]�^�֕ϊ�
	private byte[] intToByte(int num){
		
		// �o�C�g�z��ւ̏o�͂��s���X�g���[��
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// �o�C�g�z��ւ̏o�͂��s���X�g���[����DataOutputStream�ƘA������
		DataOutputStream out = new DataOutputStream(bout);
			
		try{	// ���l���o��
			out.writeInt(num);
		}catch(Exception e){
				System.out.println(e);
		}
		
		// �o�C�g�z����o�C�g�X�g���[��������o��
		byte[] bytes = bout.toByteArray();
		return bytes;
	}
}