package jp.ac.ehime_u.cite.udptest;

// �Е��������N�ł���A�������ׂ�RREQ���M���O�z�b�v�m�[�h
public class BlackData {
	byte[] ip_add;
	long life_time;
	
	public BlackData(byte[] address,long life){
		ip_add = address;
		life_time = life;
	}
}
