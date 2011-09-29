package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
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

		// �A�N�e�B�r�e�B�Ƀr���[�O���[�v��ǉ�����
		setContentView(R.layout.main);

		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);


		// ��M���O�p��TextView�A���l��ID����擾
		TextView text_view_received = (TextView) findViewById(R.id.textViewReceived);
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


		// ���MButton�A���l��ID����擾
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// �N���b�N�����A�����N���X(���̏����̖��O�̖����N���X)�𗘗p
		// �{�^�����ɁA�r���[���ӎ������ɏ������L�q�ł���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// Dest�G���M�� editText����IP(������)�Aport(int)�̎擾
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				// ���M����
				InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
						destination_address, destination_port);

				// ���M���镶����̎擾
				String string_to_be_sent = editTextToBeSent.getText()
						.toString() + "\n\r";
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
			}
		});

	}
}