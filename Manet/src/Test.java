import java.net.*;
import java.io.*;
import java.util.*;

import aodv.*;



public class Test {
	
	public static RREQ RReq = new RREQ();
	public static RREP RRep = new RREP();
	public static RERR RErr = new RERR();
	
	// ���[�g�e�[�u��
	public static ArrayList<RouteTable> list = new ArrayList<RouteTable>();
	
	// PATH_DISCOVERY_TIME�̊ԂɎ�M����RREQ�̑��M����ID���L�^
	public static ArrayList<pastData> receiveRREQ_List = new ArrayList<pastData>();
	
	// ���̑��ϐ�
	public static int RREQ_ID = 0;
	public static int seqNum  = 0;
	public static boolean do_BroadCast = false;		// ��莞�ԓ��ɉ�����۰�޷��Ă������ǂ���
	
	//	�I���t���O(��M�X���b�h�I�����ɑ��̃X���b�h���I��)
	public static boolean flag =false;
	
	// �p�����[�^�̃f�t�H���g�l��錾
	
	public static final int ACTIVE_ROUTE_TIMEOUT	= 3000; //[ms]
	public static final int ALLOWED_HELLO_LOSS		= 2;
	public static final int HELLO_INTERVAL			= 1000; //[ms]
	public static final int DELETE_PERIOD			= (ACTIVE_ROUTE_TIMEOUT >= HELLO_INTERVAL)?
										5*ACTIVE_ROUTE_TIMEOUT : 5*HELLO_INTERVAL;
	public static final int LOCAL_ADD_TTL			= 2;
	public static final int MY_ROUTE_TIMEOUT		= 2 * ACTIVE_ROUTE_TIMEOUT;
	public static final int NET_DIAMETER			= 35;
	public static final int MAX_REPAIR_TTL			= (int)(0.3 * NET_DIAMETER);
	public static       int MIN_REPAIR_TTL			= -1;	// ����m�[�h�֒m���Ă���ŐV�̃z�b�v��
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
	public static       int TTL_VALUE				= 1;	// IP�w�b�_����"TTL"�t�B�[���h�̒l getTimeToLive()�H
	public static       int RING_TRAVERSAL_TIME		= 2 * NODE_TRAVERSAL_TIME * (TTL_VALUE + TIMEOUT_BUFFER);
	
	
	
