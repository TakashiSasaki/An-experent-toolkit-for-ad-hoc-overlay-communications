package jp.ac.ehime_u.cite.udptest;
/* 未完成部分のキーワード「###」 */

import java.net.*;
import java.io.*;
import java.util.*;

public class RREP {
	// RREPメッセージのフィールド	以下[フォーマット上の位置(バイト)]、解説
	byte type;		// [0] メッセージタイプ
	byte flag;		// [1] 先頭2ビットのみフラグ(RA)、残りは0
					//	Repairフラグ；マルチキャスト用
					//	Acknowledgementフラグ→RREP-ACKの発行を許可
					
	byte reserved_prefix;	// [2] 予約済み：0として送信され、使用しない+プレフィクス
	byte newHopCount;	// [3] ホップ数
	byte[] toIpAdd;		// [4-7] あて先ノードのIPアドレス
	int toSeqNum;		// [8-11] 宛先ノードへの経路において、送信元ノードによって過去の受信した最新のシーケンス番号
	byte[] fromIpAdd;	// [12-15] 送信元ノードのIPアドレス
	int lifeTime;		// [16-19] 生存期間→経路が有効であると考えられるときのRREPを受け取るための時間
	
	/********************************************************
	 * HELLO メッセージの場合、形式が異なる
	 * byte type;			// [0] メッセージタイプ
	 * byte hopCount;		// [1] ホップ数
	 * byte[] toIpAdd;		// [2-5] あて先ノードのIPアドレス
	 * int toSeqNum;		// [6-9] 宛先ノードへの経路において、送信元ノードによって過去の受信した最新のシーケンス番号
	 * int lifeTime;		// [10-13] 生存期間→経路が有効であると考えられるときのRREPを受け取るための時間
	 ********************************************************/
	
	
	// RREPメッセージの送信
	// 引数：前ホップのノードのアドレス(InetAddress型),RREPのデータ（RREPの宛先ＩＰアドレスはRREPの送信元ＩＰアドレスだから)
	
