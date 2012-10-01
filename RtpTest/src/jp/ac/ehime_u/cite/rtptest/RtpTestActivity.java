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
	
	byte[] buffer = new byte[1003];
	byte a=1,b=1;
	final int BYTE_PER_SEC = 8000;	// a,b�̏���p �c�Ȃ��BYTE_PER_SEC�Ȃ�Ė��O?
	Timer m_Timer;
	FileInputStream fi = null;
	BufferedInputStream in = null;
	Context context = this;
	
	final String FILE_NAME = "test_back.mp4";

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
		
		video = (VideoView) findViewById(R.id.videoView1);
		video.setVisibility(View.VISIBLE);
		video.setVideoPath("/data/data/" + context.getPackageName() + "/files/test_front.mp4");

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
				streaming(destination_address, destination_port);
			}
		});
		
		buttonClear.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "clear_clicked");
				// �\�����O�̍폜
				text_view_received.setText("");
			}
		});
		
		buttonMedia.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.d(getClass().getName(), "media_clicked");
				// ���f�B�A�\��,�{�^���B��
				video.setVisibility(View.VISIBLE);
				
				//video.setVideoPath("/data/data/" + context.getPackageName() + "/files/" +FILE_NAME);
				video.start();
			}
		});
		
		// ����^�b�v���Ɍ��ɖ߂�
		video.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				video.setVisibility(View.INVISIBLE);
				return false;
			}
		});
		
		// ����̎�����~
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
		
        //�r�f�I�̍Đ�������������OnCompletion�C�x���g������
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        	@Override
        	public void onCompletion(MediaPlayer mp) {
        		Log.d("rtptest","onCompletion");
        		mp.stop();
        		mp.release();
        		mp = null;
        	}
        });
		
		// �t�@�C���I�[�v���A���l��ID����擾���N���b�N�C�x���g��ǉ�
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
		//�����ŉ�ʉ�]���Œ肷�ׂ��i��ʂ��Œ肳��Ă��Ȃ��Ȃ�j
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
	
//	// ���O�̕\���pEditText�̃T�C�Y����ʃT�C�Y�ɍ��킹�ē��I�Ɍ���
//	// OnCreate()�ł͂܂�View�����C�A�E�g������������Ă��Ȃ����߁H
//	// View�T�C�Y�Ȃǂ̎擾���s��
//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {  
//		super.onWindowFocusChanged(hasFocus);
//		
//		// received��Y���W���擾 * �^�C�g���o�[,�X�e�[�^�X�o�[�̉���0���� *
//		final EditText text_view_received = (EditText) findViewById(R.id.textViewReceived);
//		int received_top = text_view_received.getTop();
//		
//		// Clear�̃T�C�Y���擾
//		final Button clear_button = (Button) findViewById(R.id.buttonClear);
//		int clear_height = clear_button.getHeight();
//		
//		// ��ʃT�C�Y���擾 *�^�C�g���o�[,�X�e�[�^�X�o�[�܂�*
//		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE); 
//		Display display = wm.getDefaultDisplay(); 
//		int display_height = display.getHeight();
//		
//		// �^�C�g��+�X�e�[�^�X�o�[�̃T�C�Y��50�Ɖ���A�s���S�ȓ��I����
//		text_view_received.setHeight(display_height-received_top-clear_height-50);
//	} 
	
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
		
		// ���M����
		final InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
				destination_address, destination_port);
		
		// �t�@�C���I�[�v��
		try {
			fi = openFileInput(FILE_NAME);
		} catch (FileNotFoundException e1) {
			// TODO �����������ꂽ catch �u���b�N
			e1.printStackTrace();
		}
		in = new BufferedInputStream(fi);
		
		m_Timer = new Timer();
		long delay  = 0;	// delay �~���b��ɊJ�n
		long period = 50;	// period�~���b���Ɏ��s
		
		Handler m_handler = new Handler();
		
		a=1;
		b=1;
		
		// �ċN����
		m_Timer.scheduleAtFixedRate(new TimerTask(){
			
			@Override
			public void run() {
				
				if(1000*b > BYTE_PER_SEC){
					a++;
					b=1;
				}
				buffer[0] = a;
				buffer[1] = b;
				
				// ���Mtry
				try {
					if(in.read(buffer, 2, 1000) == -1){
						// ���M�I��
						in.close();
						fi.close();
						m_Timer.cancel();
						m_Timer = null;
					}
					else{
						// ���M�p�P�b�g�̐���
						DatagramPacket packet_to_be_sent = new DatagramPacket(
								buffer, buffer.length,
								destination_inet_socket_address);
						
						// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
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