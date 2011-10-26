package jp.ac.ehime_u.cite.udptest;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;

import android.os.Handler;
import android.widget.EditText;
import android.widget.ScrollView;

public class UdpListener implements Runnable {

	//　受信した情報を操作するためインスタンス化しておく
	public static RREQ RReq = new RREQ();
	public static RREP RRep = new RREP();
	public static RERR RErr = new RERR();
		
	//　受信したメッセージの内容を表す
	String received_destination_address;	// 宛先IPアドレス
	String received_src_address;			//　送信元IPアドレス
	byte received_type;
	
	Handler handler;
	EditText editText;
	ScrollView scrollView;

	// 受信用の配列やパケットなど
	private byte[] buffer = new byte[2000];
	private int port;
	private DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	private DatagramSocket socket;
	private byte[] my_address;

	// コンストラクタ
	// 引数1:Handler	メインスレッドのハンドル(Handlerを使うことでUIスレッドの持つキューにジョブ登録ができる)
	// 引数2:TextView	受信結果を表示するTextView
	// 引数3:port_		ポート番号(受信)
	// 引数4:max_packets 記録する最大パケット数(受信可能回数)
	public UdpListener(Handler handler_, EditText edit_text,
			int port_, int max_packets) throws SocketException {
		port = port_;
		socket = new DatagramSocket(port);
		handler = handler_;
		editText = edit_text;
		try {
			my_address = new RREQ().getByteAddress(AODV_Activity.getIPAddress());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
	label: while (true) {
			try {
				socket.receive(packet); // blocking
				// 受信したデータを抽出	received_dataをreceiveBufferに変更したよ
				byte[] receiveBuffer = packet.getData();//cut_byte_spare(packet.getData() ,packet.getLength());
				
				//前ホップのノードのアドレスを取得
		    	InetAddress cAddr = packet.getAddress();
				
				
				//　受信したデータから個々の情報を抜き出す				
				received_type = receiveBuffer[0];	//まずメッセージのタイプを調べる
				if(received_type == 1){		//RREQの場合
					received_destination_address = InetAddress.getByAddress(RReq.getToIpAdd(receiveBuffer)).toString();				
					received_src_address = InetAddress.getByAddress(RReq.getFromIpAdd(receiveBuffer)).toString();
				}
				else if(received_type == 2){	//RREPの場合
					received_destination_address = InetAddress.getByAddress(RRep.getToIpAdd(receiveBuffer, receiveBuffer.length)).toString();				
					try {
						received_src_address = InetAddress.getByAddress(RRep.getFromIpAdd(receiveBuffer, receiveBuffer.length)).toString();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				//PrintReceivedDataList();
				
	        	switch( receiveBuffer[0] ){
	        	case 0: // 通信相手からのメッセージ
        			
	        		// 自分宛のメッセージなら
	        		if( isToMe(receiveBuffer,my_address)){
	        			final String s = AODV_Activity.getStringByByteAddress(getAddressSrc(receiveBuffer))
	        				+ ":" + new String(getMessage(receiveBuffer, packet.getLength()));
	        			
	    				handler.post(new Runnable() {
	    					@Override
	    					public void run() {
	    						editText.append(s);
	    						editText.setSelection(editText.getText().toString().length());
	    					}
	    				});
	        		}
	        		else{	// 次ホップへ転送
	        			// 宛先までの有効経路を持っているか検索
        				int index = AODV_Activity.searchToAdd(getAddressDest(receiveBuffer));
        				
        				// 経路を知っていて
        				if(index != -1){
        					// 有効な経路なら
        					if(AODV_Activity.getRoute(index).stateFlag == 1){
        						
        						// 次ホップへそのまま転送
        						sendMessage(receiveBuffer, AODV_Activity.getRoute(index).nextIpAdd);
        						
        						break;
        					}
        				}
        				// 有効な経路を持っていない場合

						// RERRの送信?
						RouteManager.RERR_Sender(AODV_Activity.getRoute(index),port);
        				
	        		}
	        		
	        		break;
	        		
        		case 1:	// RREQ
        			
        			// RREQの受信履歴の中で、古すぎるアドレスの削除
        			if( !AODV_Activity.receiveRREQ_List.isEmpty() ){	// 空でなければ
        				// リストの中で最も古いのは先頭の項目、その生存時間をチェック
        				synchronized (AODV_Activity.pastDataLock) {
        					if(AODV_Activity.receiveRREQ_List.get(0).lifeTime < new Date().getTime() ){
        						AODV_Activity.receiveRREQ_List.remove(0);
        					}
        				}
        			}
        			
        			// 受信履歴リストの中の情報と、RREQ_ID,送信元が一致すればメッセージを無視
        			if( AODV_Activity.RREQ_ContainCheck( RReq.getRREQ_ID(receiveBuffer), RReq.getFromIpAdd(receiveBuffer))){
        				// "重複したRREQメッセージのため無視します\n");
        				continue label;
        			}
        			
        			// ホップ数+1,
        			receiveBuffer[3]++;
        			
        			// 受信したRREQメッセージの情報をListに追加
        			// 引数はRREQ_ID,送信元アドレス
        			AODV_Activity.newPastRReq(RReq.getRREQ_ID(receiveBuffer),RReq.getFromIpAdd(receiveBuffer));
        			
        			
        			// 逆経路を、生存時間短めで記録
        			// 生存時間は、既に経路があるならその生存時間をそのまま、またはMinimalLifeTimeの大きいほうにセット
        			long life;
        			int index;
        			long MinimalLifeTime = new Date().getTime()+ 2*AODV_Activity.NET_TRAVERSAL_TIME 
        				- 2 * RReq.getHopCount(receiveBuffer) * AODV_Activity.NODE_TRAVERSAL_TIME;
        			
        			if( (index = AODV_Activity.searchToAdd(RReq.getFromIpAdd(receiveBuffer))) != -1){
        				life = (AODV_Activity.getRoute(index).lifeTime > MinimalLifeTime)? AODV_Activity.getRoute(index).lifeTime:MinimalLifeTime; 
        			}
        			else{
        				life = MinimalLifeTime;
        			}
        			
        			// シーケンス番号も同様に、既存経路の値と受信RREQ中の値を比較して高いほう
        			// また、既存経路のほうが高い場合、受信したRREQの値をそちらをコピー
        			if(index != -1){
        				if(AODV_Activity.getRoute(index).toSeqNum > RReq.getFromSeqNum(receiveBuffer)){
            				receiveBuffer = RReq.setFromSeqNum(receiveBuffer, AODV_Activity.getRoute(index).toSeqNum);
        				}
        			}

        			// 既に逆経路があるなら上書きset、なければ追加add
        			if(index != -1){
        				AODV_Activity.setRoute(index, new RouteTable( RReq.getFromIpAdd(receiveBuffer), RReq.getFromSeqNum(receiveBuffer)
            					, true, (byte)1, RReq.getHopCount(receiveBuffer), cAddr.getAddress(), life
            					, new HashSet<byte[]>() ));
        			}
        			else{
        				AODV_Activity.addRoute( new RouteTable( RReq.getFromIpAdd(receiveBuffer), RReq.getFromSeqNum(receiveBuffer)
	        					, true, (byte)1, RReq.getHopCount(receiveBuffer), cAddr.getAddress(), life
	        					, new HashSet<byte[]>() ));
        			}
        			
        			// RREQメッセージの内容チェック
        			if(RReq.isToMe(receiveBuffer, my_address)){
        				// RREQ:自分宛のメッセージです
        				// RREPを前ホップノード" + cAddr.getHostAddress() + "にユニキャストします");
        				
        				// 返信前に、シーケンス番号の更新
        				if( RReq.getToSeqNum(receiveBuffer) == AODV_Activity.seqNum+1){
        					AODV_Activity.seqNum++;
        				}
        				
        				// RREPの返信	        				
        				RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer), my_address, port
        						,(byte)0 ,AODV_Activity.seqNum , AODV_Activity.MY_ROUTE_TIMEOUT);
        				
        			}
        			else{
        				// RREQ:自分宛のメッセージではありません");
        				
        				// 宛先までの有効経路を持っているか検索
        				index = AODV_Activity.searchToAdd(RReq.getToIpAdd(receiveBuffer));
        				
        				// 経路を知っていて、Dフラグがオフなら
        				if( (index != -1) && (!RReq.getFlagD(receiveBuffer))){
        					if( AODV_Activity.getRoute(index).stateFlag == 1
        							&& AODV_Activity.getRoute(index).toSeqNum > RReq.getToSeqNum(receiveBuffer)
        							&& !RReq.getFlagD(receiveBuffer)){

	        					// 経路が既知な中間ノードなので、RREPを返信");
	        					
	        					// Gフラグがセットされていれば、宛先ノードにもRREPが必要
	        					// (双方向に経路を確立するために用いる)
	        					if(RReq.getFlagG(receiveBuffer)){
	        						
	        						// 宛先は順経路の次ホップ
	        						InetAddress str = InetAddress.getByAddress(AODV_Activity.getRoute(index).nextIpAdd);
	        						
	        						// RREPの送信
		        					RRep.reply(str, RReq.getToIpAdd(receiveBuffer),
		        							RReq.getFromIpAdd(receiveBuffer), port,
		        							AODV_Activity.getRoute(index).hopCount, RReq.getFromSeqNum(receiveBuffer),
		        							(int)(AODV_Activity.getRoute(index).lifeTime - new Date().getTime()));
	        					}
	        					
	        					// 経路のprecursorListに追加
	        					// まず順経路の更新
	        					// 順経路のprecursorListに、逆経路の次ホップを追加。エラー時にRERRを伝えるノード
	        					// すでにListに含まれていても、HashSetにより重複は認められないのでOK
	        					RouteTable route = AODV_Activity.getRoute(index);		// 一旦リストから出す
	        					route.preList.add(cAddr.getAddress());		// 書き加え
	        					AODV_Activity.setRoute(index, route);		// リストに上書き
	        					
	        					// 次に逆経路の更新
	        					// 逆経路のprecursorListに、順経路の次ホップを追加。エラー時にRERRを伝えるノード
	        					int index2 = AODV_Activity.searchToAdd(RReq.getFromIpAdd(receiveBuffer));
	        					route = AODV_Activity.getRoute(index2);
	        					route.preList.add(AODV_Activity.getRoute(index).nextIpAdd);
	        					AODV_Activity.setRoute(index2, route);
	        					
	        					
	        					// RREPの返信
	        					RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer),
	        							RReq.getToIpAdd(receiveBuffer), port,
	        							AODV_Activity.getRoute(index).hopCount, AODV_Activity.getRoute(index).toSeqNum,
	        							(int)(AODV_Activity.getRoute(index).lifeTime - new Date().getTime()));
	        					
	        					continue label;
        					}
        				}
        				// (else 上記if(true)のときはcontinueで通らない)
        				// =宛先までの有効経路を持っていない場合、または持っていてDフラグがオンの場合
        				{
        					// TTLを減らすタイミングはこの後にしているので、比較は1以下
        					// 順番変えても別にいい気もする
        					if(RReq.getTimeToLive(receiveBuffer)<=1){
        						// "TTLが0以下なので転送しません");
        					}
        					else{
        						// 条件を満たせば、中継するためブロードキャスト
        						// 引数のTTL--;
        						receiveBuffer = RReq.setTimeToLive(receiveBuffer, RReq.getTimeToLive(receiveBuffer)-1);
        						
        						RReq.send2(receiveBuffer,port);
        					}
        				}
        			}
        			break;
        	
        			
        		case 2:	//RREPを受け取った場合
        			
        			// 受信データのサイズ
        			int mesLength = packet.getLength();
        			
        			// 自分自身からのRREPなら無視
        			byte[] local_address = new RREQ().getByteAddress("127.0.0.1");
        			
        			if( Arrays.equals(RRep.getToIpAdd(receiveBuffer,mesLength),local_address)
        					|| Arrays.equals(RRep.getToIpAdd(receiveBuffer,mesLength), my_address)){
        				continue label;
        			}
        			
        			// ホップ数++
        			receiveBuffer = RRep.hopCountInc(receiveBuffer, mesLength);
        			
        			// 順経路（RREQ送信元⇒宛先）が存在するかどうか検索
    				int index2 = AODV_Activity.searchToAdd(RRep.getToIpAdd(receiveBuffer,mesLength));
    				
    				// 存在しない場合、順経路の作成
    				if( index2 == -1 ){
    					AODV_Activity.addRoute( new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<byte[]>() )); 
	        			
	        			// 経路が追加されました"
    				}
    				// 順経路が存在する場合
    				else{
    					// 以下のいずれかの条件を満たしている場合、経路を更新する
    					// 1.既存経路のシーケンス番号が無効であると記録されている
    					// 2.RREPの宛先シーケンス番号＞既存経路の番号であり、有効
    					// 3.シーケンス番号が等しく既存経路が無効である
    					// 4.シーケンス番号が等しくホップ数が既存経路よりも小さい
    					if(	(AODV_Activity.getRoute(index2).validToSeqNumFlag == false)
    						||(RRep.getHopCount(receiveBuffer, mesLength) > AODV_Activity.getRoute(index2).toSeqNum)
    						||( (AODV_Activity.getRoute(index2).stateFlag != 1)
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == AODV_Activity.getRoute(index2).toSeqNum))
    						||( (RRep.getHopCount(receiveBuffer, mesLength) < AODV_Activity.getRoute(index2).hopCount) 
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == AODV_Activity.getRoute(index2).toSeqNum)))
    					{
    						// 順経路の上書き
    						AODV_Activity.setRoute(index2, new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<byte[]>() ));
    					}
    				}
    				
    				// さらに、RREPを送信してきた前ノードが
    				// 何かの経路の次ホップなら、その経路の生存時間を更新
    				for(int i=0;i<AODV_Activity.routeTable.size();i++){
    					if( Arrays.equals((AODV_Activity.getRoute(i).nextIpAdd) , cAddr.getAddress())){
    						RouteTable route = AODV_Activity.getRoute(i);
    						// 現在の生存時間と、HELLOの式を比較して大きい方に更新
    						if(route.lifeTime < (AODV_Activity.ALLOWED_HELLO_LOSS * AODV_Activity.HELLO_INTERVAL)){
    							route.lifeTime = AODV_Activity.ALLOWED_HELLO_LOSS * AODV_Activity.HELLO_INTERVAL;
    						}
    					}
    				}
        			
        			if(RRep.isToMe(receiveBuffer,mesLength,my_address)){
        				// RREP:送信元ＩＰアドレスが自分です
        				
        			}
        			else{
        				// RREP:送信元ＩＰアドレスが自分ではありません
        				
        				// 順経路を示すindex2を再検索、更新
        				index2 = AODV_Activity.searchToAdd(RRep.getToIpAdd(receiveBuffer,mesLength));
        				// RREQ生成ノードへの経路（逆経路）を検索
        				int index3 = AODV_Activity.searchToAdd(RRep.getFromIpAdd(receiveBuffer,mesLength));
        				
        				// 順経路のPrecursorListに、逆経路への次ホップを追加
    					// すでにListに含まれていても、HashSetにより重複は認められないのでOK
    					RouteTable route = AODV_Activity.getRoute(index2);		// 一旦リストから出す
    					route.preList.add(AODV_Activity.getRoute(index3).nextIpAdd);	// 書き加え
    					AODV_Activity.setRoute(index2, route);					// リストに上書き
    					
    					// 逆経路のPrecursorListに、順経路の次ホップを追加
    					// また、生存時間も更新
    					route = AODV_Activity.getRoute(index3);					// 一旦リストから出す
    					route.preList.add(AODV_Activity.getRoute(index2).nextIpAdd);	// 書き加え
    					
    					// 現在の生存時間と現在時刻+ACTIVE_ROUTE_TIMEOUTの最大値側にセット
    					if(route.lifeTime < (new Date().getTime()+AODV_Activity.ACTIVE_ROUTE_TIMEOUT)){
    						route.lifeTime = new Date().getTime()+AODV_Activity.ACTIVE_ROUTE_TIMEOUT;
    					}
    					AODV_Activity.setRoute(index3, route);					// リストに上書き
        				
        				// RREPを前ホップノードに転送
        				RRep.reply2(receiveBuffer, InetAddress.getByAddress(AODV_Activity.getRoute(index3).nextIpAdd),port);
        			
        			}
        			break;
        			
        		case 3:	// RERRを受け取った場合
        			
        			// "このメッセージはRERRです\n");
        			
        			// 経路の検索
        			index = AODV_Activity.searchToAdd(RErr.getIpAdd(receiveBuffer));
        			
        			// 経路が存在するなら
        			if(index != -1){
        				// リストから出し、フィールドの更新
        				RouteTable route = AODV_Activity.getRoute(index);
        				
        				route.stateFlag = 2;	// 無効化
        				route.lifeTime  = (new Date().getTime()+AODV_Activity.DELETE_PERIOD);	// 削除時間の設定
        				if(route.validToSeqNumFlag){	// シーケンス番号が有効なら
        					route.toSeqNum = RErr.getSeqNum(receiveBuffer);		// ｼｰｹﾝｽ番号も更新
        				}
        				
        				// 読みだした場所に上書き
        				AODV_Activity.setRoute(index, route);
        				
						// ローカルリペアを行えるホップ数か？
						if(route.hopCount <= AODV_Activity.MAX_REPAIR_TTL){
							RouteManager.localRepair(route,port,my_address);
						}
						else{
							final byte[] destination_address = route.toIpAdd;
							
		    				handler.post(new Runnable() {
		    					@Override
		    					public void run() {
		    						editText.append("Route[To:"+AODV_Activity.getStringByByteAddress(destination_address)+"] cannot use\n");
		    					}
		    				});
							
							// RERRの送信
							RouteManager.RERR_Sender(route,port);
						}
        			}
        			
        			break;
	        	}
			
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}
	
	// バイト配列の取り出し（余分な部分の削除）
	byte[] cut_byte_spare(byte[] b,int size){
		
		byte[] slim_byte = new byte[size];
		System.arraycopy(b, 0, slim_byte, 0, size);
		
		return slim_byte;
	}
	
	// メッセージ0が自分宛か？
	boolean isToMe(byte[] receiveBuffer,byte[] myAddress){
		// 宛先IPアドレスのコピー先を作成
		byte[] toIpAdd = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer,1,toIpAdd,0,4);
		
		if(Arrays.equals(toIpAdd,myAddress))
				return true;
		else return false;
	}
	
	// メッセージ0の中から宛先アドレスを抜き出す
	byte[] getAddressDest(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 1, add, 0, 4);
		
		return add;
	}
	
	// メッセージ0の中から送信元アドレスを抜き出す
	byte[] getAddressSrc(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer, 5, add, 0, 4);
		
		return add;
	}
	
	// メッセージ0の中から伝送データを抜き出す
	byte[] getMessage(byte[] receiveBuffer,int length){
		// 宛先IPアドレスのコピー先を作成
		byte[] message = new byte[length-9];
		
		// 該当部分を抜き出し
		System.arraycopy(receiveBuffer,9,message,0,length-9);
		
		return message;
	}
	
	// メッセージ0を宛先へ転送
	void sendMessage(byte[] receiveBuffer,byte[] destination_address){
		
		// データグラムソケットを開く
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
        // UDPパケットを送信する先となるアドレス
        InetSocketAddress remoteAddress = null;
		try {
			remoteAddress = new InetSocketAddress( InetAddress.getByAddress(destination_address), port);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
        
        // UDPパケット
        DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(receiveBuffer, receiveBuffer.length, remoteAddress);
		} catch (SocketException e) {
			e.printStackTrace();
		}
        
        // DatagramSocketインスタンスを生成して、UDPパケットを送信
        try {
			soc.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
        	
        //データグラムソケットを閉じる
        soc.close();
	}
}