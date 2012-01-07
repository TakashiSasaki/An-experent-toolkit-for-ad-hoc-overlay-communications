package jp.ac.ehime_u.cite.udptest;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

public class FileReceivedManager {
	
	// メンバ変数
	String file_name;			// ファイル名
	int receive_file_next_no;	// 次に受信すべきNo
	FileOutputStream file;		// 出力用にオープンしたFileクラス
	BufferedOutputStream out;	// 同上
	long life_time;				// 生存時間
	
	// コンストラクター
	public FileReceivedManager(int receive_file_next_no, FileOutputStream file,
			BufferedOutputStream out) {
		super();
		this.receive_file_next_no = receive_file_next_no;
		this.file = file;
		this.out = out;
	}
	
	// 排他制御用 ArrayListに追加
	public void add(){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.add(this);
		}
	}
	
	// 排他制御用 ArrayListから取得
	public FileReceivedManager get(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			return UdpListener.file_received_manager.get(index);
		}
	}
	
	// 排他制御用 ArrayListから削除
	public void remove(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.remove(index);
		}
	}
	
	// 排他制御用 ArrayListの情報を上書き
	// 宛先・ファイル名が呼び出し元(this)と一致する経過記録に上書き
	public void set(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.set(index, this);
		}
	}
}
