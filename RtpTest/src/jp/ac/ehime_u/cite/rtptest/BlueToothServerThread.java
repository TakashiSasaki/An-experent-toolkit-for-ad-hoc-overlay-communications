package jp.ac.ehime_u.cite.rtptest;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.widget.EditText;

public class BlueToothServerThread extends Thread {
    //サーバー側の処理
    //UUID：Bluetoothプロファイル毎に決められた値
    private final BluetoothServerSocket servSock;
    static BluetoothAdapter myServerAdapter;
    private Context mContext;
    private EditText edit_text;
    private Handler handler;
    //UUIDの生成
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public String myNumber;
 
    //コンストラクタの定義
    public BlueToothServerThread(Context context,String myNum, BluetoothAdapter btAdapter, Handler handler_, EditText edit_){
        //各種初期化
        mContext = context;
        BluetoothServerSocket tmpServSock = null;
        myServerAdapter = btAdapter;
        myNumber = myNum;
        edit_text = edit_;
        handler = handler_;
        try{
            //自デバイスのBluetoothサーバーソケットの取得
             tmpServSock = myServerAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", TECHBOOSTER_BTSAMPLE_UUID);
        }catch(IOException e){
            e.printStackTrace();
        }
        servSock = tmpServSock;
    }
 
    public void run(){
        BluetoothSocket receivedSocket = null;
        while(true){
            try{
                //クライアント側からの接続要求待ち。ソケットが返される。
                receivedSocket = servSock.accept();
            }catch(IOException e){
                break;
            }
 
            if(receivedSocket != null){
                //ソケットを受け取れていた(接続完了時)の処理
                handler.post(new Runnable() {
        			
        			@Override
        			public void run() {
        				// TODO 自動生成されたメソッド・スタブ
        				edit_text.append("\n接続され成功");
        			}
        		});
 
                try {
                    //処理が完了したソケットは閉じる。
                    servSock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
 
    public void cancel() {
            try {
                servSock.close();
            } catch (IOException e) { }
        }
}
