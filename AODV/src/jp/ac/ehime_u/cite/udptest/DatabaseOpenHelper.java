package jp.ac.ehime_u.cite.udptest;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
	// データベース名
	private static final String DB_NAME = "ROUTE_TABLE";
	// テーブル名
	public static final String TABLE_NAME = "route";
	// カラム名
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_IP_1 = "AddressPart1";
	public static final String COLUMN_IP_2 = "AddressPart2";
	public static final String COLUMN_IP_3 = "AddressPart3";
	public static final String COLUMN_IP_4 = "AddressPart4";
	public static final String COLUMN_HOP_COUNT = "HopCount";
	public static final String COLUMN_LifeTime = "LifeTime";
	public static final String COLUMN_CAN_USE = "CanUse";

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
	
	
	// データベースの更新 
	@Override 
	public void onUpgrade(SQLiteDatabase db, int
		oldVersion, int newVersion) { 
		// nothing
	}
}
