package jp.ac.ehime_u.cite.udptest;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class RouteActivity extends Activity {

	protected SQLiteDatabase db;
	protected Cursor cursor;
	protected ListAdapter adapter;
	protected ListView cigaretteList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		db = (new DatabaseOpenHelper(this)).getWritableDatabase();
		
		cigaretteList = (ListView) findViewById(R.id.list);
		
		Cursor c = db.query("member", null, null, null, null, null, null);
		c.moveToFirst();
		
		
//		AODV_Activity.handler.post(new Runnable() {
//			
//			@Override
//			public void run() {
//				cigaretteList.setAdapter(new SimpleCursorAdapter(this,
//						R.layout.list,
//						c,
//						new String[] { "name", "position","number" },
//						new int[] { R.id.name, R.id.position, R.id.number }));
//			}
//		});

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
			return true;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
