package jp.ac.ehime_u.cite.udptest;

import java.net.*;
import java.io.*;
import java.util.*;

// RREP受信時にACKを返す
// 片方向リンクの検知に用いる
public class RREP_ACK {

	// フォーマット [数字]はバイト列中の位置を示す
	byte type;		// [0] 4:RREP_ACKを示す
	byte reserved;	// [1] 空バイト,使用されない(拡張用)

	// RREP_ACKの送信
	// 引数1: ACKを返す宛先ノードのアドレス
	// 引数2: port番号
	public void send(InetAddress destination_inet,int port){

		type = 4;
		reserved = 0;

		// 送信バイト列
		byte[] send_buffer = new byte[2];

		send_buffer[0] = type;
		send_buffer[1] = reserved;

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
        			 new InetSocketAddress(destination_inet.getHostAddress(), port);
        
        // UDPパケット
        DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(send_buffer, send_buffer.length, remoteAddress);
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
        
        System.out.println("RREP_ACKメッセージを送信しました");	//###デバッグ用###
        	
        //データグラムソケットを閉じる
        soc.close();
	}
}