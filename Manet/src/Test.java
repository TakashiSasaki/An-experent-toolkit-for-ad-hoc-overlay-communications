import java.net.*;
import java.io.*;
import java.util.*;

import aodv.*;



public class Test {
	
	public static RREQ RReq = new RREQ();
	public static RREP RRep = new RREP();
	public static RERR RErr = new RERR();
	
	// ルートテーブル
	public static ArrayList<RouteTable> list = new ArrayList<RouteTable>();
	
	// PATH_DISCOVERY_TIMEの間に受信したRREQの送信元とIDを記録
	public static ArrayList<pastData> receiveRREQ_List = new ArrayList<pastData>();
	
	// その他変数
	public static int RREQ_ID = 0;
	public static int seqNum  = 0;
	public static boolean do_BroadCast = false;		// 一定時間内に何かﾌﾞﾛｰﾄﾞｷｬｽﾄしたかどうか
	
	//	終了フラグ(受信スレッド終了時に他のスレッドも終了)
	public static boolean flag =false;
	
	// パラメータのデフォルト値を宣言
	
	public static final int ACTIVE_ROUTE_TIMEOUT	= 3000; //[ms]
	public static final int ALLOWED_HELLO_LOSS		= 2;
	public static final int HELLO_INTERVAL			= 1000; //[ms]
	public static final int DELETE_PERIOD			= (ACTIVE_ROUTE_TIMEOUT >= HELLO_INTERVAL)?
										5*ACTIVE_ROUTE_TIMEOUT : 5*HELLO_INTERVAL;
	public static final int LOCAL_ADD_TTL			= 2;
	public static final int MY_ROUTE_TIMEOUT		= 2 * ACTIVE_ROUTE_TIMEOUT;
	public static final int NET_DIAMETER			= 35;
	public static final int MAX_REPAIR_TTL			= (int)(0.3 * NET_DIAMETER);
	public static       int MIN_REPAIR_TTL			= -1;	// 宛先ノードへ知られている最新のホップ数
	public static final int NODE_TRAVERSAL_TIME		= 40;	//[ms]
	public static final int NET_TRAVERSAL_TIME		= 2 * NODE_TRAVERSAL_TIME * NET_DIAMETER;
	public static final int NEXT_HOP_WAIT			= NODE_TRAVERSAL_TIME + 10;
	public static final int PATH_DISCOVERY_TIME		= 2 * NET_TRAVERSAL_TIME;
	public static final int PERR_RATELIMIT			= 10;
	public static final int RREQ_RETRIES			= 2;
	public static final int RREQ_RATELIMIT			= 10;
	public static final int TIMEOUT_BUFFER			= 2;
	public static final int TTL_START				= 1;
	public static final int TTL_INCREMENT			= 2;
	public static final int TTL_THRESHOLD			= 7;
	public static       int TTL_VALUE				= 1;	// IPヘッダ内の"TTL"フィールドの値 getTimeToLive()？
	public static       int RING_TRAVERSAL_TIME		= 2 * NODE_TRAVERSAL_TIME * (TTL_VALUE + TIMEOUT_BUFFER);
	
	
	
