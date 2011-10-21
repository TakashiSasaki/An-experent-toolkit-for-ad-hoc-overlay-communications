package jp.ac.ehime_u.cite.udptest;

import java.util.HashSet;

public class RouteTable {
	/************** フィールドの要素 ***********************/
	
	byte[] toIpAdd;	// 宛先IPアドレス
	int toSeqNum;	// 宛先シーケンス番号
	boolean validToSeqNumFlag;	// 有効宛先シーケンス番号フラグ
	byte stateFlag;		// 他の状態フラグ（有効1,無効2,修復可能3,修復中4)
	
	// ### ネットワークインタフェース ###
	// ？？？
	
	byte hopCount;		// ホップ数
	byte[] nextIpAdd;	// 次ホップのIPアドレス
	long lifeTime;		// 生存時間
	
	// 宛先に対応した逆経路のリスト,RERRの送信先
	HashSet<byte[]> preList = new HashSet<byte[]>();
	
	/************** フィールドの要素ここまで ***************/
	
	// new用、list.add(new RouteTable(上記フィールドを埋める引数));で要素を追加できる
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
