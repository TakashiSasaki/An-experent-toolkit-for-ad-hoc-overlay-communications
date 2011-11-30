package jp.ac.ehime_u.cite.udptest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

// �t�@�C�����M�p�N���X
public class FSEND {
	
	/*** ���b�Z�[�W�t�H�[�}�b�g [����]***/
	byte type;	// [0] ���b�Z�[�W�^�C�v 10����g�p
	byte[] destination_address;	// [1-4]	����A�h���X
	byte[] source_address;		// [5-8]	���M���A�h���X
	int step_total;				// [9-12]	����,����
	int step_no;				// [13-16]	������̓��p�P�b�g�̔ԍ�,�ʒu
	int file_name_b_size;		// [17-20]	�t�@�C�����̃T�C�Y
	String file_name;			// [21-??]	�t�@�C����
	byte[] file_data;			// [??-??]	�t�@�C���f�[�^
	
	
	// �t�@�C���p�P�b�g�v�����b�Z�[�W�̑��M
	// �����m�F�v���g�R���̉�������
	public void file_req(InetAddress str, byte[] soushinmoto,byte[] atesaki,int step_next_no,String file_str,int port) {
		
		// �e�t�B�[���h�̏�����
		type = 11;
		destination_address = atesaki;
		source_address = soushinmoto;
		byte[] file_name_b = file_str.getBytes();
		int file_name_b_length = file_name_b.length;

		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
		byte[] sendBuffer = new byte[13+file_name_b_length];
		
		sendBuffer[0] = type;
		System.arraycopy(destination_address  	,0,sendBuffer,1,4);
		System.arraycopy(source_address		  	,0,sendBuffer,5,4);
		System.arraycopy(intToByte(step_next_no),0,sendBuffer,9,4);
		System.arraycopy(file_name_b		  	,0,sendBuffer,13,file_name_b_length);
		
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
        			 new InetSocketAddress(str.getHostAddress(), port);
        
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
	
	/****** ���b�Z�[�W�^�C�v 10 �p���\�b�h ******/
	// ���b�Z�[�W�����������H
	boolean isToMe(byte[] receiveBuffer,byte[] myAddress){
		// ����IP�A�h���X�̃R�s�[����쐬
		byte[] toIpAdd = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer,1,toIpAdd,0,4);
		
		if(Arrays.equals(toIpAdd,myAddress))
				return true;
		else return false;
	}
	
	// ���b�Z�[�W�̒����父��A�h���X�𔲂��o��
	byte[] getAddressDest(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 1, add, 0, 4);
		
		return add;
	}
	
	// ���b�Z�[�W�̒����瑗�M���A�h���X�𔲂��o��
	byte[] getAddressSrc(byte[] receiveBuffer){
		byte[] add = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 5, add, 0, 4);
		
		return add;
	}
	
	// ���b�Z�[�W�̒�����A���̃p�P�b�g���S�̂̓��A���Ԗڂ̃p�P�b�g���𔲂��o��
	int getStepNo(byte[] receiveBuffer){
		// ���l�̃R�s�[����쐬
		byte[] step_no = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 9, step_no, 0, 4);
		
		return byteToInt(step_no);
	}
	
	// ���b�Z�[�W�̒�����A���f�[�^�����p�P�b�g�ɕ����������𔲂��o��
	int getStepTotal(byte[] receiveBuffer){
		// ���l�̃R�s�[����쐬
		byte[] step_total = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 13, step_total, 0, 4);
		
		return byteToInt(step_total);
	}
	
	// ���b�Z�[�W�̒�����A�t�@�C����(byte)�̒����𔲂��o��
	int getFileNameLength(byte[] receiveBuffer){
		// ���l�̃R�s�[����쐬
		byte[] name_size = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 17, name_size, 0, 4);
		
		return byteToInt(name_size);
	}
	
	// ���b�Z�[�W�̒�����A�t�@�C�����𔲂��o��
	String getFileName(byte[] receiveBuffer,int length){
		// ���l�̃R�s�[����쐬
		byte[] file_name = new byte[length];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 21, file_name, 0, length);
		
		return new String(file_name);
	}
	
	// ���b�Z�[�W�̒�����A�t�@�C���f�[�^�𔲂��o��
	byte[] getFileData(byte[] receiveBuffer,int file_name_length,int length){
		// ���l�̃R�s�[����쐬
		byte[] file_data = new byte[length];
		
		// �Y�������𔲂��o��
		System.arraycopy(receiveBuffer, 21+file_name_length, file_data, 0, length);
		
		return file_data;
	}
	
	/************* �^�ϊ��p���\�b�h *************/
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