	public static void main(String[] args) throws Exception {
		
        //自分のローカルＩＰアドレスを取得
        InetAddress addr = InetAddress.getLocalHost();
        byte[] myAdd = addr.getAddress();
        
        System.out.println("自身のアドレス:"+addr.getHostAddress());
        
        // 受信 並びに その後の処理を行うスレッドを作成、開始
        (new ConnectionThread()).start();
        
        // 隣接ノードの接続状態を監視、並びに隣接ノードに接続状態を通知するスレッドを作成、開始
        (new helloThread()).start();
        
        /************  ここから受信モード  *************/
    	
        //5100番ポートを監視するUDPソケットを生成
        DatagramSocket receiveSocket = new DatagramSocket(5100);
        	
        // 受け付けるデータバッファとUDPパケットを作成
        byte receiveBuffer[] = new byte[1024];	//受信用配列
             
        DatagramPacket receivePacket = 
           			 new DatagramPacket(receiveBuffer, receiveBuffer.length);
        

        
        	
    label:  while (true) {
            // UDPパケットを受信
            receiveSocket.receive(receivePacket);
           
            
            //前ホップのノードのアドレスを取得
	    	InetAddress cAddr = receivePacket.getAddress();
            
            System.out.println("\n！受信！"); 
            System.out.println("受信サイズ:"+receivePacket.getLength());
            /******** 隣接ノードの更新など **************/
        		            
            // 受信データのタイプ(0バイト目)を識別
        	switch( receiveBuffer[0] ){
        		case 1:	// RREQ
        			
        			// RREQの受信履歴の中で、古すぎるアドレスの削除
        			if( !receiveRREQ_List.isEmpty() ){	// 空でなければ
        				// リストの中で最も古いのは先頭の項目、その生存時間をチェック
        				while( receiveRREQ_List.get(0).lifeTime < new Date().getTime() ){
        					receiveRREQ_List.remove(0);
        				}
        			}
        			
        			// 受信履歴リストの中の情報と、RREQ_ID,送信元が一致すればメッセージを無視
        			if( RREQ_ContainCheck( RReq.getRREQ_ID(receiveBuffer), RReq.getFromIpAdd(receiveBuffer))){
        				System.out.println("重複したRREQメッセージのため無視します\n");
        				continue label;
        			}
        			
        			// ホップ数+1,
        			receiveBuffer[3]++;
        			
        			// RREQの内容を出力（完成時には削除？)
        			System.out.println("このメッセージはRREQです"); 
        			System.out.print("　ホップ数:"+RReq.getHopCount(receiveBuffer)+"回");
        			System.out.println(",RREQ_ID:"+RReq.getRREQ_ID(receiveBuffer));
        			System.out.print("　宛先\""+InetAddress.getByAddress(RReq.getToIpAdd(receiveBuffer))+"\"");
        			System.out.println(",シーケンス番号"+RReq.getToSeqNum(receiveBuffer));
        			System.out.print("　送信元\""+InetAddress.getByAddress(RReq.getFromIpAdd(receiveBuffer))+"\"");
        			System.out.println(",シーケンス番号:"+RReq.getFromSeqNum(receiveBuffer));
        			
        			System.out.println("　自ノードのアドレス："+InetAddress.getLocalHost().getHostAddress());
        			System.out.println("　前ホップノードのアドレス:" + cAddr.getHostAddress());
        			

        			
        			// 受信したRREQメッセージの情報をListに追加
        			// 引数はRREQ_ID,送信元アドレス
        			newPastRReq(RReq.getRREQ_ID(receiveBuffer),RReq.getFromIpAdd(receiveBuffer));
        			
        			
        			// 逆経路を、生存時間短めで記録
        			// 生存時間は、既に経路があるならその生存時間をそのまま、またはMinimalLifeTimeの大きいほうにセット
        			long life;
        			int index;
        			long MinimalLifeTime = new Date().getTime()+ 2*NET_TRAVERSAL_TIME -2*RReq.getHopCount(receiveBuffer)*NODE_TRAVERSAL_TIME;
        			
        			if( (index = searchToAdd(list, RReq.getFromIpAdd(receiveBuffer))) != -1){
        				life = (list.get(index).lifeTime > MinimalLifeTime)? list.get(index).lifeTime:MinimalLifeTime; 
        			}
        			else{
        				life = MinimalLifeTime;
        			}
        			
        			// シーケンス番号も同様に、既存経路の値と受信RREQ中の値を比較して高いほう
        			// また、既存経路のほうが高い場合、受信したRREQの値をそちらをコピー
        			if(index != -1){
        				if(list.get(index).toSeqNum > RReq.getFromSeqNum(receiveBuffer)){
            				receiveBuffer = RReq.setFromSeqNum(receiveBuffer, list.get(index).toSeqNum);
        				}
        			}

        			// 既に逆経路があるなら上書きset、なければ追加add
        			if(index != -1){
        				list.set(index, new RouteTable( RReq.getFromIpAdd(receiveBuffer), RReq.getFromSeqNum(receiveBuffer)
            					, true, (byte)1, RReq.getHopCount(receiveBuffer), cAddr.getAddress(), life
            					, new HashSet<Precursor>() ));
        			}
        			else{
	        			list.add( new RouteTable( RReq.getFromIpAdd(receiveBuffer), RReq.getFromSeqNum(receiveBuffer)
	        					, true, (byte)1, RReq.getHopCount(receiveBuffer), cAddr.getAddress(), life
	        					, new HashSet<Precursor>() ));
        			}
        			
        			// RREQメッセージの内容チェック ########
        			if(RReq.isToMe(receiveBuffer)){
        				System.out.println("RREQ:自分宛のメッセージです");
        				System.out.println("RREPを前ホップノード" + cAddr.getHostAddress() + "にユニキャストします");
        				
        				// 返信前に、シーケンス番号の更新
        				if( RReq.getRREQ_ID(receiveBuffer) == seqNum+1){
        					seqNum++;
        				}
        				
        				// RREPの返信	        				
        				RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer),InetAddress.getLocalHost()
        						,(byte)0 ,seqNum , MY_ROUTE_TIMEOUT);
        				
        				
        			}
        			else{
        				System.out.println("RREQ:自分宛のメッセージではありません");
        				
        				// 宛先までの有効経路を持っているか検索
        				index = searchToAdd(list, RReq.getToIpAdd(receiveBuffer));
        				
        				if( index != -1){
        					if( list.get(index).stateFlag == 1
        							&& list.get(index).toSeqNum > RReq.getToSeqNum(receiveBuffer)
        							&& !RReq.getFlagD(receiveBuffer)){

	        					// 経路を既に知っているため、RREPを返信
	        					System.out.println("経路が既知な中間ノードなので、RREPを返信");
	        					
	        					// 経路のprecursorListに追加
	        					// まず順経路の更新
	        					// 順経路のprecursorListに、逆経路の次ホップを追加。エラー時にRERRを伝えるノード
	        					// すでにListに含まれていても、HashSetにより重複は認められないのでOK
	        					RouteTable route = list.get(index);		// 一旦リストから出す
	        					route.preList.add(new Precursor(cAddr.getAddress()));	// 書き加え
	        					list.set(index, route);		// リストに上書き
	        					
	        					// 次に逆経路の更新
	        					// 逆経路のprecursorListに、順経路の次ホップを追加。エラー時にRERRを伝えるノード
	        					int index2 = searchToAdd(list, RReq.getFromIpAdd(receiveBuffer));
	        					route = list.get(index2);
	        					route.preList.add(new Precursor(list.get(index).nextIpAdd));
	        					list.set(index2, route);
	        					
	        					// RREPの返信
	        					RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer),
	        							InetAddress.getByAddress(RReq.getToIpAdd(receiveBuffer)),
	        							list.get(index).hopCount, list.get(index).toSeqNum,
	        							(int)(list.get(index).lifeTime - new Date().getTime()));
	        					
	        					continue label;
        					}
        				}
        				// (else 上記if(true)のときはcontinueで通らない)
        				// =宛先までの有効経路を持っていない場合
        				{
        					// TTLを減らすタイミングはこの後にしているので、比較は1以下
        					// 順番変えても別にいい気もする
        					if(RReq.getTimeToLive(receiveBuffer)<=1){
        						System.out.println("TTLが0以下なので転送しません");
        					}
        					else{
        						// 条件を満たせば、中継するためブロードキャスト
        						// 引数のTTL--;
        						receiveBuffer = RReq.setTimeToLive(receiveBuffer, RReq.getTimeToLive(receiveBuffer)-1);
        						
        						RReq.send2(receiveBuffer);
        					}
        				}
        			}
        			break;
        	
        			
        		case 2:	//RREPを受け取った場合
        			// 受信データのサイズ
        			int mesLength = receivePacket.getLength();
        			
        			// ホップ数++
        			receiveBuffer = RRep.hopCountInc(receiveBuffer, mesLength);
        			
        			System.out.println("　このメッセージはRREPです\n");
        			System.out.println("　ホップ数\t:"+RRep.getHopCount(receiveBuffer,mesLength));
        			System.out.println("　宛先IPアドレス:"+InetAddress.getByAddress(RRep.getToIpAdd(receiveBuffer,mesLength)));
        			System.out.println("　宛先順序番号:"+RRep.getToSeqNum(receiveBuffer,mesLength));
        			System.out.println("　送信元IPアドレス"+InetAddress.getByAddress(RRep.getFromIpAdd(receiveBuffer,mesLength)));
        			System.out.println("　自ノードのアドレス："+InetAddress.getLocalHost().getHostAddress());
        			System.out.println("　前ホップノードのアドレス:" + cAddr.getHostAddress());
        			
        			// 順経路（RREQ送信元⇒宛先）が存在するかどうか検索
    				int index2 = searchToAdd(list, RRep.getToIpAdd(receiveBuffer,mesLength));
    				
    				// 存在しない場合、順経路の作成
    				if( index2 == -1 ){
	        			list.add( new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<Precursor>() ));    					
    				}
    				// 順経路が存在する場合
    				else{
    					// 以下のいずれかの条件を満たしている場合、経路を更新する
    					// 1.既存経路のシーケンス番号が無効であると記録されている
    					// 2.RREPの宛先シーケンス番号＞既存経路の番号であり、有効
    					// 3.シーケンス番号が等しく既存経路が無効である
    					// 4.シーケンス番号が等しくホップ数が既存経路よりも小さい
    					if(	(list.get(index2).validToSeqNumFlag == false)
    						||(RRep.getHopCount(receiveBuffer, mesLength) > list.get(index2).toSeqNum)
    						||( (list.get(index2).stateFlag != 1)
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == list.get(index2).toSeqNum))
    						||( (RRep.getHopCount(receiveBuffer, mesLength) < list.get(index2).hopCount) 
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == list.get(index2).toSeqNum)))
    					{
    						// 順経路の上書き
    						list.set(index2, new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<Precursor>() ));
    					}
    				}
        			
        			if(RRep.isToMe(receiveBuffer,mesLength)){
        				System.out.println("RREP:送信元ＩＰアドレスが自分です");
        				
        				System.out.println("通信を開始します");      				        				
        				/*** 通信開始 ***/
        			}
        			else{
        				System.out.println("RREP:送信元ＩＰアドレスが自分ではありません");
        				
        				// 順経路を示すindex2を再検索、更新
        				index2 = searchToAdd(list, RRep.getToIpAdd(receiveBuffer,mesLength));
        				// RREQ生成ノードへの経路（逆経路）を検索
        				int index3 = searchToAdd(list, RRep.getFromIpAdd(receiveBuffer,mesLength));
        				
        				// 順経路のPrecursorListに、逆経路への次ホップを追加
    					// すでにListに含まれていても、HashSetにより重複は認められないのでOK
    					RouteTable route = list.get(index2);		// 一旦リストから出す
    					route.preList.add(new Precursor(list.get(index3).nextIpAdd));	// 書き加え
    					list.set(index2, route);					// リストに上書き
    					
    					// 逆経路のPrecursorListに、順経路の次ホップを追加
    					route = list.get(index3);					// 一旦リストから出す
    					route.preList.add(new Precursor(list.get(index2).nextIpAdd));	// 書き加え
    					list.set(index3, route);					// リストに上書き
        				
        				System.out.println("RREPを転送します");
        				// RREPを前ホップノードに転送
        				RRep.reply2(receiveBuffer, InetAddress.getByAddress(list.get(index3).nextIpAdd));
        			
        			}
        			break;
        			
        			
        	}
        }
	}
	
	
	public static class RouteTable{
		
		/************** フィールドの要素 ***********************/
		
		byte[] toIpAdd;	// 宛先IPアドレス
		int toSeqNum;	// 宛先シーケンス番号
		boolean validToSeqNumFlag;	// 有効宛先シーケンス番号フラグ
		byte stateFlag;		// 他の状態フラグ（有効1,無効2,修復可能3,修復中4)
		
		// ### ネットワークインタフェース ###
		// ？？？
		
		byte hopCount;		// ホップ数
		byte[] nextIpAdd;	// 次ホップのIPアドレス
		long lifeTime;		// 生存時間
		
		// 宛先に対応した逆経路のリスト,RERRの送信先
		HashSet<Precursor> preList = new HashSet<Precursor>();
		
		/************** フィールドの要素ここまで ***************/
		
		// new用、list.add(new RouteTable(上記フィールドを埋める引数));で要素を追加できる
		public RouteTable(byte[] IP,int num,boolean numFlag,byte state,
					byte hopNum,byte[] nextAdd,long time,HashSet<Precursor> list){
			toIpAdd = IP;
			toSeqNum = num;
			validToSeqNumFlag = numFlag;
			stateFlag = state;
			hopCount = hopNum;
			nextIpAdd = nextAdd;
			lifeTime = time;
			preList = list;
		}
	}
	
	// RouteTable(list)に宛先アドレス(Add)が含まれていないか検索する
	// 戻り値：リスト内で発見した位置、インデックス
	//         見つからない場合 -1を返す
	public static int searchToAdd(ArrayList<RouteTable> list,byte[] Add){
		
		
		for(int i=0;i<list.size();i++){
			if( Arrays.equals((list.get(i).toIpAdd) , Add)){return i;}
		}
		
		return -1;
	}
	
	// RERRの転送先を保持するPrecursorListの元
	public static class Precursor{
		
		// 変数はアドレスだけ
		byte[] IpAdd;	// 次ホップの経路切断時にエラーを送るノード（逆経路？）
		
		public Precursor(byte[] Add){
			IpAdd = Add;
		}
	}
	
	// RREQを重複して受信しないよう、短い間に受信したRREQIDとアドレスを記録
	public static class pastData{
		
		int RREQ_ID;
		byte[] IpAdd;
		long lifeTime;
		
		public pastData(int ID,byte[] Add,long life){
			RREQ_ID = ID;
			IpAdd = Add;
			lifeTime = life;
		}
	}
	
	// 短い間のRREQ受信履歴中に、引数のID,アドレスのものが無いか検索
	public static boolean RREQ_ContainCheck(int ID,byte[] Add){
		
		for(int i=0;i<receiveRREQ_List.size();i++){
			if( (ID == receiveRREQ_List.get(i).RREQ_ID) 
					&& Arrays.equals(Add, receiveRREQ_List.get(i).IpAdd)){
				return true;
			}
		}
		return false;
	}
	
	// 同時に参照が起こらないよう、リストに追加するメソッド
	public static synchronized void newPastRReq(int IDnum,byte[] FromIpAdd){
		receiveRREQ_List.add( new pastData( IDnum, FromIpAdd,
											(new Date().getTime())+PATH_DISCOVERY_TIME ));
	}
}

