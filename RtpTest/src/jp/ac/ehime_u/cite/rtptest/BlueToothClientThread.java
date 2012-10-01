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
    //�N���C�A���g���̏���
    private final BluetoothSocket clientSocket;
    private final BluetoothDevice mDevice;
    private Context mContext;
    private EditText edit_text;
    private Handler handler;
    //UUID�̐���
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    static BluetoothAdapter myClientAdapter;
    public String myNumber;
 
    //�R���X�g���N�^��`
    public BlueToothClientThread(Context context, String myNum , BluetoothDevice device, BluetoothAdapter btAdapter, Handler handler_, EditText edit_){
        //�e�평����
        mContext = context;
        BluetoothSocket tmpSock = null;
        mDevice = device;
        myClientAdapter = btAdapter;
        myNumber = myNum;
        edit_text = edit_;
        handler = handler_;
 
        try{
            //���f�o�C�X��Bluetooth�N���C�A���g�\�P�b�g�̎擾
            tmpSock = device.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
        }catch(IOException e){
            e.printStackTrace();
        }
        clientSocket = tmpSock;
    }
 
    public void run(){
        //�ڑ��v�����o���O�ɁA���������𒆒f����B
        if(myClientAdapter.isDiscovering()){
            myClientAdapter.cancelDiscovery();
        }
 
        try{
            //�T�[�o�[���ɐڑ��v��
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
				// TODO �����������ꂽ���\�b�h�E�X�^�u
				edit_text.append("\n�ڑ�����");
			}
		});
 
        //�ڑ��������̏���
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
