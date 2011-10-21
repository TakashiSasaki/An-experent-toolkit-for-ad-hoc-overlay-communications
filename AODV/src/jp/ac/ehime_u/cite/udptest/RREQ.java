package jp.ac.ehime_u.cite.udptest;
/* �����������̃L�[���[�h�u###�v */

import java.net.*;
import java.io.*;
import java.util.*;

public class RREQ {
	// RREQ���b�Z�[�W�̃t�B�[���h	�ȉ�[�t�H�[�}�b�g��̈ʒu(�o�C�g)]�A���
	byte type;		// [0] ���b�Z�[�W�^�C�v
	byte flag;		// [1] �擪5�r�b�g�̂݃t���O(JRGDU)�A�c���0
					//	Join�t���O�F�}���`�L���X�g�p
					//	Repair�t���O�G�}���`�L���X�g�p
					//	Gratuitous RREP�t���O : ����IP�A�h���X�t�B�[���h�ɂ���Ďw�肳�ꂽ�m�[�h�ցAGratuitous RREP�����j�L���X�g���邩�ǂ���������
					//	Destination Only�t���O : ����m�[�h����������RREQ�ɑ΂��ĕԐM���邱�Ƃ�����
					//	���m�V�[�P���X�ԍ� : ����V�[�P���X�ԍ����m���Ă��Ȃ����Ƃ�����
	byte reserved;	// [2] �\��ς݁F0�Ƃ��đ��M����A�g�p���Ȃ�
	byte hopCount;	// [3] �z�b�v��
	int RREQ_ID;	// [4-7] ���M���m�[�h��IP�A�h���X�ƂƂ��Ɏ�M�������ARREQ�����ʂ��邽�߂̃V�[�P���X�ԍ�
	byte[] toIpAdd;		// [8-11] ���Đ�m�[�h��IP�A�h���X
	int toSeqNum;		// [12-15] ����m�[�h�ւ̌o�H�ɂ����āA���M���m�[�h�ɂ���ĉߋ��̎�M�����ŐV�̃V�[�P���X�ԍ�
	byte[] fromIpAdd;	// [16-19] ���M���m�[�h��IP�A�h���X
	int fromSeqNum;		// [20-23] ���M���m�[�h�ւ̌o�H�ɂ����ė��p����錻�݂̃V�[�P���X�ԍ�
	int timeToLive;		// [24-27] ��������TTL�A���ԃm�[�h���c�肢���܂ŋ�����
	
	// RREQ���b�Z�[�W�̑��M
	// �����F���M��(String�^)
	public void send(byte[] destination_address, byte[] myAddress, boolean flagJ ,boolean flagR ,boolean flagG ,boolean flagD ,boolean flagU
			,int toSeq ,int fromSeq,int ID,int TTL,int port) {
		
		// �e�t�B�[���h�̏�����
		type = 1;	// RREQ������
		// �e�t���O��1�o�C�g�̐擪5�r�b�g�ɔ[�߂�
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
		
		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
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
		
		// �f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		
        // UDP�p�P�b�g�𑗐M�����ƂȂ�u���[�h�L���X�g�A�h���X
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress("255.255.255.255", port);
        
        // UDP�p�P�b�g
        DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
        
        // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
        try {
			soc.send(sendPacket);
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
        
        System.out.println("RREQ���b�Z�[�W�𑗐M���܂���");	//###�f�o�b�O�p###
        	
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
		
/****************** �ȑO�̒ʐM��i *******************************/
//		// �f�[�^�O�����\�P�b�g���J��
//		DatagramSocket soc = new DatagramSocket();
//		
//        // UDP�p�P�b�g�𑗐M�����ƂȂ�u���[�h�L���X�g�A�h���X (5100�ԃ|�[�g)
//        InetSocketAddress remoteAddress =
//        			 new InetSocketAddress("133.71.3.255", 5100);
//        
//        // UDP�p�P�b�g
//        DatagramPacket sendPacket =
//            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
//        
//        // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
//        new DatagramSocket().send(sendPacket);
//        
//        System.out.println("RREQ���b�Z�[�W�𑗐M���܂���");	//###�f�o�b�O�p###
//        	
//        //�f�[�^�O�����\�P�b�g�����
//        soc.close();
/******************************************************************/
        
	}

	/***** RREQ���b�Z�[�W�̓]�� *****/
	public void send2(byte[] data,int port){
		
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e1) {
			// TODO �����������ꂽ catch �u���b�N
			e1.printStackTrace();
		}
		
	    // UDP�p�P�b�g�𑗐M�����ƂȂ�u���[�h�L���X�g�A�h���X
	    InetSocketAddress remoteAddress =
	    			 new InetSocketAddress("255.255.255.255", port);
	    
	    // UDP�p�P�b�g
	    DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(data, 28, remoteAddress);
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
	    
	    // UDP�p�P�b�g�𑗐M
	    try {
			soc.send(sendPacket);
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
	    
	    System.out.println("RREQ���b�Z�[�W��]�����܂���");	//###�f�o�b�O�p###
	    	
	    //�f�[�^�O�����\�P�b�g�����
	    soc.close();
	}
	
	// ��M����RREQ���b�Z�[�W�����g�̃m�[�h���̂��̂����ׂ�
	// �����FRREQ���b�Z�[�W
	public boolean isToMe(byte[] receiveBuffer, byte[] myAddress){
		// ����IP�A�h���X�̃R�s�[����쐬
		byte[] toIpAdd = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer,8,toIpAdd,0,4);
		
		if(Arrays.equals(toIpAdd,myAddress))
				return true;
		else return false;
	}
	