	public void reply(InetAddress str, byte[] soushinmoto,byte[] atesaki,int port,byte hopNum,int seq,int life) {
		
		// 各フィールドの初期化
		type = 2;	// RREPを示す
		flag = 2<<5;	// Rフラグを0,Aフラグを1
		reserved_prefix = 0;
		newHopCount = hopNum;
		
		//受け取ったバイト配列のデータの送信元のＩＰアドレスdata[16~19]の４バイトを
		//ＲＲＥＰの宛先ＩＰアドレスのフィールドにコピーし，それを送信するパケットのバイト配列の4番目からコピーする
		//宛先ＩＰアドレスにはＲＲＥＰを作成した自分自身のアドレスが入るので注意
		toIpAdd = atesaki;

		toSeqNum = seq;
		
		fromIpAdd = soushinmoto;
		
		lifeTime = life;

		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[20];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = reserved_prefix;
		sendBuffer[3] = newHopCount;
		System.arraycopy(toIpAdd			  ,0,sendBuffer,4,4);
		System.arraycopy(intToByte(toSeqNum)  ,0,sendBuffer,8,4);
		System.arraycopy(fromIpAdd			  ,0,sendBuffer,12,4);
		System.arraycopy(intToByte(lifeTime)  ,0,sendBuffer,16,4);
		
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
        
        System.out.println("RREPメッセージを送信しました");	//###デバッグ用###
        	
        //データグラムソケットを閉じる
        soc.close();
		
        final byte[] next_hop_address = str.getAddress();
        
        // ACK要求リストに次ホップノードを追加
        UdpListener.ack_demand_list.add(next_hop_address);
        
		// 一定時間後、RREP_ACKが戻ってこなければ片方向リンクと見なし
		// BlackListにノードを追加する
		// BlackListに含まれる時間はExpanding_Ring_Searchが終わるまで
		try {
			
			new Thread(new Runnable() {
				public void run() {
					// 一定時間停止する
					try {
						Thread.sleep(2 * AODV_Activity.NODE_TRAVERSAL_TIME
								* (1 + AODV_Activity.TIMEOUT_BUFFER));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// ack_demand_listに停止前に追加したアドレスが存在すれば
					// RREP_ACKが戻ってきていないことを示す
					if(UdpListener.ack_demand_list.contains(next_hop_address)){
						// 片方向であることを示すBlackListに追加
						UdpListener.black_list.add(new BlackData(next_hop_address,
								new Date().getTime() + AODV_Activity.BLACKLIST_TIMEOUT));
					}
					
					// 要求リストから消しておく
					UdpListener.ack_demand_list.remove(next_hop_address);
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// RREPメッセージの送信（フラグ指定用のオーバーロード ###）
	// 引数：送信先byte[4]、フラグboolean[5]
	/* ..... */
	
	
	/***** RREPメッセージの転送 *****/
	
	// 引数：受け取ったバイト配列，RREP転送先のＩＰアドレス
	public void reply2(byte[] data, InetAddress lastNODE,int port){
		
		// ホップ数+1
		data[3]++;

		//データグラムソケットを開く
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
	    // UDPパケットを送信する先となるアドレス
	    InetSocketAddress remoteAddress =
	    			 new InetSocketAddress(lastNODE.getHostAddress(), port);
	    
	    // UDPパケット
	    DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(data, 20, remoteAddress);
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
	    
	    System.out.println("RREPメッセージを転送しました");	//###デバッグ用###
	    	
	    //データグラムソケットを閉じる
	    soc.close();
	    
        final byte[] next_hop_address = lastNODE.getAddress();
        
        // ACK要求リストに次ホップノードを追加
        UdpListener.ack_demand_list.add(next_hop_address);
        
		// 一定時間後、RREP_ACKが戻ってこなければ片方向リンクと見なし
		// BlackListにノードを追加する
		// BlackListに含まれる時間はExpanding_Ring_Searchが終わるまで
		try {
			
			new Thread(new Runnable() {
				public void run() {
					// 一定時間停止する
					try {
						Thread.sleep(2 * AODV_Activity.NODE_TRAVERSAL_TIME
								* (1 + AODV_Activity.TIMEOUT_BUFFER));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					// ack_demand_listに停止前に追加したアドレスが存在すれば
					// RREP_ACKが戻ってきていないことを示す
					if(UdpListener.ack_demand_list.contains(next_hop_address)){
						// 片方向であることを示すBlackListに追加
						UdpListener.black_list.add(new BlackData(next_hop_address,
								new Date().getTime() + AODV_Activity.BLACKLIST_TIMEOUT));
					}
					
					// 要求リストから消しておく
					UdpListener.ack_demand_list.remove(next_hop_address);
				}
			}).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// HELLOメッセージの送信（TTL=1のRREP）
	// 引数：シーケンス番号、生存時間
	public void send(int seq,int port,int life,byte[] my_address) throws Exception{
		
		type = 2;	// HELLOメッセージ
		newHopCount = 0;
		
		// 自ノードのアドレス
		toIpAdd  = my_address;
		toSeqNum = seq;
		
		lifeTime = life;
		
		
		// UDPパケットに含めるデータ
		byte[] sendBuffer = new byte[14];
		
		sendBuffer[0] = type;
		sendBuffer[1] = newHopCount;
		System.arraycopy(toIpAdd			  ,0,sendBuffer,2,4);
		System.arraycopy(intToByte(toSeqNum)  ,0,sendBuffer,6,4);
		System.arraycopy(intToByte(lifeTime)  ,0,sendBuffer,10,4);
		
		// データグラムソケットを開く
		DatagramSocket soc = new DatagramSocket();
		
        // ブロードキャスト
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress("255.255.255.255", port);
        
        // UDPパケット
        DatagramPacket sendPacket =
            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
        
        // DatagramSocketインスタンスを生成して、UDPパケットを送信
        new DatagramSocket().send(sendPacket);
        
        //データグラムソケットを閉じる
        soc.close();
		
	}
	
	// 受信したRREPメッセージが自身のノード宛のものか調べる
	// 引数：RREPメッセージ
	public boolean isToMe(byte[] receieveBuffer,int length,byte[] my_address){
		
		// HELLOメッセージならtrueで問題なし
		if(length == 14)
			return true;
		
		// 宛先IPアドレスのコピー先を作成
		byte[] fromIpAdd = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receieveBuffer,12,fromIpAdd,0,4);
		
		if(Arrays.equals(fromIpAdd,my_address))
				return true;
		else return false;
	}
	// RREPメッセージからRフィールドを返す
	public boolean getFlagR(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return false;
		
		if( (RREPMes[1]&(2<<6)) ==1)
			return true;
		else return false;
	}
	// RREPメッセージからAフィールドを返す
	public boolean getFlagA(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return false;
		
		if( (RREPMes[1]&(2<<5)) ==1)
			return true;
		else return false;
	}
	// RREPメッセージからnewHopCountフィールドを返す
	public byte getHopCount(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return RREPMes[1];
		
		return RREPMes[3];
	}
	
	// RREPメッセージからtoIpAddフィールドを返す
	public byte[] getToIpAdd(byte[] RREPMes,int length){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,2,buf,0,4);
		else
			System.arraycopy(RREPMes,4,buf,0,4);
		
		return buf;
	}
	// RREPメッセージからtoSeqNumフィールドを返す
	public int getToSeqNum(byte[] RREPMes,int length){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,6,buf,0,4);
		else
			System.arraycopy(RREPMes,8,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	// RREPメッセージからfromoIpAddフィールドを返す
	public byte[] getFromIpAdd(byte[] RREPMes,int length){
		
		// HELLO?
		if(length == 14)
			try {
				return getByteAddress(AODV_Activity.getIPAddress());
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		System.arraycopy(RREPMes,12,buf,0,4);
		
		return buf;
	}
	// RREPメッセージからlifeTimeフィールドを返す
	public int getLifeTime(byte[] RREPMes,int length){
		
		// 該当部分のbyte[]を抜き出し
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,10,buf,0,4);
		else
			System.arraycopy(RREPMes,16,buf,0,4);
		
		// int型に変換
		return byteToInt(buf);
	}
	
	// HopCount++
	public byte[] hopCountInc(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			RREPMes[1]++;
		else
			RREPMes[3]++;
		
		return RREPMes;
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
