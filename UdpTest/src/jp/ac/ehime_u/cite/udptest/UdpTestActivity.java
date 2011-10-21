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
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Date;

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

	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	// ��M�X���b�h
	private Thread udpListenerThread;

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
		setContentView(R.layout.main);

		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);
		
		Button buttonClear = (Button) findViewById(R.id.buttonClear);

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
		
		text_view_received.append("tessssssss");

		// ���MButton�A���l��ID����擾
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// �N���b�N�����A�����N���X(���̏����̖��O�̖����N���X)�𗘗p
		// �{�^�����ɁA�r���[���ӎ������ɏ������L�q�ł���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// editText���瑗�M��IP(String)�Aport(int)�̎擾
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				// ���M����
				InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
						destination_address, destination_port);

				// ���M���镶����̎擾
				String string_to_be_sent = editTextToBeSent.getText()
						.toString() + "\r\n";
				// ### �f�o�b�O�p ###
				Date date1 = new Date(new Date().getTime());
				string_to_be_sent = date1.toString()+"\n\r";

				// Byte��
				byte[] buffer = string_to_be_sent.getBytes();

				// ���Mtry
				try {
					// LogCat�ɑ��M�����o��
					Log.d("UdpTest", "sending " + string_to_be_sent);
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
		text_view_received.setHeight(display_height-received_top-clear_height-50);
	} 
	
	// ���M��IP�����[�J���t�@�C���ɕۑ�
	private void save_ip_to_local(String s){
		
		try{
			OutputStream out = openFileOutput("ip.txt",MODE_APPEND|MODE_PRIVATE);
			PrintWriter writer =
				new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
			writer.append(s+"\n");
			writer.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}