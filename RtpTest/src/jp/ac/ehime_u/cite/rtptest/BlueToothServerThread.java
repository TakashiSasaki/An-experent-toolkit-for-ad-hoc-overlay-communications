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
    //�T�[�o�[���̏���
    //UUID�FBluetooth�v���t�@�C�����Ɍ��߂�ꂽ�l
    private final BluetoothServerSocket servSock;
    static BluetoothAdapter myServerAdapter;
    private Context mContext;
    private EditText edit_text;
    private Handler handler;
    //UUID�̐���
    public static final UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public String myNumber;
 
    //�R���X�g���N�^�̒�`
    public BlueToothServerThread(Context context,String myNum, BluetoothAdapter btAdapter, Handler handler_, EditText edit_){
        //�e�평����
        mContext = context;
        BluetoothServerSocket tmpServSock = null;
        myServerAdapter = btAdapter;
        myNumber = myNum;
        edit_text = edit_;
        handler = handler_;
        try{
            //���f�o�C�X��Bluetooth�T�[�o�[�\�P�b�g�̎擾
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
                //�N���C�A���g������̐ڑ��v���҂��B�\�P�b�g���Ԃ����B
                receivedSocket = servSock.accept();
            }catch(IOException e){
                break;
            }
 
            if(receivedSocket != null){
                //�\�P�b�g���󂯎��Ă���(�ڑ�������)�̏���
                handler.post(new Runnable() {
        			
        			@Override
        			public void run() {
        				// TODO �����������ꂽ���\�b�h�E�X�^�u
        				edit_text.append("\n�ڑ����ꐬ��");
        			}
        		});
 
                try {
                    //���������������\�P�b�g�͕���B
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
