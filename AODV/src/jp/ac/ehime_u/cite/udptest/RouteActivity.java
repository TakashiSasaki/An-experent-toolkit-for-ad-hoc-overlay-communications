package jp.ac.ehime_u.cite.udptest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class RouteActivity extends Activity {

	public static SQLiteDatabase db;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected static ListView cigaretteList;
	
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
	
	// View
	EditText edit_text_address1;
	EditText edit_text_address2;
	EditText edit_text_address3;
	EditText edit_text_address4;
	CheckBox checkbox;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// 起動時にソフトキーボードの立ち上がりを防ぐ
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// route.layoutをセット
		setContentView(R.layout.route);
		
		// データベース読み込み、存在しない場合(初回)は自動作成される
		db = (new DatabaseOpenHelper(this)).getWritableDatabase();
		
		deleteRoute_sql();
		// SQLに全データを投入
		RouteTable route;
		for(int i=0;i<AODV_Activity.routeTable.size();i++){
			// i番目の経路を取得
			route = AODV_Activity.getRoute(i);
			
			addRoute_sql(route.toIpAdd, route.hopCount
					, route.lifeTime - new Date().getTime(), route.stateFlag);
		}
		
		cigaretteList = (ListView) findViewById(R.id.list);
		
		Cursor c = db.query(TABLE_NAME, null, null, null, null, null, null);
		c.moveToFirst();
		
		// リストビューにデータを反映
		cigaretteList.setAdapter(new SimpleCursorAdapter(this,
				R.layout.list,
				c,
				new String[] { COLUMN_IP_1, COLUMN_IP_2, COLUMN_IP_3, COLUMN_IP_4
					, COLUMN_HOP_COUNT, COLUMN_LifeTime, COLUMN_CAN_USE },
				new int[] { R.id.AddressPart1, R.id.AddressPart2, R.id.AddressPart3, R.id.AddressPart4
					, R.id.HopCount, R.id.LifeTime, R.id.CanUse }));
		
		
		// リストビュークリック時の処理
		cigaretteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				EditText editTextDest = AODV_Activity.editTextDest;
				
				// viewは選択した項目を指す
				LinearLayout linear = (LinearLayout)view;
				
				// ListView中のLinearLayout中のTextViewを参照
				TextView text1 = (TextView)linear.findViewById(R.id.AddressPart1);
				TextView text2 = (TextView)linear.findViewById(R.id.AddressPart2);
				TextView text3 = (TextView)linear.findViewById(R.id.AddressPart3);
				TextView text4 = (TextView)linear.findViewById(R.id.AddressPart4);
				
				editTextDest.setText( text1.getText() + "." 
						+ text2.getText() + "." + text3.getText() + "." + text4.getText() );
				
				finish();
			}
		});
		
		
		Button buttonReload = (Button)findViewById(R.id.reload);
		
		// クリック処理、匿名クラス(その場限りの名前の無いクラス)を利用
		// ボタン毎に、ビューを意識せずに処理を記述できる
		buttonReload.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// SQLのデータを全削除
				deleteRoute_sql();
				
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				
				// ListViewに反映
				reflesh();
			}
		});
		
		// 各検索条件のEditTextなどを読み込み
		edit_text_address1 = (EditText)findViewById(R.id.search_addressPart1);
		edit_text_address2 = (EditText)findViewById(R.id.search_addressPart2);
		edit_text_address3 = (EditText)findViewById(R.id.search_addressPart3);
		edit_text_address4 = (EditText)findViewById(R.id.search_addressPart4);
		checkbox = (CheckBox)findViewById(R.id.checkBox1);
		
		// 変更時のイベント割り当て
		edit_text_address1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				// SQLのデータを全削除
				deleteRoute_sql();
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				// ListViewに反映
				reflesh();
			}
		});
		
		// 変更時のイベント割り当て
		edit_text_address2.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				// SQLのデータを全削除
				deleteRoute_sql();
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				// ListViewに反映
				reflesh();
			}
		});
		
		// 変更時のイベント割り当て
		edit_text_address3.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				// SQLのデータを全削除
				deleteRoute_sql();
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				// ListViewに反映
				reflesh();
			}
		});
		
		// 変更時のイベント割り当て
		edit_text_address4.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				// SQLのデータを全削除
				deleteRoute_sql();
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				// ListViewに反映
				reflesh();
			}
		});
		
		// チェック時のイベント割り当て
		checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// SQLのデータを全削除
				deleteRoute_sql();
				// SQLに全データを投入
				RouteTable route;
				for(int i=0;i<AODV_Activity.routeTable.size();i++){
					// i番目の経路を取得
					route = AODV_Activity.getRoute(i);
					
					addRoute_sql(route.toIpAdd, route.hopCount
							, route.lifeTime - new Date().getTime(), route.stateFlag);
				}
				// ListViewに反映
				reflesh();
			}
		});
	}
	
	// テーブルにレコード(データ)を追加する
	public static void addRoute_sql(byte[] ip_address,int hopCount,long lifeTime_left,int state){

		// データの投入
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
		
			if(ip_address[0]<0)
				values.put(COLUMN_IP_1, ip_address[0]+256);
			else
				values.put(COLUMN_IP_1, ip_address[0]);
			if(ip_address[1]<0)
				values.put(COLUMN_IP_2, ip_address[1]+256);
			else
				values.put(COLUMN_IP_2, ip_address[1]);
			if(ip_address[2]<0)
				values.put(COLUMN_IP_3, ip_address[2]+256);
			else
				values.put(COLUMN_IP_3, ip_address[2]);
			if(ip_address[3]<0)
				values.put(COLUMN_IP_4, ip_address[3]+256);
			else
				values.put(COLUMN_IP_4, ip_address[3]);
			values.put(COLUMN_HOP_COUNT, hopCount);
			values.put(COLUMN_LifeTime, lifeTime_left);
			
			if(state == 1)
				values.put(COLUMN_CAN_USE, "有効");
			else
				values.put(COLUMN_CAN_USE, "無効");
			
			db.insert(TABLE_NAME, null, values);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}
	
	// テーブル中のレコード全削除
	public static void deleteRoute_sql(){
		db.execSQL("delete from route");
	}
	
	
	// Activityを更新する
	// 更新の際、検索条件などを加味する
	public void reflesh(){
		// 更新処理
		boolean where_on = false;
		// 検索条件
		String text_address1 = edit_text_address1.getText().toString();
		String text_address2 = edit_text_address2.getText().toString();
		String text_address3 = edit_text_address3.getText().toString();
		String text_address4 = edit_text_address4.getText().toString();
		boolean available_sw = checkbox.isChecked();
		
		// sql対応の検索条件(query化)
		StringBuilder where_builder = new StringBuilder();
		
		where_builder.append("select * from "+ TABLE_NAME);
		
		// IPアドレスの最初の部分が入力されているなら
		if(! "".equals(text_address1)){
			where_on = true;
			
			where_builder.append(" where ");
			where_builder.append(COLUMN_IP_1 +" like '"+ text_address1 +"%'");	// %はワイルドカード
		}
		
		// IPアドレスの2番目の部分が入力されているなら
		if(! "".equals(text_address2)){
			if(!where_on){
				where_builder.append(" where ");
			}
			else{
				where_builder.append(" and ");
				where_on = true;
			}
			where_builder.append(COLUMN_IP_2 +" like '"+ text_address2 +"%'");	// %はワイルドカード
		}
		
		// IPアドレスの3番目の部分が入力されているなら
		if(! "".equals(text_address3)){
			if(!where_on){
				where_builder.append(" where ");
			}
			else{
				where_builder.append(" and ");
				where_on = true;
			}
			where_builder.append(COLUMN_IP_3 +" like '"+ text_address3 +"%'");	// %はワイルドカード
		}
		
		// IPアドレスの4番目の部分が入力されているなら
		if(! "".equals(text_address4)){
			if(!where_on){
				where_builder.append(" where ");
			}
			else{
				where_builder.append(" and ");
				where_on = true;
			}
			where_builder.append(COLUMN_IP_4 +" like '"+ text_address4 +"%'");	// %はワイルドカード
		}
		
		// 有効経路のみ表示がチェックされているなら
		if(available_sw){
			if(!where_on){
				where_builder.append(" where ");
			}
			else{
				where_builder.append(" and ");
				where_on = true;
			}
			where_builder.append(COLUMN_CAN_USE +" like '有効'");	// %はワイルドカード
		}
		
		String where = where_builder.toString();
		Cursor c = db.rawQuery(where, null);
		c.moveToFirst();
		
		cigaretteList.setAdapter(new SimpleCursorAdapter(getApplicationContext(),
				R.layout.list,
				c,
				new String[] { COLUMN_IP_1, COLUMN_IP_2, COLUMN_IP_3, COLUMN_IP_4
					, COLUMN_HOP_COUNT, COLUMN_LifeTime, COLUMN_CAN_USE },
				new int[] { R.id.AddressPart1, R.id.AddressPart2, R.id.AddressPart3, R.id.AddressPart4
					, R.id.HopCount, R.id.LifeTime, R.id.CanUse }));
		
	}
	
	// sqlから経路を一つ削除
	public static void removeRoute_sql(byte[] address){
		int[] address_fixed = new int[4];
		
		for(int i=0;i<4;i++){
			if(address[i]<0)
				address_fixed[i] = address[i]+256;
		}
		
		String sql = "delete from route where "
					+ COLUMN_IP_1 +" = '" + address_fixed[0] + "' and "
					+ COLUMN_IP_2 +" = '" + address_fixed[1] + "' and "
					+ COLUMN_IP_3 +" = '" + address_fixed[2] + "' and "
					+ COLUMN_IP_4 +" = '" + address_fixed[3] + "'";
		db.execSQL(sql);
		
	}
	
	// メニューの追加
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST, Menu.NONE, getString(R.string.menu_main))
				.setIcon(android.R.drawable.ic_menu_crop);
		return ret;
	}

	// メニューが押されたとき
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Menu.FIRST:
			// Activityを終了し、MainActivityに戻る
			finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
