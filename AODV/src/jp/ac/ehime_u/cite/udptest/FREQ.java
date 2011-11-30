package jp.ac.ehime_u.cite.udptest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

// ファイル送信用クラス
public class FREQ {
	
	byte type;	// メッセージタイプ 10台を使用
	byte[] destination_address;	// 宛先アドレス
	byte[] source_address;		// 送信元アドレス
	byte[] file_name_b;			// ファイル名(byte[])
	int file_name_b_length;		// ファイル名のサイズ
	
	
	// ファイルパケット要求メッセージの送信
	// 逐次確認プロトコルにおける受信側の応答
	public void file_req(InetAddress str, byte[] soushinmoto,byte[] atesaki,int step_next_no,String file_str,int port) {
		
		/********************************************************
		 * メッセージフォーマット
		 * byte type;			// [0] メッセージタイプ = 11
		 * byte[] dest_add;		// [1-4] 宛先IPアドレス
		 * byte[] source_add;	// [5-8] 送信元IPアドレス
		 * int step_next_no;	// [9-12] 要求するパケットの番号
		 * String file_name;	// [13-??] ファイル名(可変長)
		 ********************************************************/	
		// 各フィールドの初期化
		type = 11;
		destination_address = atesaki;
		source_address = soushinmoto;
		file_name_b = file_str.getBytes();
		file_name_b_length = file_name_b.length;

		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[13+file_name_b_length];
		
		sendBuffer[0] = type;
		System.arraycopy(destination_address  	,0,sendBuffer,1,4);
		System.arraycopy(source_address		  	,0,sendBuffer,5,4);
		System.arraycopy(intToByte(step_next_no),0,sendBuffer,9,4);
		System.arraycopy(file_name_b		  	,0,sendBuffer,13,file_name_b_length);
		
		// データグラムソケットを開く
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
        // UDPパケットを送信する先となる前ホップノードのアドレス
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress(str.getHostAddress(), port);
        
        // UDPパケット
        DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        
        // DatagramSocketインスタンスを生成して、UDPパケットを送信
        try {
			soc.send(sendPacket);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
        //データグラムソケットを閉じる
        soc.close();
	}
	
	// メッセージが自分宛か？
	boolean isToMe(byte[] receiveBuffer,byte[] myAddress){
		// 宛先IPアドレスのコピー先を作成
		byte[] toIpAdd = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer,1,toIpAdd,0,4);
		
		if(Arrays.equals(toIpAdd,myAddress))
				return true;
		else return false;
	}
	
	// メッセージの中から宛先アドレスを抜き出す
	byte[] getAddressDest(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 1, add, 0, 4);
		
		return add;
	}
	
	// メッセージの中から送信元アドレスを抜き出す
	byte[] getAddressSrc(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 5, add, 0, 4);
		
		return add;
	}
	
	// メッセージの中から、要求パケットNoを抜きだす
	int getStepNextNo(byte[] receiveBuffer){
		// 数値のコピー先を作成
		byte[] step_no = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 9, step_no, 0, 4);
		
		return byteToInt(step_no);
	}
	
	// メッセージの中から、ファイル名を抜き出す
	String getFileName(byte[] receiveBuffer,int length){
		// 数値のコピー先を作成
		byte[] file_name = new byte[length-13];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 13, file_name, 0, length-13);
		
		return new String(file_name);
	}
	
	/************* 型変換用メソッド *************/
	// int型をbyte[]型へ変換
	public byte[] intToByte(int num){
		
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
	
	// byte[]型をint型へ変換
	public int byteToInt(byte[] num){
		
		int value = 0;
		// バイト配列の入力を行うストリーム
		ByteArrayInputStream bin = new ByteArrayInputStream(num);
		
		// DataInputStreamと連結
		DataInputStream in = new DataInputStream(bin);
		
		try{	// intを読み込み
			value = in.readInt();
		}catch(IOException e){
			System.out.println(e);
		}
		return value;
	}
	
	
}
