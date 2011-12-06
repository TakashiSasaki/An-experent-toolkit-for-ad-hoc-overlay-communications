package jp.ac.ehime_u.cite.udptest;

// 片方向リンクであり、無視すべきRREQ送信元前ホップノード
public class BlackData {
	byte[] ip_add;
	long life_time;
	
	public BlackData(byte[] address,long life){
		ip_add = address;
		life_time = life;
	}
}
