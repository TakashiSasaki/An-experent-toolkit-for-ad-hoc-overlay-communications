package jp.ac.ehime_u.cite.udptest;
/* 未完成部分のキーワード「###」 */

import java.net.*;
import java.io.*;
import java.util.*;

public class RERR {
	// RERRメッセージのフィールド	以下[フォーマット上の位置(バイト)]、解説
	byte type;		// [0] メッセージタイプ
	byte flag;		// [1] NoDeleteフラグ：ノードがリンクのLocalRepairを行っており、上流ノードが経路を削除すべきでない場合
	byte destCount;	// [2] 不達宛先数：RERRに含まれている不達宛先数(1以上)
	byte[] IpAdd;	// [3-6] 不達となったノードのIPアドレス
	int SeqNum;		// [7-10] 以前にIpAddフィールドでリストアップされた宛先ノードに対する経路のシーケンス番号
	
	// RERRメッセージの送信、ユニキャスト用
	// 引数：
	public void send(boolean flagN,byte[] add,int seq,byte[] atesaki,int port){
		
		type = 3;	// RERRメッセージ
		
		flag = (byte)((flagN)? 2<<6:0);
		destCount = 1;
		IpAdd  = add;
		SeqNum = seq;
		
		
		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[11];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = destCount;
		System.arraycopy(IpAdd			  ,0,sendBuffer,3,4);
		System.arraycopy(intToByte(SeqNum),0,sendBuffer,7,4);
	
		// データグラムソケットを開く
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
        // UDPパケットを送信する先となる前ホップノードのアドレス
        InetSocketAddress remoteAddress = null;
		try {
			remoteAddress = new InetSocketAddress(InetAddress.getByAddress(atesaki).getHostAddress(), port);
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        
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
			new DatagramSocket().send(sendPacket);
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
	
	// RERRメッセージの送信、ブロードキャスト用
	public void send(boolean flagN,byte[] add,int seq,int port){
		
		type = 3;	// RERRメッセージ
		
		flag = (byte)((flagN)? 2<<6:0);
		destCount = 1;
		IpAdd  = add;
		SeqNum = seq;
		
		
		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[11];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = destCount;
		System.arraycopy(IpAdd			  ,0,sendBuffer,3,4);
		System.arraycopy(intToByte(SeqNum),0,sendBuffer,7,4);
	
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
        			 new InetSocketAddress("255.255.255.255", port);
        
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
			new DatagramSocket().send(sendPacket);
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
	
	// RERRメッセージからIpAddフィールドを返す
	public byte[] getIpAdd(byte[] RERRMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RERRMes,3,buf,0,4);
		
		return buf;
	}
	// RERRメッセージからSeqNumフィールドを返す
	public int getSeqNum(byte[] RERRMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RERRMes,7,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	
	
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