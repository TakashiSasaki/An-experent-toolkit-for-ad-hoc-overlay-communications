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
	
	// �{�^���Ȃǂ��\�������O�̏����������Ȃ�
	// onCreate���\�b�h���I�[�o�[���C�h�Ƃ��ċL�q���Ă���
	public void onCreate(Bundle savedInstanceState) {
		// onCreate���I�[�o�[���C�h����ꍇ�A�X�[�p�[�N���X�̃��\�b�h���Ăяo���K�v������
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.list);
		context = this;
		
        listView = (ListView)findViewById(R.id.blueToothList);
        listView2 = (ListView)findViewById(R.id.blueToothList2);
        
        // �f�o�C�X�̗����擾
        adapter = new ArrayAdapter<String>(this, R.layout.rowdata);
        
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        
        if(pairedDevices.size() > 0){    //�ڑ������̂���f�o�C�X�����݂���
        	for(BluetoothDevice device:pairedDevices){
        		//�ڑ������̂���f�o�C�X�̏������Ɏ擾���ăA�_�v�^�ɋl�߂�
        		//getName()�E�E�E�f�o�C�X���擾���\�b�h
        		//getAddress()�E�E�E�f�o�C�X��MAC�A�h���X�擾���\�b�h
        		adapter.add(device.getName() + "(" + getBondState(device.getBondState()) + ")" + "\n" + device.getAddress());
        	}
        	listView.setAdapter(adapter);
        }
        
        // ���X�g�r���[(����)����A�C�e����I����
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,
        							int position, long id) {
        		ListView listView = (ListView) parent;
        		// �N���b�N���ꂽ�A�C�e�����擾���܂�
        		String item = (String) listView.getItemAtPosition(position);
        		
        		// �ڑ���������A�C�e���Ɠ��������̂�􂢏o���A�f�o�C�X��������Activity�ɕԂ�
        		Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        		
                if(pairedDevices.size() > 0){    //�ڑ������̂���f�o�C�X�����݂���
                	for(BluetoothDevice device:pairedDevices){
                		// ��v����
                		if(item.equals(device.getName() + "(" + getBondState(device.getBondState()) + ")" +
                				"\n" + device.getAddress())){
                			Intent intent = new Intent();
                			intent.putExtra("DEVICE", device);
                			setResult(RESULT_OK, intent);
                			finish();
                		}
                	}
                	Toast.makeText(getBaseContext(), "���ԋp���s", Toast.LENGTH_LONG);
                }
        	}
        });
        
        // ���X�g�r���[(�V�K)����A�C�e����I����
        listView2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        	@Override
        	public void onItemClick(AdapterView<?> parent, View view,
        							int position, long id) {
        		ListView listView2 = (ListView) parent;
        		// �N���b�N���ꂽ�A�C�e�����擾���܂�
        		String item = (String) listView2.getItemAtPosition(position);
        	}
        });
	}
	
	// ���j���[�̒ǉ�
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0 , Menu.FIRST , Menu.NONE 
				, "���o����").setIcon(android.R.drawable.ic_menu_crop);
		menu.add(0 , Menu.FIRST+1 , Menu.NONE 
				, "���o").setIcon(android.R.drawable.ic_menu_crop);
		
		return ret;
	}
	
	// ���j���[�������ꂽ�Ƃ�
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        
        // ���o���������ꂽ�Ƃ�
        case Menu.FIRST:
            // BlueTooth�̃y�A����
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(intent);
            
        	return true;
        	
        case Menu.FIRST+1:
            // �V�K�f�o�C�X�̌���
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(DevieFoundReceiver, filter);
            
            nonAdapter = new ArrayAdapter<String>(this, R.layout.rowdata);
            //�ڑ��\�ȃf�o�C�X�����o
            if(btAdapter.isDiscovering()){
            	//�������̏ꍇ�͌��o���L�����Z������
            	btAdapter.cancelDiscovery();
            }
            
            //�f�o�C�X����������
            //��莞�Ԃ̊Ԍ��o���s��
            btAdapter.startDiscovery();
        	
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }
	
	private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver(){
		//���o���ꂽ�f�o�C�X����̃u���[�h�L���X�g���󂯂�
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;
			
			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				Log.d("ACTION_DISCOVERY_STARTED","�X�L�����J�n");
			}
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				//�f�o�C�X�����o���ꂽ
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null){
					if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
						//�ڑ��������Ƃ̂Ȃ��f�o�C�X�̂݃A�_�v�^�ɋl�߂�
						nonAdapter.add(dName + "(" + getBondState(foundDevice.getBondState()) + ")" + "\n" + foundDevice.getAddress());
						Log.d("ACTION_FOUND", dName);
						
						// �Ԃ��Ă��܂��[,���艟��
            			Intent intent1 = new Intent();
            			intent1.putExtra("DEVICE", foundDevice);
            			((Activity) context).setResult(RESULT_OK, intent1);
            			finish();
					}
				}
				listView2.setAdapter(nonAdapter);
			}
			if(BluetoothDevice.ACTION_NAME_CHANGED.equals(action)){
				//���O�����o���ꂽ
				Log.d("ACTION_NAME_CHANGED", dName);
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//�ڑ��������Ƃ̂Ȃ��f�o�C�X�̂݃A�_�v�^�ɋl�߂�
					nonAdapter.add(dName + "(" + getBondState(foundDevice.getBondState()) + ")" + "\n" + foundDevice.getAddress());
				}
				listView2.setAdapter(nonAdapter);
			}
			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				Log.d("ACTION_DISCOVERY_FINISHED","�X�L�����I��");
			}
		}
	};
	String getBondState(int state) {
		String strState;

		switch (state) {
		case BluetoothDevice.BOND_BONDED:
			strState = "�ڑ���������";
			break;
		case BluetoothDevice.BOND_BONDING:
			strState = "�ڑ���";
			break;
		case BluetoothDevice.BOND_NONE:
			strState = "�ڑ������Ȃ�";
			break;
		default:
			strState = "�G���[";
		}
		return strState;
	}
}
