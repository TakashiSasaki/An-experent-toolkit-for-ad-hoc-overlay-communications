package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.widget.EditText;



//一定時間ごとにHELLOメッセージの送信と、既存経路の生存時間をチェック
//接続切れがあった場合、RERRをPrecursorに送信する
//ついでに、DELETE時間がたった経路を削除する
public class RouteManager implements Runnable {
	
	RouteTable route;
	static Handler handler;
	EditText textDestPort;
	static EditText text_view_received;
	byte[] myAddress;
	
	public RouteManager(Handler handler_,EditText text_dest_port, EditText text_view_rec) throws IOException{
		handler = handler_;
		textDestPort = text_dest_port;
		myAddress = new RREQ().getByteAddress(AODV_Activity.getIPAddress());
		text_view_received = text_view_rec;
	}
	
	@Override
	public void run() {
		while(true){
			
			// 経路が存在するならば
			if(AODV_Activity.routeTable.isEmpty() != true){
				
				int port = Integer.parseInt(textDestPort.getText().toString());
				
				// 経路を維持するHELLOメッセージの送信
				// 一定時間内にブロードキャストしていれば実行しない
				if(AODV_Activity.do_BroadCast == false){
					try {
						new RREP().send(AODV_Activity.seqNum, port, 
								AODV_Activity.ALLOWED_HELLO_LOSS *
								AODV_Activity.HELLO_INTERVAL
								, myAddress);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				// 既存経路の生存時間チェック
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					
					route = AODV_Activity.getRoute(i);
					
					// 無効経路が削除される時間であるか
					if( (route.lifeTime+AODV_Activity.DELETE_PERIOD) < new Date().getTime()){
						
						// 無効経路の削除
						AODV_Activity.removeRoute(i);
						
						// 削除によるiのズレを修正
						i--;
					}
					
					// 有効経路が無効経路になる時間（寿命）であるか
					else if( (route.stateFlag==1) && (route.lifeTime < new Date().getTime())){
						
						// 無効化
						route.stateFlag = 2;
						route.lifeTime  = (new Date().getTime()+AODV_Activity.DELETE_PERIOD);
						route.toSeqNum++;
						
						// 上書き
						AODV_Activity.setRoute(i, route);
						
						// ローカルリペアを行えるホップ数か？
						if(route.hopCount <= AODV_Activity.MAX_REPAIR_TTL){
							localRepair(route,port,myAddress);
						}
						else{
							// RERRの送信
							RERR_Sender(route,port);
						}
					}
				}
			}
			
			AODV_Activity.do_BroadCast = false;
			
			try { // 一定時間ごとにループ処理、処理時間は無視？
				Thread.sleep(AODV_Activity.HELLO_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
	
	// 経路の自動修復
	public static void localRepair(RouteTable route, final int port, byte[] myAdd){
		
		// ***** RREQの送信 *****
		int TTL = AODV_Activity.MIN_REPAIR_TTL + AODV_Activity.LOCAL_ADD_TTL;
		
		route.toSeqNum++;
		AODV_Activity.RREQ_ID++;
		AODV_Activity.seqNum++;
		
		// 自分が送信したパケットを受信しないようにIDを登録
		AODV_Activity.newPastRReq(AODV_Activity.RREQ_ID, myAdd);
		
		AODV_Activity.do_BroadCast = true;
		
		try {
			new RREQ().send(route.toIpAdd, myAdd
					, false, false, false, false, false
					, route.toSeqNum, AODV_Activity.seqNum, AODV_Activity.RREQ_ID, TTL, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// 経路探索期間が過ぎた後、経路が修復されたかチェック
		long waitTime = 2 * AODV_Activity.NODE_TRAVERSAL_TIME * (TTL + AODV_Activity.TIMEOUT_BUFFER);
		final byte[] toIp = route.toIpAdd;
		final RouteTable route_f = route;
		
		Timer mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
				@Override
				public void run(){
					int index = AODV_Activity.searchToAdd(toIp);
					
					// 経路が追加されていて、かつホップ数が修復前以下なら、修復完了
					// それ以外の場合、RERRを送信する
					if(index == -1){
						RERR_Sender(route_f,port);
					}
					else{
						if(AODV_Activity.getRoute(index).hopCount > route_f.hopCount){
							RERR_Sender(route_f,port);
							
							final byte[] destination_address = route_f.toIpAdd;
		    				handler.post(new Runnable() {
		    					@Override
		    					public void run() {
		    						text_view_received.append("Route[To:"+AODV_Activity.getStringByByteAddress(destination_address)+"] cannot use\n");
		    					}
		    				});
						}
					}
				}
			}, waitTime);
	}
	
	// RERRの送信
	public static void RERR_Sender(RouteTable route,int port){
		// precursorListの全アドレスにRERRの送信
		if(route.preList.size()==1){
			// 伝えるノードが1つだけである場合
			
			Iterator<byte[]> it = route.preList.iterator();
			byte[] atesaki = it.next();
			
			// RERRをユニキャスト
			try {
				new RERR().send(false, route.nextIpAdd, route.toSeqNum, atesaki, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(route.preList.size()>2){
			// 伝えるノードが複数である場合
			
			// RERRをブロードキャスト
			try {
				new RERR().send(false, route.nextIpAdd, route.toSeqNum, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
