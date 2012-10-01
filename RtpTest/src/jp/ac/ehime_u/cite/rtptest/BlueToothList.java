package jp.ac.ehime_u.cite.rtptest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BlueToothList extends Activity {
	
    private ArrayAdapter<String> adapter;
    private ArrayAdapter<String> nonAdapter;
    private ListView listView;
    private ListView listView2;
    private BluetoothAdapter btAdapter;
    private Context context;
	
	// ボタンなどが表示される前の初期化処理など
	// onCreateメソッドをオーバーライドとして記述していく
	public void onCreate(Bundle savedInstanceState) {
		// onCreateをオーバーライドする場合、スーパークラスのメソッドを呼び出す必要がある
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.list);
		context = this;
		
        listView = (ListView)findViewById(R.id.blueToothList);
        listView2 = (ListView)findViewById(R.id.blueToothList2);
        
        // デバイスの履歴取得
        adapter = new ArrayAdapter<String>(this, R.layout.rowdata);
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        
        if(pairedDevices.size() > 0){    //接続履歴のあるデバイスが存在する
        	for(BluetoothDevice device:pairedDevices){
        		//接続履歴のあるデバイスの情報を順に取得してアダプタに詰める
        		//getName()・・・デバイス名取得メソッド
        		//getAddress()・・・デバイスのMACアドレス取得メソッド
        		adapter.add(device.getName() + "(" + getBondState(device.getBondState()) + ")" + "\n" + device.getAddress());
        	}
        	listView.setAdapter(adapter);
        }
        
        // リストビュー(履歴)からアイテムを選択時
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,
        							int position, long id) {
        		ListView listView = (ListView) parent;
        		// クリックされたアイテムを取得します
        		String item = (String) listView.getItemAtPosition(position);
        		
        		// 接続履歴からアイテムと等しいものを洗い出し、デバイス情報を元のActivityに返す
        		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        		
                if(pairedDevices.size() > 0){    //接続履歴のあるデバイスが存在する
                	for(BluetoothDevice device:pairedDevices){
                		// 一致検索
                		if(item.equals(device.getName() + "(" + getBondState(device.getBondState()) + ")" +
                				"\n" + device.getAddress())){
                			Intent intent = new Intent();
                			intent.putExtra("DEVICE", device);
                			setResult(RESULT_OK, intent);
                			finish();
                		}
                	}
                	Toast.makeText(getBaseContext(), "情報返却失敗", Toast.LENGTH_LONG);
                }
        	}
        });
        
        // リストビュー(新規)からアイテムを選択時
        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,
        							int position, long id) {
        		ListView listView2 = (ListView) parent;
        		// クリックされたアイテムを取得します
        		String item = (String) listView2.getItemAtPosition(position);
        	}
        });
	}
	
	// メニューの追加
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0 , Menu.FIRST , Menu.NONE 
				, "検出許可").setIcon(android.R.drawable.ic_menu_crop);
		menu.add(0 , Menu.FIRST+1 , Menu.NONE 
				, "検出").setIcon(android.R.drawable.ic_menu_crop);
		
		return ret;
	}
	
	// メニューが押されたとき
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        // 検出許可が押されたとき
        case Menu.FIRST:
            // BlueToothのペア許可
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(intent);
            
        	return true;
        	
        case Menu.FIRST+1:
            // 新規デバイスの検索
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(DevieFoundReceiver, filter);
            
            nonAdapter = new ArrayAdapter<String>(this, R.layout.rowdata);
            //接続可能なデバイスを検出
            if(btAdapter.isDiscovering()){
            	//検索中の場合は検出をキャンセルする
            	btAdapter.cancelDiscovery();
            }
            
            //デバイスを検索する
            //一定時間の間検出を行う
            btAdapter.startDiscovery();
        	
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
	
	private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver(){
		//検出されたデバイスからのブロードキャストを受ける
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;
			
			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				Log.d("ACTION_DISCOVERY_STARTED","スキャン開始");
			}
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				//デバイスが検出された
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null){
					if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
						//接続したことのないデバイスのみアダプタに詰める
						nonAdapter.add(dName + "(" + getBondState(foundDevice.getBondState()) + ")" + "\n" + foundDevice.getAddress());
						Log.d("ACTION_FOUND", dName);
						
						// 返してしまえー,ごり押し
            			Intent intent1 = new Intent();
            			intent1.putExtra("DEVICE", foundDevice);
            			((Activity) context).setResult(RESULT_OK, intent1);
            			finish();
					}
				}
				listView2.setAdapter(nonAdapter);
			}
			if(BluetoothDevice.ACTION_NAME_CHANGED.equals(action)){
				//名前が検出された
				Log.d("ACTION_NAME_CHANGED", dName);
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//接続したことのないデバイスのみアダプタに詰める
					nonAdapter.add(dName + "(" + getBondState(foundDevice.getBondState()) + ")" + "\n" + foundDevice.getAddress());
				}
				listView2.setAdapter(nonAdapter);
			}
			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				Log.d("ACTION_DISCOVERY_FINISHED","スキャン終了");
			}
		}
	};
	String getBondState(int state) {
		String strState;

		switch (state) {
		case BluetoothDevice.BOND_BONDED:
			strState = "接続履歴あり";
			break;
		case BluetoothDevice.BOND_BONDING:
			strState = "接続中";
			break;
		case BluetoothDevice.BOND_NONE:
			strState = "接続履歴なし";
			break;
		default:
			strState = "エラー";
		}
		return strState;
	}
}
