package jp.ac.ehime_u.cite.udptest;

import java.net.*;
import java.io.*;
import java.util.*;

// RREP��M����ACK��Ԃ�
// �Е��������N�̌��m�ɗp����
public class RREP_ACK {

	// �t�H�[�}�b�g [����]�̓o�C�g�񒆂̈ʒu������
	byte type;		// [0] 4:RREP_ACK������
	byte reserved;	// [1] ��o�C�g,�g�p����Ȃ�(�g���p)

	// RREP_ACK�̑��M
	// ����1: ACK��Ԃ�����m�[�h�̃A�h���X
	// ����2: port�ԍ�
	public void send(InetAddress destination_inet,int port){

		type = 4;
		reserved = 0;

		// ���M�o�C�g��
		byte[] send_buffer = new byte[2];

		send_buffer[0] = type;
		send_buffer[1] = reserved;

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
        			 new InetSocketAddress(destination_inet.getHostAddress(), port);
        
        // UDP�p�P�b�g
        DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(send_buffer, send_buffer.length, remoteAddress);
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
        
        System.out.println("RREP_ACK���b�Z�[�W�𑗐M���܂���");	//###�f�o�b�O�p###
        	
        //�f�[�^�O�����\�P�b�g�����
        soc.close();
	}
}