package jp.ac.ehime_u.cite.udptest;
/* 未完成部分のキーワード「###」 */

import java.net.*;
import java.io.*;
import java.util.*;

public class RREQ {
	// RREQメッセージのフィールド	以下[フォーマット上の位置(バイト)]、解説
	byte type;		// [0] メッセージタイプ
	byte flag;		// [1] 先頭5ビットのみフラグ(JRGDU)、残りは0
					//	Joinフラグ：マルチキャスト用
					//	Repairフラグ；マルチキャスト用
					//	Gratuitous RREPフラグ : 宛先IPアドレスフィールドによって指定されたノードへ、Gratuitous RREPをユニキャストするかどうかを示す
					//	Destination Onlyフラグ : 宛先ノードだけがこのRREQに対して返信することを示す
					//	未知シーケンス番号 : 宛先シーケンス番号が知られていないことを示す
	byte reserved;	// [2] 予約済み：0として送信され、使用しない
	byte hopCount;	// [3] ホップ数
	int RREQ_ID;	// [4-7] 送信元ノードのIPアドレスとともに受信した時、RREQを識別するためのシーケンス番号
	byte[] toIpAdd;		// [8-11] あて先ノードのIPアドレス
	int toSeqNum;		// [12-15] 宛先ノードへの経路において、送信元ノードによって過去の受信した最新のシーケンス番号
	byte[] fromIpAdd;	// [16-19] 送信元ノードのIPアドレス
	int fromSeqNum;		// [20-23] 送信元ノードへの経路において利用される現在のシーケンス番号
	int timeToLive;		// [24-27] 生存時間TTL、中間ノードを残りいくつまで許すか
	
	// RREQメッセージの送信
	// 引数：送信先(String型)
	public void send(byte[] destination_address, byte[] myAddress, boolean flagJ ,boolean flagR ,boolean flagG ,boolean flagD ,boolean flagU
			,int toSeq ,int fromSeq,int ID,int TTL,int port) {
		
		// 各フィールドの初期化
		type = 1;	// RREQを示す
		// 各フラグを1バイトの先頭5ビットに納める
		flag =	(byte)(((flagJ)? (2<<6):0)	// 10000000
				|((flagR)? (2<<5):0)		// 01000000
				|((flagG)? (2<<4):0)		// 00100000
				|((flagD)? (2<<3):0)		// 00010000
				|((flagU)? (2<<2):0));		// 00001000
		reserved = 0;
		hopCount = 0;
		
		RREQ_ID = ID;
		
		toIpAdd  = destination_address;
		toSeqNum = toSeq;
		
		fromIpAdd = myAddress;
		fromSeqNum = fromSeq;
		timeToLive = TTL;
		
		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[28];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = reserved;
		sendBuffer[3] = hopCount;
		System.arraycopy(intToByte(RREQ_ID)   ,0,sendBuffer,4 ,4);
		System.arraycopy(toIpAdd			  ,0,sendBuffer,8 ,4);
		System.arraycopy(intToByte(toSeqNum)  ,0,sendBuffer,12,4);
		System.arraycopy(fromIpAdd			  ,0,sendBuffer,16,4);
		System.arraycopy(intToByte(fromSeqNum),0,sendBuffer,20,4);
		System.arraycopy(intToByte(timeToLive),0,sendBuffer,24,4);
		
		// データグラムソケットを開く
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
        // UDPパケットを送信する先となるブロードキャストアドレス
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
			soc.send(sendPacket);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
        
        System.out.println("RREQメッセージを送信しました");	//###デバッグ用###
        	
        //データグラムソケットを閉じる
        soc.close();
		
/****************** 以前の通信手段 *******************************/
//		// データグラムソケットを開く
//		DatagramSocket soc = new DatagramSocket();
//		
//        // UDPパケットを送信する先となるブロードキャストアドレス (5100番ポート)
//        InetSocketAddress remoteAddress =
//        			 new InetSocketAddress("133.71.3.255", 5100);
//        
//        // UDPパケット
//        DatagramPacket sendPacket =
//            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
//        
//        // DatagramSocketインスタンスを生成して、UDPパケットを送信
//        new DatagramSocket().send(sendPacket);
//        
//        System.out.println("RREQメッセージを送信しました");	//###デバッグ用###
//        	
//        //データグラムソケットを閉じる
//        soc.close();
/******************************************************************/
        
	}

