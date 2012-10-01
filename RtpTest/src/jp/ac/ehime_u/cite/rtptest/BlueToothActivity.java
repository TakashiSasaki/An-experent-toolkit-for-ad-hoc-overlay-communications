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

	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	
	private Button buttonSend;
	private Button buttonMedia;
	private Button buttonClear;
	
	static VideoView video;
	
	// ��M�X���b�h���쐬���Ȃ�
	private Thread rtpListenerThread;
	
	private BluetoothAdapter Bt;
	
	Context context = this;

	/** Called when the activity is first created. */
	@Override	// �I�[�o�[���C�h(�e��q�N���X�Ń��\�b�h�����������Ƃ��q�N���X�̐錾�ŏ㏑��)

	// �{�^���Ȃǂ��\�������O�̏����������Ȃ�
	// onCreate���\�b�h���I�[�o�[���C�h�Ƃ��ċL�q���Ă���
	public void onCreate(Bundle savedInstanceState) {
		// onCreate���I�[�o�[���C�h����ꍇ�A�X�[�p�[�N���X�̃��\�b�h���Ăяo���K�v������
		super.onCreate(savedInstanceState);
		
		// �N�����Ƀ\�t�g�L�[�{�[�h�̗����オ���h��
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// �A�N�e�B�r�e�B�Ƀr���[�O���[�v��ǉ�����
		setContentView(R.layout.send);

		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
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
		
		// ��M���O�p��TextView�A���l��ID����擾
		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
		
		try {
			// ��M�X���b�h�̃C���X�^���X���쐬
			RtpListener rtp_listener = new RtpListener(new Handler(),
					text_view_received, 12345, 100, this);
			// �X���b�h���擾
			rtpListenerThread = new Thread(rtp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		// ��M�X���b�hrun()
		rtpListenerThread.start();
		
		// BlueTooth�T�[�o�[�X���b�h���쐬
		Thread serverThread = null;
		BlueToothServerThread server = new BlueToothServerThread(context, "myNum", BluetoothAdapter.getDefaultAdapter()
																, new Handler(), text_view_received);
		// �X���b�h���擾
		serverThread = new Thread(server);
		serverThread.start();
		
		// �N���b�N�����A�����N���X(���̏����̖��O�̖����N���X)�𗘗p
		// �{�^�����ɁA�r���[���ӎ������ɏ������L�q�ł���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				Log.d(getClass().getName(), "send_clicked");
				
				// editText���瑗�M��IP(String)�Aport(int)�̎擾
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				
				// ���M����
			}
		});
		
		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "clear_clicked");
				// �\�����O�̍폜
				text_view_received.setText("");
			}
		});
		
		// BlueTooth
		Bt = BluetoothAdapter.getDefaultAdapter();
		if(Bt.equals(null)){
			text_view_received.setText("BlueTooth�ɑΉ����Ă��܂���");
		}else{
			text_view_received.setText("BlueTooth�ɑΉ����Ă��܂�");
			
			boolean btEnable = Bt.isEnabled();
			if(btEnable==false){	// BlueTooth�̋�������
				Intent btOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(btOn, 1);
			}
		}
		
		buttonMedia.setText("BlueTooth");
		buttonMedia.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "bluetooth_clicked");
				
				if(Bt.isEnabled()){
					// �ڑ��\�f�o�C�X�ꗗ�\��
			        Intent intent = new Intent(context, BlueToothList.class);
			        int requestCode = 123;
			        startActivityForResult(intent, requestCode);
				}
			}
		});
	}
	
	// ActivityIntent�̌��ʂ��󂯎��
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (requestCode) {
			case 123:
				if (resultCode == RESULT_OK) {
					// �ڑ��̊J�n(Client)
					BluetoothDevice device = data.getParcelableExtra("DEVICE");
					BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
					EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
					Handler handler = new Handler();
					
		            if(btAdapter.isDiscovering()){
		            	//�������̏ꍇ�͌��o���L�����Z������
		            	btAdapter.cancelDiscovery();
		            }
					
					Thread clientThread = null;
					BlueToothClientThread client = new BlueToothClientThread(context, "Mynum", device, btAdapter, handler, text_view_received);
					// �X���b�h���擾
					clientThread = new Thread(client);
					clientThread.start();
					
					
				}else if (resultCode == RESULT_CANCELED) {
					
				}
			break;
			default:
			break;
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
	
	// ���M���[�v
	protected void streaming(String destination_address,int destination_port){
		
		byte buffer[] = null;
		// ���M����
		final InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				destination_address, destination_port);
		
		// ���M�p�P�b�g�̐���
		DatagramPacket packet_to_be_sent = null;
		try {
			packet_to_be_sent = new DatagramPacket(
					buffer, buffer.length,
					destination_inet_socket_address);
			
			// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
			DatagramSocket datagram_socket;
			datagram_socket = new DatagramSocket();
			datagram_socket.send(packet_to_be_sent);
			datagram_socket.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