	// RREQ���b�Z�[�W����J�t�B�[���h��Ԃ�
	public boolean getFlagJ(byte[] RREQMes){
		if( (RREQMes[1]&(2<<6)) ==1)
			return true;
		else return false;
	}
	// RREQ���b�Z�[�W����R�t�B�[���h��Ԃ�
	public boolean getFlagR(byte[] RREQMes){
		if( (RREQMes[1]&(2<<5)) ==1)
			return true;
		else return false;
	}
	// RREQ���b�Z�[�W����G�t�B�[���h��Ԃ�
	public boolean getFlagG(byte[] RREQMes){
		if( (RREQMes[1]&(2<<4)) ==1)
			return true;
		else return false;
	}
	// RREQ���b�Z�[�W����D�t�B�[���h��Ԃ�
	public boolean getFlagD(byte[] RREQMes){
		if( (RREQMes[1]&(2<<3)) ==1)
			return true;
		else return false;
	}
	// RREQ���b�Z�[�W����U�t�B�[���h��Ԃ�
	public boolean getFlagU(byte[] RREQMes){
		if( (RREQMes[1]&(2<<2)) ==1)
			return true;
		else return false;
	}
	// RREQ���b�Z�[�W����hopCount�t�B�[���h��Ԃ�
	public byte getHopCount(byte[] RREQMes){
		return RREQMes[3];
	}
	// RREQ���b�Z�[�W����RREQ_ID�t�B�[���h��Ԃ�
	public int getRREQ_ID(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,4,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	// RREQ���b�Z�[�W����toIpAdd�t�B�[���h��Ԃ�
	public byte[] getToIpAdd(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,8,buf,0,4);
		
		return buf;
	}
	// RREQ���b�Z�[�W����toSeqNum�t�B�[���h��Ԃ�
	public int getToSeqNum(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,12,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	// RREQ���b�Z�[�W����fromoIpAdd�t�B�[���h��Ԃ�
	public byte[] getFromIpAdd(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,16,buf,0,4);
		
		return buf;
	}
	// RREQ���b�Z�[�W����fromSeqNum�t�B�[���h��Ԃ�
	public int getFromSeqNum(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,20,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	// RREQ���b�Z�[�W����TTL�t�B�[���h��Ԃ�
	public int getTimeToLive(byte[] RREQMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREQMes,24,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	
	// RREQ���b�Z�[�W�̑��M���V�[�P���X�ԍ��t�B�[���h���Z�b�g���ĕԂ�
	public byte[] setFromSeqNum(byte[] RREQMes,int num){
		// �ύX����ԍ���byte[]�^��
		byte[] seq = intToByte(num);
		
		// ���M���V�[�P���X�ԍ��̕����ɏ㏑��
		System.arraycopy(seq,0,RREQMes,20,4);
		return RREQMes;
	}
	// RREQ���b�Z�[�W��TTL���Z�b�g���ĕԂ�
	public byte[] setTimeToLive(byte[] RREQMes,int num){
		// �ύX����ԍ���byte[]�^��
		byte[] TTL = intToByte(num);
		
		// ���M���V�[�P���X�ԍ��̕����ɏ㏑��
		System.arraycopy(TTL,0,RREQMes,24,4);
		return RREQMes;
	}
	
	// int�^��byte[]�^�֕ϊ�
	public byte[] intToByte(int num){
		
		// �o�C�g�z��ւ̏o�͂��s���X�g���[��
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// �o�C�g�z��ւ̏o�͂��s���X�g���[����DataOutputStream�ƘA������
		DataOutputStream out = new DataOutputStream(bout);
			
		try{	// ���l���o��
			out.writeInt(num);
		}catch(Exception e){
				System.out.println(e);
		}
		
		// �o�C�g�z����o�C�g�X�g���[��������o��
		byte[] bytes = bout.toByteArray();
		return bytes;
	}
	
	// byte[]�^��int�^�֕ϊ�
	public int byteToInt(byte[] num){
		
		int value = 0;
		// �o�C�g�z��̓��͂��s���X�g���[��
		ByteArrayInputStream bin = new ByteArrayInputStream(num);
		
		// DataInputStream�ƘA��
		DataInputStream in = new DataInputStream(bin);
		
		try{	// int��ǂݍ���
			value = in.readInt();
		}catch(IOException e){
			System.out.println(e);
		}
		return value;
	}
	
	// String�^�̃A�h���X��byte[]�^�ɕϊ�
	public byte[] getByteAddress(String str){
		
		// ����
		String[] s_bara = str.split("\\.");
		
		byte[] b_bara = new byte[s_bara.length];
		for(int i=0;i<s_bara.length;i++){
			b_bara[i] = (byte)Integer.parseInt(s_bara[i]);
		}
		return b_bara;
	}
	
	

	
	
	
}
