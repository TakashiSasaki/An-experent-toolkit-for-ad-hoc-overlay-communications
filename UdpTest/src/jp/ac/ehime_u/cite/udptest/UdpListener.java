package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;

import android.os.Handler;
import android.widget.TextView;

public class UdpListener implements Runnable {

	// byte配列のList、受信情報を記録
	public ArrayList<byte[]> receivedDataList = new ArrayList<byte[]>();

	private int port;
	private int maxPackets;
	Handler handler;
	TextView textView;

	// 受信用の配列やパケットなど
	private byte[] buffer = new byte[2000];
	private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private DatagramSocket socket;

	// コンストラクタ
	// 引数1:Handler	メインスレッドのハンドル(Handlerを使うことでUIスレッドの持つキューにジョブ登録ができる)
	// 引数2:TextView	受信結果を表示するTextView
	// 引数3:port_		ポート番号(受信)
	// 引数4:max_packets 記録する最大パケット数(受信可能回数)
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
				// 受信したデータを抽出
				byte[] received_data = packet.getData();
				// 受信パケット数が上限に達していないなら、ログに追加+表示
				if (receivedDataList.size() < maxPackets) {
					receivedDataList.add(received_data);
					PrintReceivedDataList();
				} else {
					// 上限に達しているなら、エラー
					throw new OutOfMemoryError();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// ログの表示
	private void PrintReceivedDataList() {
		// 可変長String(文字列の連結を頻繁に行う場合に高速)
		StringBuilder string_builder = new StringBuilder();
		// 受信ログの文字列を連結
		for (Iterator<byte[]> i = receivedDataList.iterator(); i.hasNext();) {
			string_builder.append(new String(i.next()));
		}
		final String s = string_builder.toString();
		// Handlerにジョブをpost
		// Runnableインタフェースを使ってスレッドを作成、開始
		// Threadをサブクラス化しなくても実行できる
		handler.post(new Runnable() {
			@Override
			public void run() {
				System.out.print(s);
				textView.setText(s.toString());
			}
		});
	}
}