	public static void main(String[] args) throws Exception {
		
        //�����̃��[�J���h�o�A�h���X���擾
        InetAddress addr = InetAddress.getLocalHost();
        byte[] myAdd = addr.getAddress();
        
        System.out.println("���g�̃A�h���X:"+addr.getHostAddress());
        
        // ��M ���т� ���̌�̏������s���X���b�h���쐬�A�J�n
        (new ConnectionThread()).start();
        
        // �אڃm�[�h�̐ڑ���Ԃ��Ď��A���тɗאڃm�[�h�ɐڑ���Ԃ�ʒm����X���b�h���쐬�A�J�n
        (new helloThread()).start();
        
        /************  ���������M���[�h  *************/
    	
        //5100�ԃ|�[�g���Ď�����UDP�\�P�b�g�𐶐�
        DatagramSocket receiveSocket = new DatagramSocket(5100);
        	
        // �󂯕t����f�[�^�o�b�t�@��UDP�p�P�b�g���쐬
        byte receiveBuffer[] = new byte[1024];	//��M�p�z��
             
        DatagramPacket receivePacket = 
           			 new DatagramPacket(receiveBuffer, receiveBuffer.length);
        

        
        	
    label:  while (true) {
            // UDP�p�P�b�g����M
            receiveSocket.receive(receivePacket);
           
            
            //�O�z�b�v�̃m�[�h�̃A�h���X���擾
	    	InetAddress cAddr = receivePacket.getAddress();
            
            System.out.println("\n�I��M�I"); 
            System.out.println("��M�T�C�Y:"+receivePacket.getLength());
            /******** �אڃm�[�h�̍X�V�Ȃ� **************/
        		            
            // ��M�f�[�^�̃^�C�v(0�o�C�g��)������
        	switch( receiveBuffer[0] ){
        		case 1:	// RREQ
        			
        			// RREQ�̎�M�����̒��ŁA�Â�����A�h���X�̍폜
        			if( !receiveRREQ_List.isEmpty() ){	// ��łȂ����
        				// ���X�g�̒��ōł��Â��̂͐擪�̍��ځA���̐������Ԃ��`�F�b�N
        				while( receiveRREQ_List.get(0).lifeTime < new Date().getTime() ){
        					receiveRREQ_List.remove(0);
        				}
        			}
        			
        			// ��M�������X�g�̒��̏��ƁARREQ_ID,���M������v����΃��b�Z�[�W�𖳎�
        			if( RREQ_ContainCheck( RReq.getRREQ_ID(receiveBuffer), RReq.getFromIpAdd(receiveBuffer))){
        				System.out.println("�d������RREQ���b�Z�[�W�̂��ߖ������܂�\n");
        				continue label;
        			}
        			
        			// �z�b�v��+1,
        			receiveBuffer[3]++;
        			
        			// RREQ�̓��e���o�́i�������ɂ͍폜�H)
        			System.out.println("���̃��b�Z�[�W��RREQ�ł�"); 
        			System.out.print("�@�z�b�v��:"+RReq.getHopCount(receiveBuffer)+"��");
        			System.out.println(",RREQ_ID:"+RReq.getRREQ_ID(receiveBuffer));
        			System.out.print("�@����\""+InetAddress.getByAddress(RReq.getToIpAdd(receiveBuffer))+"\"");
        			System.out.println(",�V�[�P���X�ԍ�"+RReq.getToSeqNum(receiveBuffer));
        			System.out.print("�@���M��\""+InetAddress.getByAddress(RReq.getFromIpAdd(receiveBuffer))+"\"");
        			System.out.println(",�V�[�P���X�ԍ�:"+RReq.getFromSeqNum(receiveBuffer));
        			
        			System.out.println("�@���m�[�h�̃A�h���X�F"+InetAddress.getLocalHost().getHostAddress());
        			System.out.println("�@�O�z�b�v�m�[�h�̃A�h���X:" + cAddr.getHostAddress());
        			

        			
        			// ��M����RREQ���b�Z�[�W�̏���List�ɒǉ�
        			// ������RREQ_ID,���M���A�h���X
        			newPastRReq(RReq.getRREQ_ID(receiveBuffer),RReq.getFromIpAdd(receiveBuffer));
        			
        			
        			// �t�o�H���A�������ԒZ�߂ŋL�^
        			// �������Ԃ́A���Ɍo�H������Ȃ炻�̐������Ԃ����̂܂܁A�܂���MinimalLifeTime�̑傫���ق��ɃZ�b�g
        			long life;
        			int index;
        			long MinimalLifeTime = new Date().getTime()+ 2*NET_TRAVERSAL_TIME -2*RReq.getHopCount(receiveBuffer)*NODE_TRAVERSAL_TIME;
        			
        			if( (index = searchToAdd(list, RReq.getFromIpAdd(receiveBuffer))) != -1){
        				life = (list.get(index).lifeTime > MinimalLifeTime)? list.get(index).lifeTime:MinimalLifeTime; 
        			}
        			else{
        				life = MinimalLifeTime;
        			}
        			
        			// �V�[�P���X�ԍ������l�ɁA�����o�H�̒l�Ǝ�MRREQ���̒l���r���č����ق�
        			// �܂��A�����o�H�̂ق��������ꍇ�A��M����RREQ�̒l����������R�s�[
        			if(index != -1){
        				if(list.get(index).toSeqNum > RReq.getFromSeqNum(receiveBuffer)){
            				receiveBuffer = RReq.setFromSeqNum(receiveBuffer, list.get(index).toSeqNum);
        				}
        			}

        			// ���ɋt�o�H������Ȃ�㏑��set�A�Ȃ���Βǉ�add
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
        			
        			// RREQ���b�Z�[�W�̓��e�`�F�b�N ########
        			if(RReq.isToMe(receiveBuffer)){
        				System.out.println("RREQ:�������̃��b�Z�[�W�ł�");
        				System.out.println("RREP��O�z�b�v�m�[�h" + cAddr.getHostAddress() + "�Ƀ��j�L���X�g���܂�");
        				
        				// �ԐM�O�ɁA�V�[�P���X�ԍ��̍X�V
        				if( RReq.getRREQ_ID(receiveBuffer) == seqNum+1){
        					seqNum++;
        				}
        				
        				// RREP�̕ԐM	        				
        				RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer),InetAddress.getLocalHost()
        						,(byte)0 ,seqNum , MY_ROUTE_TIMEOUT);
        				
        				
        			}
        			else{
        				System.out.println("RREQ:�������̃��b�Z�[�W�ł͂���܂���");
        				
        				// ����܂ł̗L���o�H�������Ă��邩����
        				index = searchToAdd(list, RReq.getToIpAdd(receiveBuffer));
        				
        				if( index != -1){
        					if( list.get(index).stateFlag == 1
        							&& list.get(index).toSeqNum > RReq.getToSeqNum(receiveBuffer)
        							&& !RReq.getFlagD(receiveBuffer)){

	        					// �o�H�����ɒm���Ă��邽�߁ARREP��ԐM
	        					System.out.println("�o�H�����m�Ȓ��ԃm�[�h�Ȃ̂ŁARREP��ԐM");
	        					
	        					// �o�H��precursorList�ɒǉ�
	        					// �܂����o�H�̍X�V
	        					// ���o�H��precursorList�ɁA�t�o�H�̎��z�b�v��ǉ��B�G���[����RERR��`����m�[�h
	        					// ���ł�List�Ɋ܂܂�Ă��Ă��AHashSet�ɂ��d���͔F�߂��Ȃ��̂�OK
	        					RouteTable route = list.get(index);		// ��U���X�g����o��
	        					route.preList.add(new Precursor(cAddr.getAddress()));	// ��������
	        					list.set(index, route);		// ���X�g�ɏ㏑��
	        					
	        					// ���ɋt�o�H�̍X�V
	        					// �t�o�H��precursorList�ɁA���o�H�̎��z�b�v��ǉ��B�G���[����RERR��`����m�[�h
	        					int index2 = searchToAdd(list, RReq.getFromIpAdd(receiveBuffer));
	        					route = list.get(index2);
	        					route.preList.add(new Precursor(list.get(index).nextIpAdd));
	        					list.set(index2, route);
	        					
	        					// RREP�̕ԐM
	        					RRep.reply(cAddr, RReq.getFromIpAdd(receiveBuffer),
	        							InetAddress.getByAddress(RReq.getToIpAdd(receiveBuffer)),
	        							list.get(index).hopCount, list.get(index).toSeqNum,
	        							(int)(list.get(index).lifeTime - new Date().getTime()));
	        					
	        					continue label;
        					}
        				}
        				// (else ��Lif(true)�̂Ƃ���continue�Œʂ�Ȃ�)
        				// =����܂ł̗L���o�H�������Ă��Ȃ��ꍇ
        				{
        					// TTL�����炷�^�C�~���O�͂��̌�ɂ��Ă���̂ŁA��r��1�ȉ�
        					// ���ԕς��Ă��ʂɂ����C������
        					if(RReq.getTimeToLive(receiveBuffer)<=1){
        						System.out.println("TTL��0�ȉ��Ȃ̂œ]�����܂���");
        					}
        					else{
        						// �����𖞂����΁A���p���邽�߃u���[�h�L���X�g
        						// ������TTL--;
        						receiveBuffer = RReq.setTimeToLive(receiveBuffer, RReq.getTimeToLive(receiveBuffer)-1);
        						
        						RReq.send2(receiveBuffer);
        					}
        				}
        			}
        			break;
        	
        			
        		case 2:	//RREP���󂯎�����ꍇ
        			// ��M�f�[�^�̃T�C�Y
        			int mesLength = receivePacket.getLength();
        			
        			// �z�b�v��++
        			receiveBuffer = RRep.hopCountInc(receiveBuffer, mesLength);
        			
        			System.out.println("�@���̃��b�Z�[�W��RREP�ł�\n");
        			System.out.println("�@�z�b�v��\t:"+RRep.getHopCount(receiveBuffer,mesLength));
        			System.out.println("�@����IP�A�h���X:"+InetAddress.getByAddress(RRep.getToIpAdd(receiveBuffer,mesLength)));
        			System.out.println("�@���揇���ԍ�:"+RRep.getToSeqNum(receiveBuffer,mesLength));
        			System.out.println("�@���M��IP�A�h���X"+InetAddress.getByAddress(RRep.getFromIpAdd(receiveBuffer,mesLength)));
        			System.out.println("�@���m�[�h�̃A�h���X�F"+InetAddress.getLocalHost().getHostAddress());
        			System.out.println("�@�O�z�b�v�m�[�h�̃A�h���X:" + cAddr.getHostAddress());
        			
        			// ���o�H�iRREQ���M���ˈ���j�����݂��邩�ǂ�������
    				int index2 = searchToAdd(list, RRep.getToIpAdd(receiveBuffer,mesLength));
    				
    				// ���݂��Ȃ��ꍇ�A���o�H�̍쐬
    				if( index2 == -1 ){
	        			list.add( new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<Precursor>() ));    					
    				}
    				// ���o�H�����݂���ꍇ
    				else{
    					// �ȉ��̂����ꂩ�̏����𖞂����Ă���ꍇ�A�o�H���X�V����
    					// 1.�����o�H�̃V�[�P���X�ԍ��������ł���ƋL�^����Ă���
    					// 2.RREP�̈���V�[�P���X�ԍ��������o�H�̔ԍ��ł���A�L��
    					// 3.�V�[�P���X�ԍ��������������o�H�������ł���
    					// 4.�V�[�P���X�ԍ����������z�b�v���������o�H����������
    					if(	(list.get(index2).validToSeqNumFlag == false)
    						||(RRep.getHopCount(receiveBuffer, mesLength) > list.get(index2).toSeqNum)
    						||( (list.get(index2).stateFlag != 1)
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == list.get(index2).toSeqNum))
    						||( (RRep.getHopCount(receiveBuffer, mesLength) < list.get(index2).hopCount) 
    								&&(RRep.getHopCount(receiveBuffer, mesLength) == list.get(index2).toSeqNum)))
    					{
    						// ���o�H�̏㏑��
    						list.set(index2, new RouteTable( RRep.getToIpAdd(receiveBuffer,mesLength), RRep.getToSeqNum(receiveBuffer,mesLength)
	        					, true, (byte)1, RRep.getHopCount(receiveBuffer,mesLength), cAddr.getAddress()
	        					, RRep.getLifeTime(receiveBuffer,mesLength) + (new Date().getTime())
	        					, new HashSet<Precursor>() ));
    					}
    				}
        			
        			if(RRep.isToMe(receiveBuffer,mesLength)){
        				System.out.println("RREP:���M���h�o�A�h���X�������ł�");
        				
        				System.out.println("�ʐM���J�n���܂�");      				        				
        				/*** �ʐM�J�n ***/
        			}
        			else{
        				System.out.println("RREP:���M���h�o�A�h���X�������ł͂���܂���");
        				
        				// ���o�H������index2���Č����A�X�V
        				index2 = searchToAdd(list, RRep.getToIpAdd(receiveBuffer,mesLength));
        				// RREQ�����m�[�h�ւ̌o�H�i�t�o�H�j������
        				int index3 = searchToAdd(list, RRep.getFromIpAdd(receiveBuffer,mesLength));
        				
        				// ���o�H��PrecursorList�ɁA�t�o�H�ւ̎��z�b�v��ǉ�
    					// ���ł�List�Ɋ܂܂�Ă��Ă��AHashSet�ɂ��d���͔F�߂��Ȃ��̂�OK
    					RouteTable route = list.get(index2);		// ��U���X�g����o��
    					route.preList.add(new Precursor(list.get(index3).nextIpAdd));	// ��������
    					list.set(index2, route);					// ���X�g�ɏ㏑��
    					
    					// �t�o�H��PrecursorList�ɁA���o�H�̎��z�b�v��ǉ�
    					route = list.get(index3);					// ��U���X�g����o��
    					route.preList.add(new Precursor(list.get(index2).nextIpAdd));	// ��������
    					list.set(index3, route);					// ���X�g�ɏ㏑��
        				
        				System.out.println("RREP��]�����܂�");
        				// RREP��O�z�b�v�m�[�h�ɓ]��
        				RRep.reply2(receiveBuffer, InetAddress.getByAddress(list.get(index3).nextIpAdd));
        			
        			}
        			break;
        			
        			
        	}
        }
	}
	
	
	public static class RouteTable{
		
		/************** �t�B�[���h�̗v�f ***********************/
		
		byte[] toIpAdd;	// ����IP�A�h���X
		int toSeqNum;	// ����V�[�P���X�ԍ�
		boolean validToSeqNumFlag;	// �L������V�[�P���X�ԍ��t���O
		byte stateFlag;		// ���̏�ԃt���O�i�L��1,����2,�C���\3,�C����4)
		
		// ### �l�b�g���[�N�C���^�t�F�[�X ###
		// �H�H�H
		
		byte hopCount;		// �z�b�v��
		byte[] nextIpAdd;	// ���z�b�v��IP�A�h���X
		long lifeTime;		// ��������
		
		// ����ɑΉ������t�o�H�̃��X�g,RERR�̑��M��
		HashSet<Precursor> preList = new HashSet<Precursor>();
		
		/************** �t�B�[���h�̗v�f�����܂� ***************/
		
		// new�p�Alist.add(new RouteTable(��L�t�B�[���h�𖄂߂����));�ŗv�f��ǉ��ł���
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
	
	// RouteTable(list)�Ɉ���A�h���X(Add)���܂܂�Ă��Ȃ�����������
	// �߂�l�F���X�g���Ŕ��������ʒu�A�C���f�b�N�X
	//         ������Ȃ��ꍇ -1��Ԃ�
	public static int searchToAdd(ArrayList<RouteTable> list,byte[] Add){
		
		
		for(int i=0;i<list.size();i++){
			if( Arrays.equals((list.get(i).toIpAdd) , Add)){return i;}
		}
		
		return -1;
	}
	
	// RERR�̓]�����ێ�����PrecursorList�̌�
	public static class Precursor{
		
		// �ϐ��̓A�h���X����
		byte[] IpAdd;	// ���z�b�v�̌o�H�ؒf���ɃG���[�𑗂�m�[�h�i�t�o�H�H�j
		
		public Precursor(byte[] Add){
			IpAdd = Add;
		}
	}
	
	// RREQ���d�����Ď�M���Ȃ��悤�A�Z���ԂɎ�M����RREQID�ƃA�h���X���L�^
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
	
	// �Z���Ԃ�RREQ��M���𒆂ɁA������ID,�A�h���X�̂��̂�����������
	public static boolean RREQ_ContainCheck(int ID,byte[] Add){
		
		for(int i=0;i<receiveRREQ_List.size();i++){
			if( (ID == receiveRREQ_List.get(i).RREQ_ID) 
					&& Arrays.equals(Add, receiveRREQ_List.get(i).IpAdd)){
				return true;
			}
		}
		return false;
	}
	
	// �����ɎQ�Ƃ��N����Ȃ��悤�A���X�g�ɒǉ����郁�\�b�h
	public static synchronized void newPastRReq(int IDnum,byte[] FromIpAdd){
		receiveRREQ_List.add( new pastData( IDnum, FromIpAdd,
											(new Date().getTime())+PATH_DISCOVERY_TIME ));
	}
}

