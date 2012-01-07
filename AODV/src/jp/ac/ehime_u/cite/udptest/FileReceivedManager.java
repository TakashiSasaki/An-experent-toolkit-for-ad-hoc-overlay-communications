package jp.ac.ehime_u.cite.udptest;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

public class FileReceivedManager {
	
	// �����o�ϐ�
	String file_name;			// �t�@�C����
	int receive_file_next_no;	// ���Ɏ�M���ׂ�No
	FileOutputStream file;		// �o�͗p�ɃI�[�v������File�N���X
	BufferedOutputStream out;	// ����
	long life_time;				// ��������
	
	// �R���X�g���N�^�[
	public FileReceivedManager(int receive_file_next_no, FileOutputStream file,
			BufferedOutputStream out) {
		super();
		this.receive_file_next_no = receive_file_next_no;
		this.file = file;
		this.out = out;
	}
	
	// �r������p ArrayList�ɒǉ�
	public void add(){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.add(this);
		}
	}
	
	// �r������p ArrayList����擾
	public FileReceivedManager get(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			return UdpListener.file_received_manager.get(index);
		}
	}
	
	// �r������p ArrayList����폜
	public void remove(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.remove(index);
		}
	}
	
	// �r������p ArrayList�̏����㏑��
	// ����E�t�@�C�������Ăяo����(this)�ƈ�v����o�ߋL�^�ɏ㏑��
	public void set(int index){
		synchronized(AODV_Activity.fileReceivedManagerLock){
			UdpListener.file_received_manager.set(index, this);
		}
	}
}
