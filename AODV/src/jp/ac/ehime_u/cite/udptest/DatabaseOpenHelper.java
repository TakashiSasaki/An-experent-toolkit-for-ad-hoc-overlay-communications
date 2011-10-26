package jp.ac.ehime_u.cite.udptest;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	// �f�[�^�x�[�X��
	private static final String DB_NAME = "ROUTE_TABLE";
	// �e�[�u����
	public static final String TABLE_NAME = "route";
	// �J������
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_IP_1 = "AddressPart1";
	public static final String COLUMN_IP_2 = "AddressPart2";
	public static final String COLUMN_IP_3 = "AddressPart3";
	public static final String COLUMN_IP_4 = "AddressPart4";
	public static final String COLUMN_HOP_COUNT = "HopCount";
	public static final String COLUMN_LifeTime = "LifeTime";
	public static final String COLUMN_CAN_USE = "CanUse";

	/** * �R���X�g���N�^ */
	public DatabaseOpenHelper(Context context) {
		// �w�肵���f�[�^�x�[�X�������݂��Ȃ��ꍇ�́A�V���ɍ쐬����onCreate()���Ă΂��
		// �o�[�W������ύX�����onUpgrade()���Ă΂��
		super(context, DB_NAME, null, 1);
	}

	/** * �f�[�^�x�[�X�̐����ɌĂяo�����̂ŁA �X�L�[�}�̐������s�� */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			// �e�[�u���̐���
			StringBuilder createSql = new StringBuilder();
			createSql.append("create table " + TABLE_NAME + " (");
			createSql.append(COLUMN_ID + " integer primary key,");
			
			createSql.append(COLUMN_IP_1 + " integer not null,");
			createSql.append(COLUMN_IP_2 + " integer not null,");
			createSql.append(COLUMN_IP_3 + " integer not null,");
			createSql.append(COLUMN_IP_4 + " integer not null,");
			createSql.append(COLUMN_HOP_COUNT + " integer not null,");
			createSql.append(COLUMN_LifeTime + " integer not null,");
			createSql.append(COLUMN_CAN_USE + " text not null");
			createSql.append(")");
			
			db.execSQL(createSql.toString());
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	
	// �f�[�^�x�[�X�̍X�V 
	@Override 
	public void onUpgrade(SQLiteDatabase db, int
		oldVersion, int newVersion) { 
		// nothing
	}
}
