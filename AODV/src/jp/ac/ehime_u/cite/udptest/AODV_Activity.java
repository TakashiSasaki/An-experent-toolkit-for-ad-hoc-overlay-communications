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

	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;

	// �X���b�h
	private Thread udpListenerThread; // ��M�X���b�h
	private Thread routeManagerThread; // ���[�g�Ď��X���b�h

	// ���[�g�e�[�u��
	public static ArrayList<RouteTable> routeTable = new ArrayList<RouteTable>();

	// PATH_DISCOVERY_TIME�̊ԂɎ�M����RREQ�̑��M����ID���L�^
	public static ArrayList<PastData> receiveRREQ_List = new ArrayList<PastData>();
	
	// �Ƃ肠����PATH_DISCOVERY_TIME �̊ԂɎ�M�������b�Z�[�W0�̑��M����ID���L�^
	public static ArrayList<PastData> receiveMessageList = new ArrayList<PastData>();

	// �}���`�X���b�h�̔r������p�I�u�W�F�N�g
	public static Object routeLock = new Object();
	public static Object pastDataLock = new Object();
	
	
	// ���̑��ϐ�
	public static int RREQ_ID = 0;
	public static int seqNum = 0;
	public static boolean do_BroadCast = false; // ��莞�ԓ��ɉ�����۰�޷��Ă������ǂ���
	public static int message_ID = 0;

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
	public static final int TIMEOUT_BUFFER = 2;
	public static final int TTL_START = 1;
	public static final int TTL_INCREMENT = 2;
	public static final int TTL_THRESHOLD = 7;
	public static int TTL_VALUE = 1; // IP�w�b�_����"TTL"�t�B�[���h�̒l getTimeToLive()�H
	public static int RING_TRAVERSAL_TIME = 2 * NODE_TRAVERSAL_TIME
			* (TTL_VALUE + TIMEOUT_BUFFER);

	/** Called when the activity is first created. */
	@Override
	// �I�[�o�[���C�h(�e��q�N���X�Ń��\�b�h�����������Ƃ��q�N���X�̐錾�ŏ㏑��)
	// �{�^���Ȃǂ��\�������O�̏����������Ȃ�
	// onCreate���\�b�h���I�[�o�[���C�h�Ƃ��ċL�q���Ă���
	public void onCreate(Bundle savedInstanceState) {
		// onCreate���I�[�o�[���C�h����ꍇ�A�X�[�p�[�N���X�̃��\�b�h���Ăяo���K�v������
		super.onCreate(savedInstanceState);

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
			editTextDest.setText("133.71.232.13");
		
		// ��M���O�p��TextView�A���l��ID����擾
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);

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

		// ���MButton�A���l��ID����擾
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// �N���b�N�����A�����N���X(���̏����̖��O�̖����N���X)�𗘗p
		// �{�^�����ɁA�r���[���ӎ������ɏ������L�q�ł���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// editText���瑗�M��IP(String)�A���M��(String)�Aport(int)�̎擾
				String destination_address = editTextDest.getText().toString();
				String source_address = editTextSrc.getText().toString();
				final int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());

				// ���M��,���M��IP�A�h���X��byte[]��
				final byte[] destination_address_b = new RREQ()
						.getByteAddress(destination_address);
				
				final byte[] source_address_b = new RREQ()
				.getByteAddress(source_address);

				// ���M��ւ̌o�H�����݂��邩�`�F�b�N
				final int index = searchToAdd(routeTable, destination_address_b);

				// �o�H�����݂���ꍇ�A�L�����ǂ����`�F�b�N
				boolean enableRoute = false; // ������

				if (index != -1) {
					if (getRoute(index).stateFlag == 1
							&& (getRoute(index).lifeTime > new Date().getTime())) {
						enableRoute = true;
					}
				}

				// ********* �o�H�����ɑ��݂���ꍇ *******
				if (enableRoute) {
					// ���b�Z�[�W�̑��M
					sendMessage(getRoute(index).nextIpAdd, destination_port
							, destination_address_b, source_address_b);
					
					// ���M�������Ƃ�\��
					text_view_received.append(editTextToBeSent.getText().toString()
							+ "-->" + destination_address+"\n");
				}

				// *********** �o�H�����݂��Ȃ��ꍇ ***********
				else {
					text_view_received.append("Try Connect...\n");

					// ���g�̃V�[�P���X�ԍ����C���N�������g
					seqNum++;

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
					// �^�C�}�[�̏������A�C���X�^���X�̐���
					final Timer mTimer;
					mTimer = new Timer(true);
					final Handler mHandler = new Handler();
					
					mTimer.scheduleAtFixedRate( new TimerTask(){
						@Override
						public void run(){
							
							// �`�揈��������̂�handler�ɓ�����
							mHandler.post(new Runnable(){
								public void run(){
									
									// �ȉ��A��������̓��e						
									// �o�H�����������ꍇ�A���[�v�𔲂���
									if (searchToAdd(routeTable, destination_address_b) != -1) {
										text_view_received.append("Route_Create_Success!!\n");
										mTimer.cancel();
										
										// ���b�Z�[�W�̑��M
										sendMessage(getRoute(index).nextIpAdd, destination_port
												, destination_address_b, source_address_b);
										
										text_view_received.append(editTextToBeSent.getText().toString()
												+ "-->" + editTextDest.getText().toString() +"\n");
									}
									
									// TTL������l�Ȃ炻�̑��M���s������A���[�v�𔲂���
									if (TTL_VALUE == TTL_THRESHOLD){
										text_view_received.append("Failed\n");
										mTimer.cancel();
									}

									// TTL�̔�����
									// �Ⴆ��INCREMENT2,THRESHOLD7�̂Ƃ�,TTL�̕ω���2->4->6->7(not 8)
									if (TTL_VALUE > TTL_THRESHOLD) {
										TTL_VALUE = TTL_THRESHOLD;
									}

									// RREQ_ID���C���N�������g
									RREQ_ID++;

									// ���������M�����p�P�b�g����M���Ȃ��悤��ID��o�^
									byte[] myAdd = source_address_b;
									
									newPastRReq(RREQ_ID, myAdd);

									// RREQ�̑��M
									do_BroadCast = true;

									try {
										new RREQ().send(destination_address_b, myAdd,
												false, false, false, false, flagU,
												seqValue, seqNum, RREQ_ID, TTL_VALUE,
												destination_port);
									} catch (Exception e) {
										e.printStackTrace();
									}

									// ������Ƌ����ȑҋ@(�{����RREP���߂��Ă���Α҂��Ȃ��Ă������Ԃ��҂��Ă���)
									// �҂����Ԃ�VALUE�ɍ��킹�čX�V
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
	
	// ���O�̕\���pEditText�̃T�C�Y����ʃT�C�Y�ɍ��킹�ē��I�Ɍ���
	// OnCreate()�ł͂܂�View�����C�A�E�g������������Ă��Ȃ����߁H
	// View�T�C�Y�Ȃǂ̎擾���s��
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		// received��Y���W���擾 * �^�C�g���o�[,�X�e�[�^�X�o�[�̉���0���� *
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		int received_top = text_view_received.getTop();

		// Clear�̃T�C�Y���擾
		final Button clear_button = (Button) findViewById(R.id.buttonClear);
		int clear_height = clear_button.getHeight();

		// ��ʃT�C�Y���擾 *�^�C�g���o�[,�X�e�[�^�X�o�[�܂�*
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int display_height = display.getHeight();

		// �^�C�g��+�X�e�[�^�X�o�[�̃T�C�Y��50�Ɖ���A�s���S�ȓ��I����
		text_view_received.setHeight(display_height - received_top
				- clear_height - 50);
	}
	
	

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
	}

	// ���[�g�e�[�u���̗v�f���폜����A�r������
	public static void removeRoute(int index) {
		synchronized (routeLock) {
			routeTable.remove(index);
		}
	}

	// ���[�g�e�[�u���̗v�f���㏑������A�r������
	public static void setRoute(int index, RouteTable route) {
		synchronized (routeLock) {
			routeTable.set(index, route);
		}
	}

	// RouteTable(list)�Ɉ���A�h���X(Add)���܂܂�Ă��Ȃ�����������
	// �߂�l�F���X�g���Ŕ��������ʒu�A�C���f�b�N�X
	// ������Ȃ��ꍇ -1��Ԃ�
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
	// [5-9]	:���M���A�h���X
	// [10-?]	:�f�[�^
	private byte[] addMessageType(byte[] message,byte[] toIPAddress,byte[] my_address) {
		byte[] new_message = new byte[message.length + 1 + 4 + 4];

		new_message[0] = 0; // ���b�Z�[�W�^�C�v0
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 1+4, 4);
		//System.arraycopy(new RREQ().intToByte(message_ID), 0, new_message, 1+4+4, 4);	// int��byte[]��
		System.arraycopy(message, 0, new_message, 1+4+4+4, message.length);

		return new_message;
	}
	private void sendMessage(byte[] destination_next_hop_address_b, int destination_port
			, byte[] destination_address_b, byte[] source_address_b){
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
				next_hop_Inet, destination_port);

		// ���M���镶����̎擾
		String string_to_be_sent = editTextToBeSent.getText()
				.toString() + "\r\n";

		// ### �f�o�b�O�p ###
		// Date date1 = new Date(new Date().getTime());
		// string_to_be_sent = date1.toString()+"\n\r";

		// Byte��
		byte[] buffer = string_to_be_sent.getBytes();
		
		// ���b�Z�[�WID���C���N�������g
		message_ID++;

		// ���b�Z�[�W�^�C�v0,����A�h���X,���M���A�h���X,���b�Z�[�WID��擪�ɕt��
		buffer = addMessageType(buffer, destination_address_b, source_address_b);

		// ���Mtry
		try {
			// LogCat�ɑ��M�����o��
			Log.d("AODV", "sending " + string_to_be_sent);
			// ���M�p�P�b�g�̐���
			DatagramPacket packet_to_be_sent = new DatagramPacket(
					buffer, buffer.length,
					destination_inet_socket_address);
			// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
			DatagramSocket datagram_socket = new DatagramSocket();
			datagram_socket.send(packet_to_be_sent);
			datagram_socket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// ���M��IP�����[�J���t�@�C���ɕۑ�
		// save_ip_to_local(destination_address);
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
}