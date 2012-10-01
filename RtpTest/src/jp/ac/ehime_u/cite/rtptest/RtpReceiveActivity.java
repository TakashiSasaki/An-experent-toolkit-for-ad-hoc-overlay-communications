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

	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	// ��M�X���b�h
	private Thread rtpListenerThread;
	
	protected static MediaPlayer mp = null;
	protected static SurfaceHolder s_holder = null;
	//final String DATA_PATH ;//= "/data/data/" + this.getPackageName() + "/files/test.mp4";
	
	Context context = this;

	/** Called when the activity is first created. */
	@Override	// �I�[�o�[���C�h(�e��q�N���X�Ń��\�b�h�����������Ƃ��q�N���X�̐錾�ŏ㏑��)

	// �{�^���Ȃǂ��\�������O�̏����������Ȃ�
	// onCreate���\�b�h���I�[�o�[���C�h�Ƃ��ċL�q���Ă���
	public void onCreate(Bundle savedInstanceState) {
		// onCreate���I�[�o�[���C�h����ꍇ�A�X�[�p�[�N���X�̃��\�b�h���Ăяo���K�v������
		super.onCreate(savedInstanceState);
		
//		// �N�����Ƀ\�t�g�L�[�{�[�h�̗����オ���h��
//		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		// �A�N�e�B�r�e�B�Ƀr���[�O���[�v��ǉ�����
		setContentView(R.layout.receive);

//		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
//		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
//		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
//
		try {
			//editTextSrc.setText(getIPAddress());
			setTitle(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
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
		setContentView(new SurfaceTestView(this));
		
		// MediaPlayer �Ǘ�
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
		
        //�r�f�I�̍Đ�������������OnCompletion�C�x���g������
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
			// TODO �����������ꂽ���\�b�h�E�X�^�u
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			s_holder = holder;
			try {
				mp.setDataSource("/data/data/" + context.getPackageName() + "/files/test.mp3");
			} catch (IllegalArgumentException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			} catch (IOException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}
			mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mp.setDisplay(RtpReceiveActivity.s_holder);
			try {
				mp.prepare();
			} catch (IllegalStateException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			} catch (IOException e) {
				// TODO �����������ꂽ catch �u���b�N
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
	
}