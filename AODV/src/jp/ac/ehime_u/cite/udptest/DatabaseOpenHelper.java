package jp.ac.ehime_u.cite.udptest;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	// �f�[�^�x�[�X��
	private static final String DB_NAME = "MEMBER";
	// �e�[�u����
	public static final String TABLE_NAME = "member";
	// �J������
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_POSITION = "position";
	public static final String COLUMN_NUMBER = "number";

	// ���������T���v���f�[�^
	private String[][] datas = new String[][] { { "�R�c���Y", "�ߎ�", "2" },
			{ "��S����", "�O�ێ�", "5" }, { "�����q", "����", "1" },
			{ "�a�n��l", "��ێ�", "4" }, { "���ΎO���Y", "�O���", "7" } };

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
			
			createSql.append(COLUMN_NAME + " text,");
			createSql.append(COLUMN_POSITION + " text,");
			createSql.append(COLUMN_NUMBER + " text");
			createSql.append(")");
			
			db.execSQL(createSql.toString());
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		// �T���v���f�[�^�̓���
		db.beginTransaction();
		try {
			for (String[] data : datas) {
				ContentValues values = new ContentValues();
				values.put(COLUMN_NAME, data[0]);
				values.put(COLUMN_POSITION, data[1]);
				values.put(COLUMN_NUMBER, data[2]);
				db.insert(TABLE_NAME, null, values);
			}
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