class ConnectionThread extends Thread{
	
	
	public void run(){
		
		try{
			// 入力用
	        String toIPAddress;
	        
	        // 送信用ループ
	        while(!Test.flag){
	        	
	        	System.out.print("キーボードから宛先アドレスを入力(終了:exit)\n");
	        	
	        	//BufferedReaderクラスのインスタンス化
	        	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        	
	        	//入力された文字を変数に代入
	        	toIPAddress = br.readLine();
	        	
	        	if( "exit".equals(toIPAddress) ){
	        		Test.flag = true;
	        	}
	        	else{
	        		// 宛先までの経路が既にルートテーブル内に有り、有効かチェック
	        		byte[] byteAdd = Test.RReq.getByteAddress(toIPAddress);
	        		int index = Test.searchToAdd(Test.list ,byteAdd);
	        		
	        		// 宛先があり、有効状態(1)であり、生存しているか？
	        		boolean effectiveFlag = false;
	        		if(index != -1){
	        			if((Test.list.get(index).stateFlag==1)
		        				&& (Test.list.get(index).lifeTime > new Date().getTime() )){
	        				effectiveFlag = true;	// 有効
	        			}
	        		}
	        		
	        		if(effectiveFlag){
	        			// 経路が存在するばあい
	        			System.out.println("宛先までの経路は既に有効です");
	        		}
	        		else{
	        			// 存在しない、または無効な場合
	        			
	        			// 自身のシーケンス番号をインクリメント
	        			Test.seqNum++;
	        			
	        			// TTLを初期値または過去のホップ数+TTL_ｲﾝｸﾘﾒﾝﾄにセット
	        			// 宛先シーケンス番号(+未知フラグ)もまとめてセット
	        			Test.TTL_VALUE = Test.TTL_START;
	        			boolean flagU = true;
	        			int seqValue = 0;
	        			
	        			if(index != -1){
	        				Test.TTL_VALUE = (Test.list.get(index).hopCount) + Test.TTL_INCREMENT;
	        				flagU = false;
	        				seqValue = Test.list.get(index).toSeqNum;
	        			}
	        			
	        			// 再送ループ、最大TTL探索まで再探索を続けるか
	        			// 経路が追加されると終了
	        			for( ;(Test.TTL_VALUE != Test.TTL_THRESHOLD+Test.TTL_INCREMENT)
	        				&& (Test.searchToAdd(Test.list ,byteAdd) == -1)
	        				;Test.TTL_VALUE += Test.TTL_INCREMENT ){
	        				
	        				// 微調整(たとえばインクリメント2,最大TTL7のとき、初期2->4->6->7と、最後だけインクリメントがずれるとき)
	        				if(Test.TTL_VALUE > Test.TTL_THRESHOLD){
	        					Test.TTL_VALUE = Test.TTL_THRESHOLD;
	        				}
	        				
							// RREQ_IDをインクリメント
							Test.RREQ_ID++;
							
		        			// 自分でループしないようにRREQ_IDなどを登録
		        			Test.newPastRReq(Test.RREQ_ID, InetAddress.getLocalHost().getAddress());
							
							try {	/* 送信 */
									Test.do_BroadCast = true;
									Test.RReq.send(byteAdd, false, false,
											false, false, flagU, seqValue,
											Test.seqNum, Test.RREQ_ID, Test.TTL_VALUE);
							} catch (Exception e) {
								System.out.println("RREQ送信エラー："+e);
							}
							
							// ### ちょっと強引な待機(本来はRREPが戻ってくれば待たなくていい時間も待っている) ###
							// 待ち時間をVALUEに合わせて更新
							Test.RING_TRAVERSAL_TIME = 2 * Test.NODE_TRAVERSAL_TIME * (Test.TTL_VALUE + Test.TIMEOUT_BUFFER);
							Thread.sleep(Test.RING_TRAVERSAL_TIME);
						}
	        		}
	        	}
	        }
		}catch(Exception e){
			System.out.println(e);
		}
	}
}

