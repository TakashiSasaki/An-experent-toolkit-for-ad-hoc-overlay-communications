package jp.ac.ehime_u.cite.udptest;

// RREQを重複して受信しないよう、短い間に受信したRREQIDとアドレスを記録
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