class ConnectionThread extends Thread{
	
	
	public void run(){
		
		try{
			// ���͗p
	        String toIPAddress;
	        
	        // ���M�p���[�v
	        while(!Test.flag){
	        	
	        	System.out.print("�L�[�{�[�h���父��A�h���X�����(�I��:exit)\n");
	        	
	        	//BufferedReader�N���X�̃C���X�^���X��
	        	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	        	
	        	//���͂��ꂽ������ϐ��ɑ��
	        	toIPAddress = br.readLine();
	        	
	        	if( "exit".equals(toIPAddress) ){
	        		Test.flag = true;
	        	}
	        	else{
	        		// ����܂ł̌o�H�����Ƀ��[�g�e�[�u�����ɗL��A�L�����`�F�b�N
	        		byte[] byteAdd = Test.RReq.getByteAddress(toIPAddress);
	        		int index = Test.searchToAdd(Test.list ,byteAdd);
	        		
	        		// ���悪����A�L�����(1)�ł���A�������Ă��邩�H
	        		boolean effectiveFlag = false;
	        		if(index != -1){
	        			if((Test.list.get(index).stateFlag==1)
		        				&& (Test.list.get(index).lifeTime > new Date().getTime() )){
	        				effectiveFlag = true;	// �L��
	        			}
	        		}
	        		
	        		if(effectiveFlag){
	        			// �o�H�����݂���΂���
	        			System.out.println("����܂ł̌o�H�͊��ɗL���ł�");
	        		}
	        		else{
	        			// ���݂��Ȃ��A�܂��͖����ȏꍇ
	        			
	        			// ���g�̃V�[�P���X�ԍ����C���N�������g
	        			Test.seqNum++;
	        			
	        			// TTL�������l�܂��͉ߋ��̃z�b�v��+TTL_�ݸ���ĂɃZ�b�g
	        			// ����V�[�P���X�ԍ�(+���m�t���O)���܂Ƃ߂ăZ�b�g
	        			Test.TTL_VALUE = Test.TTL_START;
	        			boolean flagU = true;
	        			int seqValue = 0;
	        			
	        			if(index != -1){
	        				Test.TTL_VALUE = (Test.list.get(index).hopCount) + Test.TTL_INCREMENT;
	        				flagU = false;
	        				seqValue = Test.list.get(index).toSeqNum;
	        			}
	        			
	        			// �đ����[�v�A�ő�TTL�T���܂ōĒT���𑱂��邩
	        			// �o�H���ǉ������ƏI��
	        			for( ;(Test.TTL_VALUE != Test.TTL_THRESHOLD+Test.TTL_INCREMENT)
	        				&& (Test.searchToAdd(Test.list ,byteAdd) == -1)
	        				;Test.TTL_VALUE += Test.TTL_INCREMENT ){
	        				
	        				// ������(���Ƃ��΃C���N�������g2,�ő�TTL7�̂Ƃ��A����2->4->6->7�ƁA�Ōゾ���C���N�������g�������Ƃ�)
	        				if(Test.TTL_VALUE > Test.TTL_THRESHOLD){
	        					Test.TTL_VALUE = Test.TTL_THRESHOLD;
	        				}
	        				
							// RREQ_ID���C���N�������g
							Test.RREQ_ID++;
							
		        			// �����Ń��[�v���Ȃ��悤��RREQ_ID�Ȃǂ�o�^
		        			Test.newPastRReq(Test.RREQ_ID, InetAddress.getLocalHost().getAddress());
							
							try {	/* ���M */
									Test.do_BroadCast = true;
									Test.RReq.send(byteAdd, false, false,
											false, false, flagU, seqValue,
											Test.seqNum, Test.RREQ_ID, Test.TTL_VALUE);
							} catch (Exception e) {
								System.out.println("RREQ���M�G���[�F"+e);
							}
							
							// ### ������Ƌ����ȑҋ@(�{����RREP���߂��Ă���Α҂��Ȃ��Ă������Ԃ��҂��Ă���) ###
							// �҂����Ԃ�VALUE�ɍ��킹�čX�V
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

// ��莞�Ԃ��Ƃ�HELLO���b�Z�[�W�̑��M�ƁA�����o�H�̐������Ԃ��`�F�b�N
// �ڑ��؂ꂪ�������ꍇ�ARERR��Precursor�ɑ��M����
// ���łɁADELETE���Ԃ��������o�H���폜����
class helloThread extends Thread{
	
	Test.RouteTable route;
	
	public void run(){
		
		try{
				// HELLO_INTERVAL���Ƃɑ��M�ƌo�H�`�F�b�N
				while(!Test.flag){
					// �o�H�����݂���Ȃ��
					if(!Test.list.isEmpty()){
						
						// HELLO���b�Z�[�W�̑��M
						// ��莞�ԓ��Ƀu���[�h�L���X�g���Ă���΁A���M���Ȃ�
						if( !Test.do_BroadCast )
							Test.RRep.send(Test.seqNum, Test.ALLOWED_HELLO_LOSS * Test.HELLO_INTERVAL);
						
						// �����o�H�̐������ԃ`�F�b�N
						for(int i=0;i<Test.list.size();i++){
							
							route = Test.list.get(i);
							
							// �����o�H���폜����鎞�Ԃł��邩
							if( route.lifeTime+Test.DELETE_PERIOD < new Date().getTime()){
								
								// �����o�H�̍폜
								Test.list.remove(i);
								
								// �폜�ɂ��index�̃Y�����C��
								i--;
							}
							
							// �L���o�H�������o�H�ɂȂ鎞�ԁi�����j�ł��邩
							else if( (route.stateFlag==1) && (route.lifeTime < new Date().getTime())){
								
								// precursorList�̑S�A�h���X��RERR�̑��M
								Iterator<Test.Precursor> it = route.preList.iterator();
								for(int j=0;j<route.preList.size();j++,it.hasNext()){
									
									byte[] atesaki = ((Test.Precursor)it.next()).IpAdd;
									
									// RERR���e�m�[�h�Ɍ����ă��j�L���X�g
									Test.RErr.send(false, route.nextIpAdd, route.toSeqNum, atesaki);
								}
							}
						}
					}
					
					Test.do_BroadCast = false;			// ���Z�b�g
					Thread.sleep(Test.HELLO_INTERVAL);	// �������Ԃ̖����H
				}
		}catch(Exception e){
			System.out.println(e);
		}
	}
}
		
		