package jp.ac.ehime_u.cite.rtptest;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.widget.EditText;

public class BlueToothClientThread extends Thread {
    //クライアント側の処理
    private final BluetoothSocket clientSocket;
    private final BluetoothDevice mDevice;
    private Context mContext;
    private EditText edit_text;
    private Handler handler;
    //UUIDの生成
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static BluetoothAdapter myClientAdapter;
    public String myNumber;
 
    //コンストラクタ定義
    public BlueToothClientThread(Context context, String myNum , BluetoothDevice device, BluetoothAdapter btAdapter, Handler handler_, EditText edit_){
        //各種初期化
        mContext = context;
        BluetoothSocket tmpSock = null;
        mDevice = device;
        myClientAdapter = btAdapter;
        myNumber = myNum;
        edit_text = edit_;
        handler = handler_;
 
        try{
            //自デバイスのBluetoothクライアントソケットの取得
            tmpSock = device.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
        }catch(IOException e){
            e.printStackTrace();
        }
        clientSocket = tmpSock;
    }
 
    public void run(){
        //接続要求を出す前に、検索処理を中断する。
        if(myClientAdapter.isDiscovering()){
            myClientAdapter.cancelDiscovery();
        }
 
        try{
            //サーバー側に接続要求
            clientSocket.connect();
        }catch(IOException e){
             try {
                 clientSocket.close();
             } catch (IOException closeException) {
                 e.printStackTrace();
             }
             return;
        }
        
        handler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO 自動生成されたメソッド・スタブ
				edit_text.append("\n接続成功");
			}
		});
 
        //接続完了時の処理
//        ReadWriteModel rw = new ReadWriteModel(mContext, clientSocket, myNumber);
//        rw.start();
        
    }
 
    public void cancel() {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
      }
}
