package jp.ac.ehime_u.cite.udptest;

import java.util.HashSet;

public class RouteTable {
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
	HashSet<byte[]> preList = new HashSet<byte[]>();
	
	/************** �t�B�[���h�̗v�f�����܂� ***************/
	
	// new�p�Alist.add(new RouteTable(��L�t�B�[���h�𖄂߂����));�ŗv�f��ǉ��ł���
	public RouteTable(byte[] IP,int num,boolean numFlag,byte state,
				byte hopNum,byte[] nextAdd,long time,HashSet<byte[]> list){
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
