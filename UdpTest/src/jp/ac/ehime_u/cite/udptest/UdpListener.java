package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class UdpListener implements Runnable {

	public ArrayList<byte[]> receivedDataList = new ArrayList<byte[]>();

	private int port;
	private int maxPackets;
	Handler handler;
	TextView textView;
	private byte[] buffer = new byte[2000];
	private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private DatagramSocket socket;

	public UdpListener(Handler handler_, TextView text_view, int port_,
			int max_packets) throws SocketException {
		port = port_;
		socket = new DatagramSocket(port);
		maxPackets = max_packets;
		handler = handler_;
		textView = text_view;
	}

	@Override
	public void run() {
		while (true) {
			try {
				socket.receive(packet); // blocking
				byte[] received_data = packet.getData();
				if (receivedDataList.size() < maxPackets) {
					receivedDataList.add(received_data);
					PrintReceivedDataList();
				} else {
					throw new OutOfMemoryError();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void PrintReceivedDataList() {
		StringBuilder string_builder = new StringBuilder();
		for (Iterator<byte[]> i = receivedDataList.iterator(); i.hasNext();) {
			string_builder.append( new String(i.next()));
		}
		final String s = string_builder.toString(); 
		handler.post(new Runnable() {
			@Override
			public void run() {
				Log.d("UdpTest", s);
				textView.setText(s.toString());				
			}
		});
	}
}