package jp.ac.ehime_u.cite.udptest;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Date;

import android.content.Context;

// �������M���܂��͑��M����̃t�@�C�����A�o�߂Ȃǂ�ێ�
public class FileManager {
	byte[] buffer = null;
	String file_name = null;
	BufferedInputStream file_in;
	FileInputStream file;
	int total_step;
	int file_length;
	int file_name_b_length;
	int file_next_no;
	byte[] destination_address;
	long life_time;
	
	
	
	// �t�@�C���I�[�v��
	public FileManager(String name,byte[] dest_add,byte[] source_add,Context context) throws FileNotFoundException{
		file_name = name;
		file = context.openFileInput(file_name);	// �t�@�C���I�[�v��
		file_in = new BufferedInputStream(file);
		
		try {
			file_length = file_in.available();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// �t�@�C���T�C�Y
		total_step = file_length/ AODV_Activity.MAX_SEND_FILE_SIZE +1;	// �t�@�C��������
		file_name_b_length = file_name.getBytes().length;	// �t�@�C����(byte)�̒���
		
		buffer = addMessageTypeFile(AODV_Activity.MAX_SEND_FILE_SIZE, dest_add, source_add,
				file_name,total_step);
		
		file_next_no = 1;
		destination_address = dest_add;
		
		life_time = new Date().getTime() + AODV_Activity.ACTIVE_ROUTE_TIMEOUT * 2;
	}

	// �������M(�t�@�C���I�[�v��,�N���[�Y����)
	public void fileSend(byte[] source_address, byte[] next_hop_address_b, int port){
		
		// �t�@�C���𕪊��ǂݍ���->���M
		
		if (total_step == file_next_no) { // �ŏI�p�P�b�g�Ȃ�buffer���𒲐�
			buffer = addMessageTypeFile(file_length % AODV_Activity.MAX_SEND_FILE_SIZE,
					destination_address, source_address, file_name,
					total_step);
		}
		
		// �X�V����K�v������2����������
		System.arraycopy(intToByte(file_next_no), 0, buffer, 9, 4);	// 1������,�p�P�b�g�ԍ�
		
		// 2������,�p�P�b�g�f�[�^
		try {
			file_in.read(buffer, 21 + file_name_b_length,
					// �ŏI�p�P�b�g�Ȃ�c��T�C�Y�A�����łȂ��Ȃ���E�T�C�Y�ǂݍ���
					(total_step == file_next_no) ? file_length % AODV_Activity.MAX_SEND_FILE_SIZE
							: AODV_Activity.MAX_SEND_FILE_SIZE);
		} catch (IOException e) {
			// TODO �����������ꂽ catch �u���b�N
			e.printStackTrace();
		}
		
		file_next_no++;
		
		// ���Mtry
		try {
			// ���M��(���z�b�v)���
			InetAddress next_hop_inet = InetAddress.getByAddress(next_hop_address_b);
			InetSocketAddress destination_inet_socket_address = new InetSocketAddress(
					next_hop_inet.getHostAddress(), port);
			
			// ���M�p�P�b�g�̐���
			DatagramPacket packet_to_be_sent = new DatagramPacket(buffer,
					buffer.length, destination_inet_socket_address);
			// ���M�p�̃N���X�𐶐��A���M�A�N���[�Y
			DatagramSocket datagram_socket = new DatagramSocket();
			datagram_socket.send(packet_to_be_sent);
			datagram_socket.close();
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	

	
	// �r������p ArrayList�ɒǉ�
	public void add(){
		synchronized(AODV_Activity.fileManagerLock){
			AODV_Activity.file_manager.add(this);
		}
	}
	
	// �r������p ArrayList����폜
	// ����E�t�@�C�������Ăяo����(this)�ƈ�v����o�ߋL�^���폜
	public void remove(){
		synchronized(AODV_Activity.fileManagerLock){
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( this.file_name.equals(AODV_Activity.file_manager.get(i).file_name) 
						&& Arrays.equals(this.destination_address, 
								AODV_Activity.file_manager.get(i).destination_address)){
					AODV_Activity.file_manager.remove(i);
					break;
				}
			}
		}
	}
	
	// �r������p ArrayList�̏����㏑��
	// ����E�t�@�C�������Ăяo����(this)�ƈ�v����o�ߋL�^�ɏ㏑��
	public void set(){
		synchronized(AODV_Activity.fileManagerLock){
			for(int i=0; i<AODV_Activity.file_manager.size();i++){
				if( this.file_name.equals(AODV_Activity.file_manager.get(i).file_name) 
						&& Arrays.equals(this.destination_address, 
								AODV_Activity.file_manager.get(i).destination_address)){
					AODV_Activity.file_manager.set(i, this);
					break;
				}
			}
		}
	}
	
	// �t�@�C�����M�p�^�C�v10�₻�̑�����f�[�^��t������
	// �ȉ��̓t�H�[�}�b�g�A[����]�͔z����̈ʒu������
	// [0]		: ���b�Z�[�W�^�C�v10
	// [1-4]	: ����A�h���X
	// [5-8]	: ���M���A�h���X
	// [9-12]	: �����������Ԗڂ̃f�[�^��	* ���̃��\�b�h�ł͑�����Ȃ� *
	// [13-16]	: �����ɕ���������
	// [17-20]	: �t�@�C�����̃T�C�Y
	// [21-??]	: �t�@�C����(�ϒ�)
	// [??-??]	: �t�@�C���f�[�^(�ϒ�,�ő�63K[byte]) * ���̃��\�b�h�ł͑�����Ȃ� *
	private byte[] addMessageTypeFile(int fileSize,byte[] toIPAddress,byte[] my_address,
			String fileName,int step){
		
		byte[] fileName_b = fileName.getBytes();	// �t�@�C������byte��
		byte[] fileName_size_b = intToByte(fileName_b.length);	// byte�^�t�@�C�����̃T�C�Y��byte��
		byte[] step_b = intToByte(step);			// ��������byte��
		
		byte[] new_message = new byte[21 + fileName_b.length + fileSize];
		
		new_message[0] = 10;	// ���b�Z�[�W�^�C�v10
		System.arraycopy(toIPAddress, 0, new_message, 1, 4);
		System.arraycopy(my_address, 0, new_message, 5, 4);
		// [9-12] �����������Ԗڂ̃f�[�^�� �������Ȃ�
		System.arraycopy(step_b, 0, new_message, 13, 4);
		System.arraycopy(fileName_size_b, 0, new_message, 17, 4);
		System.arraycopy(fileName_b, 0, new_message, 21, fileName_b.length);
		// [??-??] �t�@�C���f�[�^�������Ȃ�
		
		return new_message;
	}
	
	// int�^��byte[]�^�֕ϊ�
	private byte[] intToByte(int num){
		
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
}