	/***** RREQメッセージの転送 *****/
	public void send2(byte[] data,int port){
		
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}
		
	    // UDPパケットを送信する先となるブロードキャストアドレス
	    InetSocketAddress remoteAddress =
	    			 new InetSocketAddress("255.255.255.255", port);
	    
	    // UDPパケット
	    DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(data, 28, remoteAddress);
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	    
	    // UDPパケットを送信
	    try {
			soc.send(sendPacket);
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	    
	    System.out.println("RREQメッセージを転送しました");	//###デバッグ用###
	    	
	    //データグラムソケットを閉じる
	    soc.close();
	}
	
	// 受信したRREQメッセージが自身のノード宛のものか調べる
	// 引数：RREQメッセージ
	public boolean isToMe(byte[] receiveBuffer, byte[] myAddress){
		// 宛先IPアドレスのコピー先を作成
		byte[] toIpAdd = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer,8,toIpAdd,0,4);
		
		if(Arrays.equals(toIpAdd,myAddress))
				return true;
		else return false;
	}
	
	// RREQメッセージからJフィールドを返す
	public boolean getFlagJ(byte[] RREQMes){
		if( (RREQMes[1]&(2<<6)) ==1)
			return true;
		else return false;
	}
	// RREQメッセージからRフィールドを返す
	public boolean getFlagR(byte[] RREQMes){
		if( (RREQMes[1]&(2<<5)) ==1)
			return true;
		else return false;
	}
	// RREQメッセージからGフィールドを返す
	public boolean getFlagG(byte[] RREQMes){
		if( (RREQMes[1]&(2<<4)) ==1)
			return true;
		else return false;
	}
	// RREQメッセージからDフィールドを返す
	public boolean getFlagD(byte[] RREQMes){
		if( (RREQMes[1]&(2<<3)) ==1)
			return true;
		else return false;
	}
	// RREQメッセージからUフィールドを返す
	public boolean getFlagU(byte[] RREQMes){
		if( (RREQMes[1]&(2<<2)) ==1)
			return true;
		else return false;
	}
	// RREQメッセージからhopCountフィールドを返す
	public byte getHopCount(byte[] RREQMes){
		return RREQMes[3];
	}
	// RREQメッセージからRREQ_IDフィールドを返す
	public int getRREQ_ID(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,4,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	// RREQメッセージからtoIpAddフィールドを返す
	public byte[] getToIpAdd(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,8,buf,0,4);
		
		return buf;
	}
	// RREQメッセージからtoSeqNumフィールドを返す
	public int getToSeqNum(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,12,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	// RREQメッセージからfromoIpAddフィールドを返す
	public byte[] getFromIpAdd(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,16,buf,0,4);
		
		return buf;
	}
	// RREQメッセージからfromSeqNumフィールドを返す
	public int getFromSeqNum(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,20,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	// RREQメッセージからTTLフィールドを返す
	public int getTimeToLive(byte[] RREQMes){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,24,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	
	// RREQメッセージの送信元シーケンス番号フィールドをセットして返す
	public byte[] setFromSeqNum(byte[] RREQMes,int num){
		// 変更する番号をbyte[]型に
		byte[] seq = intToByte(num);
		
		// 送信元シーケンス番号の部分に上書き
		System.arraycopy(seq,0,RREQMes,20,4);
		return RREQMes;
	}
	// RREQメッセージのTTLをセットして返す
	public byte[] setTimeToLive(byte[] RREQMes,int num){
		// 変更する番号をbyte[]型に
		byte[] TTL = intToByte(num);
		
		// 送信元シーケンス番号の部分に上書き
		System.arraycopy(TTL,0,RREQMes,24,4);
		return RREQMes;
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
	
	// String型のアドレスをbyte[]型に変換
	public byte[] getByteAddress(String str){
		
		// 分割
		String[] s_bara = str.split("\\.");
		
		byte[] b_bara = new byte[s_bara.length];
		for(int i=0;i<s_bara.length;i++){
			b_bara[i] = (byte)Integer.parseInt(s_bara[i]);
		}
		return b_bara;
	}
	
	

	
	
	
}