// 一定時間ごとにHELLOメッセージの送信と、既存経路の生存時間をチェック
// 接続切れがあった場合、RERRをPrecursorに送信する
// ついでに、DELETE時間がたった経路を削除する
class helloThread extends Thread{
	
	Test.RouteTable route;
	
	public void run(){
		
		try{
				// HELLO_INTERVALごとに送信と経路チェック
				while(!Test.flag){
					// 経路が存在するならば
					if(!Test.list.isEmpty()){
						
						// HELLOメッセージの送信
						// 一定時間内にブロードキャストしていれば、送信しない
						if( !Test.do_BroadCast )
							Test.RRep.send(Test.seqNum, Test.ALLOWED_HELLO_LOSS * Test.HELLO_INTERVAL);
						
						// 既存経路の生存時間チェック
						for(int i=0;i<Test.list.size();i++){
							
							route = Test.list.get(i);
							
							// 無効経路が削除される時間であるか
							if( route.lifeTime+Test.DELETE_PERIOD < new Date().getTime()){
								
								// 無効経路の削除
								Test.list.remove(i);
								
								// 削除によるindexのズレを修正
								i--;
							}
							
							// 有効経路が無効経路になる時間（寿命）であるか
							else if( (route.stateFlag==1) && (route.lifeTime < new Date().getTime())){
								
								// precursorListの全アドレスにRERRの送信
								Iterator<Test.Precursor> it = route.preList.iterator();
								for(int j=0;j<route.preList.size();j++,it.hasNext()){
									
									byte[] atesaki = ((Test.Precursor)it.next()).IpAdd;
									
									// RERRを各ノードに向けてユニキャスト
									Test.RErr.send(false, route.nextIpAdd, route.toSeqNum, atesaki);
								}
							}
						}
					}
					
					Test.do_BroadCast = false;			// リセット
					Thread.sleep(Test.HELLO_INTERVAL);	// 処理時間の無視？
				}
		}catch(Exception e){
			System.out.println(e);
		}
	}
}
		
		