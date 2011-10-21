package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Handler;
import android.widget.EditText;



//��莞�Ԃ��Ƃ�HELLO���b�Z�[�W�̑��M�ƁA�����o�H�̐������Ԃ��`�F�b�N
//�ڑ��؂ꂪ�������ꍇ�ARERR��Precursor�ɑ��M����
//���łɁADELETE���Ԃ��������o�H���폜����
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
			
			// �o�H�����݂���Ȃ��
			if(AODV_Activity.routeTable.isEmpty() != true){
				
				int port = Integer.parseInt(textDestPort.getText().toString());
				
				// �o�H���ێ�����HELLO���b�Z�[�W�̑��M
				// ��莞�ԓ��Ƀu���[�h�L���X�g���Ă���Ύ��s���Ȃ�
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
				
				// �����o�H�̐������ԃ`�F�b�N
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					
					route = AODV_Activity.getRoute(i);
					
					// �����o�H���폜����鎞�Ԃł��邩
					if( (route.lifeTime+AODV_Activity.DELETE_PERIOD) < new Date().getTime()){
						
						// �����o�H�̍폜
						AODV_Activity.removeRoute(i);
						
						// �폜�ɂ��i�̃Y�����C��
						i--;
					}
					
					// �L���o�H�������o�H�ɂȂ鎞�ԁi�����j�ł��邩
					else if( (route.stateFlag==1) && (route.lifeTime < new Date().getTime())){
						
						// ������
						route.stateFlag = 2;
						route.lifeTime  = (new Date().getTime()+AODV_Activity.DELETE_PERIOD);
						route.toSeqNum++;
						
						// �㏑��
						AODV_Activity.setRoute(i, route);
						
						// ���[�J�����y�A���s����z�b�v�����H
						if(route.hopCount <= AODV_Activity.MAX_REPAIR_TTL){
							localRepair(route,port,myAddress);
						}
						else{
							// RERR�̑��M
							RERR_Sender(route,port);
						}
					}
				}
			}
			
			AODV_Activity.do_BroadCast = false;
			
			try { // ��莞�Ԃ��ƂɃ��[�v�����A�������Ԃ͖����H
				Thread.sleep(AODV_Activity.HELLO_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}	
		}
	}
	
	// �o�H�̎����C��
	public static void localRepair(RouteTable route, final int port, byte[] myAdd){
		
		// ***** RREQ�̑��M *****
		int TTL = AODV_Activity.MIN_REPAIR_TTL + AODV_Activity.LOCAL_ADD_TTL;
		
		route.toSeqNum++;
		AODV_Activity.RREQ_ID++;
		AODV_Activity.seqNum++;
		
		// ���������M�����p�P�b�g����M���Ȃ��悤��ID��o�^
		AODV_Activity.newPastRReq(AODV_Activity.RREQ_ID, myAdd);
		
		AODV_Activity.do_BroadCast = true;
		
		try {
			new RREQ().send(route.toIpAdd, myAdd
					, false, false, false, false, false
					, route.toSeqNum, AODV_Activity.seqNum, AODV_Activity.RREQ_ID, TTL, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// �o�H�T�����Ԃ��߂�����A�o�H���C�����ꂽ���`�F�b�N
		long waitTime = 2 * AODV_Activity.NODE_TRAVERSAL_TIME * (TTL + AODV_Activity.TIMEOUT_BUFFER);
		final byte[] toIp = route.toIpAdd;
		final RouteTable route_f = route;
		
		Timer mTimer = new Timer(true);
		mTimer.schedule( new TimerTask(){
				@Override
				public void run(){
					int index = AODV_Activity.searchToAdd(toIp);
					
					// �o�H���ǉ�����Ă��āA���z�b�v�����C���O�ȉ��Ȃ�A�C������
					// ����ȊO�̏ꍇ�ARERR�𑗐M����
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
	
	// RERR�̑��M
	public static void RERR_Sender(RouteTable route,int port){
		// precursorList�̑S�A�h���X��RERR�̑��M
		if(route.preList.size()==1){
			// �`����m�[�h��1�����ł���ꍇ
			
			Iterator<byte[]> it = route.preList.iterator();
			byte[] atesaki = it.next();
			
			// RERR�����j�L���X�g
			try {
				new RERR().send(false, route.nextIpAdd, route.toSeqNum, atesaki, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if(route.preList.size()>2){
			// �`����m�[�h�������ł���ꍇ
			
			// RERR���u���[�h�L���X�g
			try {
				new RERR().send(false, route.nextIpAdd, route.toSeqNum, port);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
