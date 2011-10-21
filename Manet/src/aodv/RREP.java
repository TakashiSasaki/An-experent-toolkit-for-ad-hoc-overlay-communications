package aodv;
/* �����������̃L�[���[�h�u###�v */

import java.net.*;
import java.io.*;
import java.util.*;

public class RREP {
	// RREP���b�Z�[�W�̃t�B�[���h	�ȉ�[�t�H�[�}�b�g��̈ʒu(�o�C�g)]�A���
	byte type;		// [0] ���b�Z�[�W�^�C�v
	byte flag;		// [1] �擪2�r�b�g�̂݃t���O(RA)�A�c���0
					//	Repair�t���O�G�}���`�L���X�g�p
					//	Acknowledgement�t���O��RREP-ACK�̔��s������
					
	byte reserved_prefix;	// [2] �\��ς݁F0�Ƃ��đ��M����A�g�p���Ȃ�+�v���t�B�N�X
	byte newHopCount;	// [3] �z�b�v��
	byte[] toIpAdd;		// [4-7] ���Đ�m�[�h��IP�A�h���X
	int toSeqNum;		// [8-11] ����m�[�h�ւ̌o�H�ɂ����āA���M���m�[�h�ɂ���ĉߋ��̎�M�����ŐV�̃V�[�P���X�ԍ�
	byte[] fromIpAdd;	// [12-15] ���M���m�[�h��IP�A�h���X
	int lifeTime;		// [16-19] �������ԁ��o�H���L���ł���ƍl������Ƃ���RREP���󂯎�邽�߂̎���
	
	/********************************************************
	 * HELLO ���b�Z�[�W�̏ꍇ�A�`�����قȂ�
	 * byte type;			// [0] ���b�Z�[�W�^�C�v
	 * byte hopCount;		// [1] �z�b�v��
	 * byte[] toIpAdd;		// [2-5] ���Đ�m�[�h��IP�A�h���X
	 * int toSeqNum;		// [6-9] ����m�[�h�ւ̌o�H�ɂ����āA���M���m�[�h�ɂ���ĉߋ��̎�M�����ŐV�̃V�[�P���X�ԍ�
	 * int lifeTime;		// [10-13] �������ԁ��o�H���L���ł���ƍl������Ƃ���RREP���󂯎�邽�߂̎���
	 ********************************************************/
	
	
	// RREP���b�Z�[�W�̑��M
	// �����F�O�z�b�v�̃m�[�h�̃A�h���X(InetAddress�^),RREP�̃f�[�^�iRREP�̈���h�o�A�h���X��RREP�̑��M���h�o�A�h���X������)
	
	public void reply(InetAddress str, byte[] soushinmoto,InetAddress atesaki,byte hopNum,int seq,int life) throws Exception{
		
		// �e�t�B�[���h�̏�����
		type = 2;	// RREP������
		flag = 0;	// �e�t���O��0
		reserved_prefix = 0;
		newHopCount = hopNum;
		
		//�󂯎�����o�C�g�z��̃f�[�^�̑��M���̂h�o�A�h���Xdata[16~19]�̂S�o�C�g��
		//�q�q�d�o�̈���h�o�A�h���X�̃t�B�[���h�ɃR�s�[���C����𑗐M����p�P�b�g�̃o�C�g�z���4�Ԗڂ���R�s�[����
		//����h�o�A�h���X�ɂ͂q�q�d�o���쐬�����������g�̃A�h���X������̂Œ���
		toIpAdd = atesaki.getAddress();

		toSeqNum = seq;
		
		fromIpAdd = soushinmoto;
		
		lifeTime = life;

		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
		byte[] sendBuffer = new byte[20];
		
		sendBuffer[0] = type;
		sendBuffer[1] = flag;
		sendBuffer[2] = reserved_prefix;
		sendBuffer[3] = newHopCount;
		System.arraycopy(toIpAdd			  ,0,sendBuffer,4,4);
		System.arraycopy(intToByte(toSeqNum)  ,0,sendBuffer,8,4);
		System.arraycopy(fromIpAdd			  ,0,sendBuffer,12,4);
		System.arraycopy(intToByte(lifeTime)  ,0,sendBuffer,16,4);
		
		// �f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = new DatagramSocket();
		
        // UDP�p�P�b�g�𑗐M�����ƂȂ�O�z�b�v�m�[�h�̃A�h���X (5100�ԃ|�[�g)
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress(str, 5100);
        
        // UDP�p�P�b�g
        DatagramPacket sendPacket =
            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
        
        // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
        new DatagramSocket().send(sendPacket);
        
        System.out.println("RREP���b�Z�[�W�𑗐M���܂���");	//###�f�o�b�O�p###
        	
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
	}
	
	// RREP���b�Z�[�W�̑��M�i�t���O�w��p�̃I�[�o�[���[�h ###�j
	// �����F���M��byte[4]�A�t���Oboolean[5]
	/* ..... */
	
	
	/***** RREP���b�Z�[�W�̓]�� *****/
	
