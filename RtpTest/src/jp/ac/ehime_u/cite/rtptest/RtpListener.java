package jp.ac.ehime_u.cite.rtptest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class RtpListener implements Runnable {

	// byte配列のList、受信情報を記録
	public ArrayList<byte[]> receivedDataList = new ArrayList<byte[]>();

	private int port;
	private int maxPackets;
	Handler handler;
	EditText editText;
	ScrollView scrollView;
	Context context;
	
    int current_pos;
    int total_len;
    long start,end;
    int flag;
    
    final String FILE_NAME = "test_front.mp4";
    
	// 受信用の配列やパケットなど
    private byte[] buffering = new byte[8100];
	private byte[] buffer = new byte[6002]; //1002
	private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private DatagramSocket socket;
	
	// コンストラクタ
	// 引数1:Handler	メインスレッドのハンドル(Handlerを使うことでUIスレッドの持つキューにジョブ登録ができる)
	// 引数2:TextView	受信結果を表示するTextView
	// 引数3:port_		ポート番号(受信)
	// 引数4:max_packets 記録する最大パケット数(受信可能回数)
	public RtpListener(Handler handler_, EditText edit_text,
			int port_, int max_packets,Context context_) throws SocketException {
		port = port_;
		socket = new DatagramSocket(port);
		maxPackets = max_packets;
		handler = handler_;
		editText = edit_text;
		//scrollView = scroll_view;
		context = context_;
	}

	@Override
	public void run() {
		
		
		// 出力ファイルオープン
	    FileOutputStream fo = null;
	    BufferedOutputStream out = null;
	    
	    int a=1,b=1;	// シーケンス番号
	    int sum=0;
	    int loss=0;
	    boolean start_flag = false;
	    
	    try {
	    	fo = context.openFileOutput(FILE_NAME, context.MODE_PRIVATE | context.MODE_APPEND);
	    	out = new BufferedOutputStream(fo);
	    	// writer.append("write test");
	    	// writer.close();
	    } catch (IOException e) {
	    	e.printStackTrace();    
	    }
	    
	    // 読み込みファイルオープン
	    FileInputStream fi = null;
	    BufferedInputStream in = null;
	    
	    try {
			fi = context.openFileInput("test_back.mp4");
			in = new BufferedInputStream(fi);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		handler.post(new Runnable() {
			@Override
			public void run() {
				RtpTestActivity.video.setVisibility(View.VISIBLE);
				
				//RtpTestActivity.video.setVideoPath("/data/data/" + context.getPackageName() + "/files/" +FILE_NAME);
				//	RtpTestActivity.mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
				//	RtpTestActivity.mp.setDisplay(RtpTestActivity.s_holder);
				//	RtpTestActivity.video.prepare();
				//	RtpTestActivity.video.start();
				
				RtpTestActivity.video.start();
			}
		});
	    
		while (true) {
			Log.d("war","わーわー");
			// 受信側で完結実験
			if(flag==0){
				try {
					if( in.read(buffer, 0, 5000) != -1){
						out.write(buffer, 0, 5000);
					}
					else{
						in.close();
						out.close();
						flag = 1;
					}
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					//if(mp.isPlaying())mp.stop();
					e.printStackTrace();
				}
			}
			
			//total_len = mp.getDuration();
			//current_pos = mp.getCurrentPosition();
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
//			
//			handler.post(new Runnable() {
//				@Override
//				public void run() {
//					editText.setText("状態:"+current_pos+"/"+total_len);
//					
//				}
//			});
			/*
			try {
				socket.receive(packet); // blocking
				
				Log.d(getClass().getName(), "received a:"+a+",b:"+b);
				
				// 受信したデータを抽出
				byte[] received_data = packet.getData();//cut_byte_spare(packet.getData() ,packet.getLength());
				Log.d(getClass().getName(), "received_data[0]="+received_data[0]+",[1]="+received_data[1]);
				
				// 同一パケットの破棄
				if(received_data[0] == a){
					if(received_data[1] < b){
						continue;
					}
				}
				if(received_data[0] < a){
					continue;
				}
				
				// 受信処理
				while(received_data[0] > a){	// aが更新され次フレームに移ったなら
					Log.d(getClass().getName(), "received1");
					// 溜まった分を書き込み
					// bが足りてなければ0補完
					if(b<=8){
						Arrays.fill(buffering ,(b-1)*1000 ,8*1000 ,(byte)0);
						sum += 8-(b-1);		// 送信パケット総数を加算
						loss+= 8-(b-1);		// 受信パケット損失数を加算
					}
					out.write(buffering,0,8*1000);
					a++;
					b=1;
					sum++;	// 送信パケット数++
					
					// VideoViewが止まっているなら再開
					if(start_flag == false){
						handler.post(new Runnable() {
							@Override
							public void run() {
								//RtpTestActivity.video.setVisibility(View.VISIBLE);
								
								//RtpTestActivity.video.setVideoPath("/data/data/" + context.getPackageName() + "/files/" +FILE_NAME);
								//	RtpTestActivity.mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
								//	RtpTestActivity.mp.setDisplay(RtpTestActivity.s_holder);
								//	RtpTestActivity.video.prepare();
								//	RtpTestActivity.video.start();
								
								RtpTestActivity.video.start();
							}
						});
						start_flag = true;
					}else{
						handler.post(new Runnable() {
							@Override
							public void run() {
								if(!RtpTestActivity.video.isPlaying()){
									RtpTestActivity.video.start();
								}
							}
						});
					};
				}
				if(received_data[0] == a){ // aが更新されてなければ
					Log.d(getClass().getName(), "received2");
					if(received_data[1] > b){ // 抜けがあるなら
						// 抜けた分を補完
						Arrays.fill(buffering ,(b-1)*1000 ,(received_data[1]-b)*1000 ,(byte)0);
						sum += 8-(b-1);		// 送信パケット総数を加算
						loss+= 8-(b-1);		// 受信パケット損失数を加算
						b = received_data[1];
					}
					// 受信部分をコピー
					System.arraycopy(received_data, 2, buffering, (b-1)*1000, packet.getLength()-2);
					b++;
					sum++;	// 送信パケット数++
				}
				
				if(packet.getLength() != 1002){
					Log.d(getClass().getName(), "received3");
					// 最終パケット？
					out.write(buffering,0,(b-1)*1000+packet.getLength()-2);
					
    				out.flush();
    				
    				out.close();
    				fo.close();
				}
				
				Log.d(getClass().getName(), "受信数: " + (sum-loss) +"/" + sum +"   (...損失:"+loss+")");
				Log.d(getClass().getName(), "受信成功率,a,b: " + (sum-loss)*100/sum + "%,"+ a +"," + b);
				
//				handler.post(new Runnable() {
//					@Override
//					public void run() {
//						editText.setText("受信数: " + (sum-loss) +"/" + sum +"   (...損失:"+loss+")\n" +
//								"受信成功率,a,b: " + (sum-loss)*100/sum + "%,"+ a +"," + b + "\n");
//					}
//				});
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
	}
	
	// バイト配列の取り出し（余分な部分の削除）
	byte[] cut_byte_spare(byte[] b,int size){
		
		byte[] slim_byte = new byte[size];
		System.arraycopy(b, 0, slim_byte, 0, size);
		
		return slim_byte;
	}
}