package jp.ac.ehime_u.cite.udptest;
/* �����������̃L�[���[�h�u###�v */

import java.net.*;
import java.io.*;
import java.util.*;

public class RERR {
	// RERR���b�Z�[�W�̃t�B�[���h	�ȉ�[�t�H�[�}�b�g��̈ʒu(�o�C�g)]�A���
	byte type;		// [0] ���b�Z�[�W�^�C�v
	byte flag;		// [1] NoDelete�t���O�F�m�[�h�������N��LocalRepair���s���Ă���A�㗬�m�[�h���o�H���폜���ׂ��łȂ��ꍇ
	byte destCount;	// [2] �s�B���搔�FRERR�Ɋ܂܂�Ă���s�B���搔(1�ȏ�)
	byte[] IpAdd;	// [3-6] �s�B�ƂȂ����m�[�h��IP�A�h���X
	int SeqNum;		// [7-10] �ȑO��IpAdd�t�B�[���h�Ń��X�g�A�b�v���ꂽ����m�[�h�ɑ΂���o�H�̃V�[�P���X�ԍ�
	
	// RERR���b�Z�[�W�̑��M�A���j�L���X�g�p
	// �����F
	public void send(boolean flagN,byte[] add,int seq,byte[] atesaki,int port){
		
		type = 3;	// RERR���b�Z�[�W
		
		flag = (byte)((flagN)? 2<<6:0);
		destCount = 1;
		IpAdd  = add;
		SeqNum = seq;
		
		
		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
		byte[] sendBuffer = new byte[11];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = destCount;
		System.arraycopy(IpAdd			  ,0,sendBuffer,3,4);
		System.arraycopy(intToByte(SeqNum),0,sendBuffer,7,4);
	
		// �f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		
        // UDP�p�P�b�g�𑗐M�����ƂȂ�O�z�b�v�m�[�h�̃A�h���X
        InetSocketAddress remoteAddress = null;
		try {
			remoteAddress = new InetSocketAddress(InetAddress.getByAddress(atesaki).getHostAddress(), port);
		} catch (UnknownHostException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
        
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
			new DatagramSocket().send(sendPacket);
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
        
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
		
	}
	
	// RERR���b�Z�[�W�̑��M�A�u���[�h�L���X�g�p
	public void send(boolean flagN,byte[] add,int seq,int port){
		
		type = 3;	// RERR���b�Z�[�W
		
		flag = (byte)((flagN)? 2<<6:0);
		destCount = 1;
		IpAdd  = add;
		SeqNum = seq;
		
		
		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
		byte[] sendBuffer = new byte[11];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = destCount;
		System.arraycopy(IpAdd			  ,0,sendBuffer,3,4);
		System.arraycopy(intToByte(SeqNum),0,sendBuffer,7,4);
	
		// �f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = null;
		try {
			soc = new DatagramSocket();
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		
        // UDP�p�P�b�g�𑗐M�����ƂȂ�O�z�b�v�m�[�h�̃A�h���X
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
			new DatagramSocket().send(sendPacket);
		} catch (SocketException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
        
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
		
	}
	
	// RERR���b�Z�[�W����IpAdd�t�B�[���h��Ԃ�
	public byte[] getIpAdd(byte[] RERRMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RERRMes,3,buf,0,4);
		
		return buf;
	}
	// RERR���b�Z�[�W����SeqNum�t�B�[���h��Ԃ�
	public int getSeqNum(byte[] RERRMes){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RERRMes,7,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
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
}