package jp.ac.ehime_u.cite.udptest;

// RREQ���d�����Ď�M���Ȃ��悤�A�Z���ԂɎ�M����RREQID�ƃA�h���X���L�^
public class PastData {
	int RREQ_ID;
	byte[] IpAdd;
	long lifeTime;
	
	public PastData(int ID,byte[] Add,long life){
		RREQ_ID = ID;
		IpAdd = Add;
		lifeTime = life;
	}
}