	// �����F�󂯎�����o�C�g�z��CRREP�]����̂h�o�A�h���X
	public void reply2(byte[] data, InetAddress lastNODE) throws Exception{
		
		// �z�b�v��+1
		data[3]++;

		//�f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = new DatagramSocket();
		
	    // UDP�p�P�b�g�𑗐M�����ƂȂ�u���[�h�L���X�g�A�h���X (5100�ԃ|�[�g)
	    InetSocketAddress remoteAddress =
	    			 new InetSocketAddress(lastNODE, 5100);
	    
	    // UDP�p�P�b�g
	    DatagramPacket sendPacket =
	        new DatagramPacket(data, 20, remoteAddress);
	    
	    // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
	    new DatagramSocket().send(sendPacket);
	    
	    System.out.println("RREP���b�Z�[�W��]�����܂���");	//###�f�o�b�O�p###
	    	
	    //�f�[�^�O�����\�P�b�g�����
	    soc.close();
	    
		}
	
	// HELLO���b�Z�[�W�̑��M�iTTL=1��RREP�j
	// �����F�V�[�P���X�ԍ��A��������
	public void send(int seq,int life) throws Exception{
		
		type = 2;	// HELLO���b�Z�[�W
		newHopCount = 0;
		
		// ���m�[�h�̃A�h���X
		toIpAdd  = InetAddress.getLocalHost().getAddress();
		toSeqNum = seq;
		
		lifeTime = life;
		
		
		// UDP�p�P�b�g�Ɋ܂߂�f�[�^
		byte[] sendBuffer = new byte[14];
		
		sendBuffer[0] = type;
		sendBuffer[1] = newHopCount;
		System.arraycopy(toIpAdd			  ,0,sendBuffer,2,4);
		System.arraycopy(intToByte(toSeqNum)  ,0,sendBuffer,6,4);
		System.arraycopy(intToByte(lifeTime)  ,0,sendBuffer,10,4);
		
		// �f�[�^�O�����\�P�b�g���J��
		DatagramSocket soc = new DatagramSocket();
		
        // �u���[�h�L���X�g (5100�ԃ|�[�g)
        InetSocketAddress remoteAddress =
        			 new InetSocketAddress("133.71.3.255", 5100);
        
        // UDP�p�P�b�g
        DatagramPacket sendPacket =
            new DatagramPacket(sendBuffer, sendBuffer.length, remoteAddress);
        
        // DatagramSocket�C���X�^���X�𐶐����āAUDP�p�P�b�g�𑗐M
        new DatagramSocket().send(sendPacket);
        
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
		
	}
	
	// ��M����RREP���b�Z�[�W�����g�̃m�[�h���̂��̂����ׂ�
	// �����FRREP���b�Z�[�W
	public boolean isToMe(byte[] receieveBuffer,int length) throws Exception{
		
		// HELLO���b�Z�[�W�Ȃ�true�Ŗ��Ȃ�
		if(length == 14)
			return true;
		
		// ����IP�A�h���X�̃R�s�[����쐬
		byte[] fromIpAdd = new byte[4];
		
		// �Y�������𔲂��o��
		System.arraycopy(receieveBuffer,12,fromIpAdd,0,4);
		
		if(Arrays.equals(fromIpAdd,InetAddress.getLocalHost().getAddress()))
				return true;
		else return false;
	}
	// RREP���b�Z�[�W����R�t�B�[���h��Ԃ�
	public boolean getFlagR(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return false;
		
		if( (RREPMes[1]&(2<<6)) ==1)
			return true;
		else return false;
	}
	// RREP���b�Z�[�W����A�t�B�[���h��Ԃ�
	public boolean getFlagA(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return false;
		
		if( (RREPMes[1]&(2<<5)) ==1)
			return true;
		else return false;
	}
	// RREP���b�Z�[�W����newHopCount�t�B�[���h��Ԃ�
	public byte getHopCount(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			return RREPMes[1];
		
		return RREPMes[3];
	}
	
	// RREP���b�Z�[�W����toIpAdd�t�B�[���h��Ԃ�
	public byte[] getToIpAdd(byte[] RREPMes,int length){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,2,buf,0,4);
		else
			System.arraycopy(RREPMes,4,buf,0,4);
		
		return buf;
	}
	// RREP���b�Z�[�W����toSeqNum�t�B�[���h��Ԃ�
	public int getToSeqNum(byte[] RREPMes,int length){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,6,buf,0,4);
		else
			System.arraycopy(RREPMes,8,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	// RREP���b�Z�[�W����fromoIpAdd�t�B�[���h��Ԃ�
	public byte[] getFromIpAdd(byte[] RREPMes,int length) throws Exception{
		
		// HELLO?
		if(length == 14)
			return InetAddress.getLocalHost().getAddress();
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		System.arraycopy(RREPMes,12,buf,0,4);
		
		return buf;
	}
	// RREP���b�Z�[�W����lifeTime�t�B�[���h��Ԃ�
	public int getLifeTime(byte[] RREPMes,int length){
		
		// �Y��������byte[]�𔲂��o��
		byte[] buf = new byte[4];
		
		// HELLO?
		if(length == 14)
			System.arraycopy(RREPMes,10,buf,0,4);
		else
			System.arraycopy(RREPMes,16,buf,0,4);
		
		// int�^�ɕϊ�
		return byteToInt(buf);
	}
	
	// HopCount++
	public byte[] hopCountInc(byte[] RREPMes,int length){
		// HELLO?
		if(length == 14)
			RREPMes[1]++;
		else
			RREPMes[3]++;
		
		return RREPMes;
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
