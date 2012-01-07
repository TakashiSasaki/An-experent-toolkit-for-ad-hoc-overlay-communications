package jp.ac.ehime_u.cite.udptest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;

// 分割送信中または送信直後のファイル名、経過などを保持
public class FileManager {
	byte[] buffer = null;
	String file_name = null;
	BufferedInputStream file_in;
	FileInputStream file;
	int total_step;
	int file_length;
	int file_name_b_length;
	int file_next_no;
	byte[] destination_address;
	long life_time;
	
	
	
	// ファイルオープン
	public FileManager(String name,byte[] dest_add,byte[] source_add,Context context) throws FileNotFoundException{
		file_name = name;
		file = context.openFileInput(file_name);	// ファイルオープン
		file_in = new BufferedInputStream(file);
		
		try {
			file_length = file_in.available();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ファイルサイズ
		total_step = file_length/ AODV_Activity.MAX_SEND_FILE_SIZE +1;	// ファイル分割数
		file_name_b_length = file_name.getBytes().length;	// ファイル名(byte)の長さ
		
		buffer = addMessageTypeFile(AODV_Activity.MAX_SEND_FILE_SIZE, dest_add, source_add,
				file_name,total_step);
		
		file_next_no = 1;
		destination_address = dest_add;
		
		life_time = new Date().getTime() + AODV_Activity.ACTIVE_ROUTE_TIMEOUT * 2;
	}

	// 分割送信(ファイルオープン,クローズ除く)
	public void fileSend(byte[] source_address, byte[] next_hop_address_b, int port){
		
		// ファイルを分割読み込み->送信
		
		if (total_step == file_next_no) { // 最終パケットならbuffer長を調整
			buffer = addMessageTypeFile(file_length % AODV_Activity.MAX_SEND_FILE_SIZE,
					destination_address, source_address, file_name,
					total_step);
		}
		
		// 更新する必要がある2か所を処理
		System.arraycopy(intToByte(file_next_no), 0, buffer, 9, 4);	// 1か所目,パケット番号
		
		// 2か所目,パケットデータ
		try {
			file_in.read(buffer, 21 + file_name_b_length,
					// 最終パケットなら残りサイズ、そうでないなら限界サイズ読み込み
					(total_step == file_next_no) ? file_length % AODV_Activity.MAX_SEND_FILE_SIZE
							: AODV_Activity.MAX_SEND_FILE_SIZE);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
		file_next_no++;
		
		// 送信try
		try {
			// 送信先(次ホップ)情報
			InetAddress next_hop_inet = InetAddress.getByAddress(next_hop_address_b);
			InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
					next_hop_inet.getHostAddress(), port);
			
			// 送信パケットの生成
			DatagramPacket packet_to_be_sent = new DatagramPacket(buffer,
					buffer.length, destination_inet_socket_address);
			// 送信用のクラスを生成、送信、クローズ
			DatagramSocket datagram_socket = new DatagramSocket();
			datagram_socket.send(packet_to_be_sent);
			datagram_socket.close();
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	

	
	// 排他制御用 ArrayListに追加
	public void add(){
		synchronized(AODV_Activity.fileManagerLock){
			AODV_Activity.file_manager.add(this);
		}
	}
	
	// 排他制御用 ArrayListから削除
	// 宛先・ファイル名が呼び出し元(this)と一致する経過記録を削除
	public void remove(){
		synchronized(AODV_Activity.fileManagerLock){
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( this.file_name.equals(AODV_Activity.file_manager.get(i).file_name) 
						&& Arrays.equals(this.destination_address, 
								AODV_Activity.file_manager.get(i).destination_address)){
					AODV_Activity.file_manager.remove(i);
					break;
				}
			}
		}
	}
	
	// 排他制御用 ArrayListの情報を上書き
	// 宛先・ファイル名が呼び出し元(this)と一致する経過記録に上書き
	public void set(){
		synchronized(AODV_Activity.fileManagerLock){
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( this.file_name.equals(AODV_Activity.file_manager.get(i).file_name) 
						&& Arrays.equals(this.destination_address, 
								AODV_Activity.file_manager.get(i).destination_address)){
					AODV_Activity.file_manager.set(i, this);
					break;
				}
			}
		}
	}
	
	// ファイル送信用タイプ10やその他制御データを付加する
	// 以下はフォーマット、[数字]は配列内の位置を示す
	// [0]		: メッセージタイプ10
	// [1-4]	: 宛先アドレス
	// [5-8]	: 送信元アドレス
	// [9-12]	: 分割した何番目のデータか	* このメソッドでは代入しない *
	// [13-16]	: いくつに分割したか
	// [17-20]	: ファイル名のサイズ
	// [21-??]	: ファイル名(可変長)
	// [??-??]	: ファイルデータ(可変長,最大63K[byte]) * このメソッドでは代入しない *
	private byte[] addMessageTypeFile(int fileSize,byte[] toIPAddress,byte[] my_address,
			String fileName,int step){
		
		byte[] fileName_b = fileName.getBytes();	// ファイル名をbyte化
		byte[] fileName_size_b = intToByte(fileName_b.length);	// byte型ファイル名のサイズをbyte化
		byte[] step_b = intToByte(step);			// 分割数をbyte化
		
		byte[] new_message = new byte[21 + fileName_b.length + fileSize];
		
		new_message[0] = 10;	// メッセージタイプ10
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 5, 4);
		// [9-12] 分割した何番目のデータか を代入しない
		System.arraycopy(step_b, 0, new_message, 13, 4);
		System.arraycopy(fileName_size_b, 0, new_message, 17, 4);
		System.arraycopy(fileName_b, 0, new_message, 21, fileName_b.length);
		// [??-??] ファイルデータを代入しない
		
		return new_message;
	}
	
	// int型をbyte[]型へ変換
	private byte[] intToByte(int num){
		
		// バイト配列への出力を行うストリーム
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// バイト配列への出力を行うストリームをDataOutputStreamと連結する
		DataOutputStream out = new DataOutputStream(bout);
			
		try{	// 数値を出力
			out.writeInt(num);
		}catch(Exception e){
				System.out.println(e);
		}
		
		// バイト配列をバイトストリームから取り出す
		byte[] bytes = bout.toByteArray();
		return bytes;
	}
}
