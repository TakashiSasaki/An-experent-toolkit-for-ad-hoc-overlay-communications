package aodv;
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
	
	// RERR���b�Z�[�W�̑��M
	// �����F
	
	public void send(boolean flagN,byte[] add,int seq,byte[] atesaki) throws Exception{
		
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
		DatagramSocket soc = new DatagramSocket();
		
        // UDP�p�P�b�g�𑗐M�����ƂȂ�O�z�b�v�m�[�h�̃A�h���X (5100�ԃ|�[�g)
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress(InetAddress.getByAddress(atesaki), 5100);
        
        // UDP�p�P�b�g
        DatagramPacket sendPacket =
            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
        
        // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
        new DatagramSocket().send(sendPacket);
        
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
		
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