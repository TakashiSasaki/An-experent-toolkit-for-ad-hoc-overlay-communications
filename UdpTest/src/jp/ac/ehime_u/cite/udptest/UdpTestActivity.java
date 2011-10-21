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

	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDestPort;
	private EditText editTextToBeSent;
	private Thread udpListenerThread;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextDestPort = (EditText) findViewById(R.id.editTextDestPort);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextToBeSent = (EditText) findViewById(R.id.editTextToBeSent);

		TextView text_view_received = (TextView) findViewById(R.id.textViewReceived);
		try {
			UdpListener udp_listener = new UdpListener(new Handler(),
					text_view_received, 12345, 100);
			udpListenerThread = new Thread(udp_listener);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		udpListenerThread.start();

		Button buttonSend = (Button) findViewById(R.id.buttonSend);
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String destination_address = editTextDest.getText().toString();
				int destination_port = Integer.parseInt(editTextDestPort
						.getText().toString());
				InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
						destination_address, destination_port);

				String string_to_be_sent = editTextToBeSent.getText()
						.toString() + "\n\r";
				byte[] buffer = string_to_be_sent.getBytes();

				try {
					Log.d("UdpTest", "sending " + string_to_be_sent);
					DatagramPacket packet_to_be_sent = new DatagramPacket(
							buffer, buffer.length,
							destination_inet_socket_address);
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