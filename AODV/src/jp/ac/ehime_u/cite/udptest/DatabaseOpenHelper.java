package jp.ac.ehime_u.cite.udptest;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	// データベース名
	private static final String DB_NAME = "MEMBER";
	// テーブル名
	public static final String TABLE_NAME = "member";
	// カラム名
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_POSITION = "position";
	public static final String COLUMN_NUMBER = "number";

	// 初期投入サンプルデータ
	private String[][] datas = new String[][] { { "山田太郎", "捕手", "2" },
			{ "岩鬼正美", "三塁手", "5" }, { "里中智", "投手", "1" },
			{ "殿馬一人", "二塁手", "4" }, { "微笑三太郎", "外野手", "7" } };

	/** * コンストラクタ */
	public DatabaseOpenHelper(Context context) {
		// 指定したデータベース名が存在しない場合は、新たに作成されonCreate()が呼ばれる
		// バージョンを変更するとonUpgrade()が呼ばれる
		super(context, DB_NAME, null, 1);
	}

	/** * データベースの生成に呼び出されるので、 スキーマの生成を行う */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			// テーブルの生成
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
		// サンプルデータの投入
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
	
	// データベースの更新 
	@Override 
	public void onUpgrade(SQLiteDatabase db, int
		oldVersion, int newVersion) { 
		// nothing
	}
}
