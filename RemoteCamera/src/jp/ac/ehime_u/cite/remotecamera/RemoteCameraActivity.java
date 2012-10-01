package jp.ac.ehime_u.cite.remotecamera;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewDebug.FlagToString;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class RemoteCameraActivity extends Activity{
	
	private static final int MENU_AUTOFOCUS = Menu.FIRST + 1;
	
	// �����o�ϐ�
	// EditText�N���X(ip���͂�{�^�������Ȃǃ��[�U�[�̓��͂��������邽��)
	private EditText editTextSrc;
	private EditText editTextSrcPort;
	private EditText editTextDest;
	private EditText editTextDefaultPictureName;
	private EditText editTextPictureSizeWidth;
	private EditText editTextPictureSizeHeight;
	private CheckBox checkBoxWatching;
	protected static String calling_address;
	private String calling_file_name;
	
	// �C���e���g����p�ϐ�
	private static int prev_receive_intent_id = -1;
	private static String prev_receive_intent_package_name = null;
	protected static int send_intent_id = 0;
	
	// �C���e���g�ɂ�鑽�d�N���𐧌䂷�邽�߂̕ϐ�
	// �N������Activity�����J�E���g
	private static int remote_camera_count = 0;
	protected static int image_viewer_count = 0;
	protected static int auto_focus_count = 0;
	
	// �t�@�C���֘A
	protected static String file_name;
	protected static String file_path;
	
	// �Â��t�@�C���̍폜
	protected static int DELETE_TIME = 90 * 1000; // [ms]
	
	// �qActivity�ɓn���R���e�L�X�g
	protected static Context context;
	
	/***** ImageViewerActivity�̃����o�ϐ�(�ꕔ)
	 * �C���e���g�̑�s������eActivity�ōs�����߂ɕK�v 
	 *****/
	protected static int select_image_no = 0;
	protected static ArrayList<String> image_name_list = new ArrayList<String>();
	protected static ArrayList<String> address_list = new ArrayList<String>();
	protected static boolean draw_switch;
	
	/***** AutoFocus�̃����o�ϐ�(�ꕔ) *****/
	protected static boolean loop_flag;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
		// �N����++
		remote_camera_count++;
		
		context = this;
		
		// �ÖٓIIntent�̉������
		Intent receive_intent = getIntent();
		
		String package_name = receive_intent.getStringExtra("PACKAGE");
		int intent_id = receive_intent.getIntExtra("ID", 0);
		
		// ���C���e���g�̑��d�����h�~ �O��ƃp�b�P�[�W���܂���ID���قȂ��Ă���Ύ�
		if( (package_name != prev_receive_intent_package_name)
				|| intent_id != prev_receive_intent_id){
			
			// ���O�̃p�b�P�[�W��,ID�Ƃ��ċL�^
			prev_receive_intent_package_name = package_name;
			prev_receive_intent_id = intent_id;
		
			// �N�����@�̃`�F�b�N �ÖٓI�C���e���g:CALL�ł���
			// �摜�����[���N�����Ă��Ȃ����
			if(Intent.ACTION_CALL.equals(receive_intent.getAction())
					&& image_viewer_count == 0){
				final Uri uri = receive_intent.getData();
				
				// scheme��"CameraCapture"�Ȃ�
				if("CameraCapture".equals(uri.getScheme())){
					
					// "CameraCapture:"�ȍ~���擾
					String call_data = uri.getEncodedSchemeSpecificPart();
					
					// STOP���߂Ȃ�t���O��؂�ւ��ďI��
					if("STOP".equals(call_data)){
						loop_flag = false;
					}
					else{
						// �擪���珇�ɐ؂�o��
						String[] call_data_list = call_data.split("_");
					
						// [0]:�t���O
						// [1]:�v�����T�C�Y
						// [2]:�v���c�T�C�Y
						// [3]:���M��(�ԐM�p)
						calling_address = call_data_list[3].toString();
						
						file_name = getPreDefaultFileName();
						
						// �J�������N�����łȂ���΋N��
						if(auto_focus_count == 0){
				            Intent intent = new Intent();
				            intent.setClassName(
				                    "jp.ac.ehime_u.cite.remotecamera",
				                    "jp.ac.ehime_u.cite.remotecamera.AutoFocus");
				            
				            intent.putExtra("FLAG", call_data_list[0]);
				            intent.putExtra("SIZE_X", Integer.parseInt(call_data_list[1]));
				            intent.putExtra("SIZE_Y", Integer.parseInt(call_data_list[2]));
				            
				            startActivity(intent);
						}
					}
				}
			}
			
			// �N�����@�̃`�F�b�N �A�N�V������VIEW�ł���
			// �J�������N�����Ă��Ȃ����
			// ImageViewerActivity��p���ĉ摜�̕\��
			if(Intent.ACTION_VIEW.equals(receive_intent.getAction())
					&& auto_focus_count == 0){
				final Uri uri = receive_intent.getData();
				
				// scheme��"Files"�Ȃ�
				if("Files".equals(uri.getScheme())){
					// Files:�ȍ~�̕�������擾
					calling_file_name = uri.getEncodedSchemeSpecificPart();
					String calling_address = receive_intent.getStringExtra("SOURCE_ADDRESS");
					
					Log.d("JpegFile",calling_file_name);
					
					deleteOldFile();
					moveFile();
					//deleteAodvFile(calling_file_name);
					
					// �C���e���g�̏�����s
					if( (calling_file_name != null) && (calling_address != null) ){
						
						int index_result_file;
						int index_result_add;
						
						// ���Ƀ��X�g�ɖ������`�F�b�N
						if((index_result_file = searchFile(calling_file_name)) == -1){	// �t�@�C�����������
							if((index_result_add = searchAddress(calling_address)) == -1){	// �A�h���X���������
								// �����ɒǉ�
								image_name_list.add(calling_file_name);
								address_list.add(calling_address);
							}
							else{
								// �A�h���X����v�����V�[�P���X�ԍ��̃t�@�C������ύX
								image_name_list.set(index_result_add, calling_file_name);
								
								// �I�𒆂̉摜�Ȃ��
								if( index_result_add == select_image_no){
									// �J������
									draw_switch = true;
								}
							}
						}
						else{	// �����̃t�@�C���Əd��
							// �ʃA�h���X����̃t�@�C�����d���Ȃ�
							if(calling_address.equals(address_list.get(index_result_file)) != true){
								// �A�h���X��*�㏑��*
								// �i���A�h���X�������ӏ��ł���\���𖳎��j
								address_list.set(index_result_file, calling_address);
							}
							
							// �I�𒆂̉摜�Ȃ��
							if( index_result_file == select_image_no){
								// �J������
								draw_switch = true;
							}
						}
					}
					
					// ImageViewerActivity���N�����Ȃ�C���e���g�͓����Ȃ�
					if(image_viewer_count < 1){
						
						// �摜�r���[�N��
			            Intent intent = new Intent();
			            intent.setClassName(
			                    "jp.ac.ehime_u.cite.remotecamera",
			                    "jp.ac.ehime_u.cite.remotecamera.ImageViewerActivity");
			            startActivity(intent);
			            
					}
					
				}
			}
		}
		
		// ���d�N���Ȃ�I������
		if(remote_camera_count > 1){
			Log.d("RemoteCameraActivity_onCreate()","if(count>1) finish()");
			finish();
		}
		
		// �N�����Ƀ\�t�g�L�[�{�[�h�̗����オ���h��
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// �A�N�e�B�r�e�B�Ƀr���[�O���[�v��ǉ�����
		setContentView(R.layout.main);
		
		// ID�ɑΉ�����View���擾�A�^���قȂ�̂ŃL���X�g
		editTextDest = (EditText) findViewById(R.id.editTextDest);
		editTextSrc = (EditText) findViewById(R.id.editTextSrc);
		editTextSrcPort = (EditText) findViewById(R.id.editTextSrcPort);
		editTextDefaultPictureName = (EditText) findViewById(R.id.editTextDefaultFileName);
		editTextPictureSizeWidth = (EditText) findViewById(R.id.editTextPictureSizeWidth);
		editTextPictureSizeHeight = (EditText) findViewById(R.id.editTextPictureSizeHeight);
		checkBoxWatching = (CheckBox) findViewById(R.id.checkBoxWatching);
		
		
		
		// ���g�̃A�h���X���擾
		try {
			editTextSrc.setText(getIPAddress());
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		
		// �f�t�H���g�ʐ^�t�@�C�������擾
		// �ߋ��ɐݒ肵�����O������Ȃ炻����p�����p *���[�J���t�@�C���𗘗p
		editTextDefaultPictureName.setText(getPreDefaultFileName(editTextSrc.getText().toString()));
		
		
		// �t�@�C������ݒ莞�Ƀ��[�J���t�@�C���ɕۑ�����C�x���g��o�^
		editTextDefaultPictureName.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
			@Override
			public void afterTextChanged(Editable s) {
				setPreDefaultFileName(editTextDefaultPictureName.getText().toString());
			}
		});
		
		// �v���ʐ^�T�C�Y�Ɏ��g�̃T�C�Y��ݒ�(������)
		WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		
		// ����������{�ɐݒ� X>Y
		if(disp.getHeight() > disp.getWidth()){
			// �c���t�ɃZ�b�g
			editTextPictureSizeHeight.setText(String.valueOf(disp.getWidth()));
			editTextPictureSizeWidth.setText(String.valueOf(disp.getHeight()));
		}
		else{	// �����̂ق����L���f�B�X�v���C�Ȃ炻�̂܂�
			editTextPictureSizeHeight.setText(String.valueOf(disp.getHeight()));
			editTextPictureSizeWidth.setText(String.valueOf(disp.getWidth()));
		}
		
		// ���MButton�A���l��ID����擾
		Button buttonSend = (Button) findViewById(R.id.buttonSend);

		// �N���b�N������o�^
		// AODV�ɐڑ��v���E�ʐ^�v���𓊂���
		buttonSend.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				// ����̕�������擾
				String destination_s = editTextDest.getText().toString();
				
				// �ʐ^�T�C�Y���擾
				String size_width = editTextPictureSizeWidth.getText().toString();
				String size_height= editTextPictureSizeHeight.getText().toString();
				
				// �ÖٓI�C���e���g�𓊂���
	            Intent intent = new Intent();
	            intent.setAction(Intent.ACTION_SENDTO);
	            intent.setData(Uri.parse("connect:"+destination_s));
	            intent.putExtra("TASK", "TASK:CameraCapture:"+getWatchingLoopCheck()+"_"
	            		+size_width+"_"+size_height);
	            intent.putExtra("PACKAGE","jp.ac.ehime_u.cite.remotecamera");
	            intent.putExtra("ID", send_intent_id);
	            
	            startActivity(intent);
			}
		});

    }
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
		// �N�������f�N�������g
		remote_camera_count --;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_AUTOFOCUS, Menu.NONE, "camera_wake");
		menu.add(Menu.NONE, MENU_AUTOFOCUS+1, Menu.NONE, "image_view");
		menu.add(Menu.NONE, MENU_AUTOFOCUS+2, Menu.NONE, "END");
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean rc = true;
		switch (item.getItemId()) {
		case MENU_AUTOFOCUS:
			// �J�����N��
			// �t�@�C�����ɓ��t���g�p
//			Date date = new Date();
//			SimpleDateFormat sdf = new SimpleDateFormat("yyyy'_'MMdd'_'HHmmss");
//			
//			file_name = sdf.format(date) + ".jpg";
//			
//			calling_address = "11.1.1.1";
//			
//            Intent intent = new Intent();
//            intent.setClassName(
//                    "jp.ac.ehime_u.cite.remotecamera",
//                    "jp.ac.ehime_u.cite.remotecamera.AutoFocus");
//            startActivity(intent);
			// �t�@�C���ړ�
//			File pre_path = new File("/data/data/jp.ac.ehime_u.cite.udptest/files/", "2011_1220_151329.jpg");
//			File next_path= new File("/data/data/jp.ac.ehime_u.cite.remotecamera/files/");
//			
//			boolean a = pre_path.renameTo(new File(next_path, "2011_1220_151329.jpg"));
			
			calling_file_name = "test.jpg";
			deleteAodvFile(calling_file_name);
			
			break;
			
		case MENU_AUTOFOCUS+1:
			
            Intent intent1 = new Intent();
            intent1.setClassName(
                    "jp.ac.ehime_u.cite.remotecamera",
                    "jp.ac.ehime_u.cite.remotecamera.ImageViewerActivity");
            intent1.putExtra("FILE_NAME", "test.jpg");
            startActivity(intent1);
			
			
			break;
		case MENU_AUTOFOCUS+2:
			Context c1 = null;
			try {
				c1 =  createPackageContext("jp.ac.ehime_u.cite.image",0);
			} catch (NameNotFoundException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}
			
			try {
				OutputStream o = c1.openFileOutput("test.jpg",MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE);
				o.write(1);
				o.close();
				
				c1.openFileInput("test.jpg");
				Toast.makeText(this, "�݂���", Toast.LENGTH_LONG).show();
				
			} catch (FileNotFoundException e) {
				// TODO �����������ꂽ catch �u���b�N
				
			} catch (IOException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}
			
            //finish();
		default:
			rc = super.onOptionsItemSelected(item);
			break;
		}
		return rc;
	}
    
	// ���g��IP�A�h���X���擾
	private String getIPAddress() throws IOException{
	    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        
	    while(interfaces.hasMoreElements()){
	        NetworkInterface network = interfaces.nextElement();
	        Enumeration<InetAddress> addresses = network.getInetAddresses();
	            
	        while(addresses.hasMoreElements()){
	            String address = addresses.nextElement().getHostAddress();
	                
	            //127.0.0.1��0.0.0.0�ȊO�̃A�h���X�����������炻���Ԃ�
	            if(!"127.0.0.1".equals(address) && !"0.0.0.0".equals(address)){
	                return address;
	            }
	        }
	    }
	        
	    return "127.0.0.1";
	}
	
	// �t�@�C���폜
	private void deleteOldFile(){
		
		// �t�@�C���ꗗ�̎擾
		File[] files = new File("/data/data/jp.ac.ehime_u.cite.remotecamera/files/").listFiles();
		
		// ���ݎ���
		long now_time = new Date().getTime();
		
		for(int i=0; i<files.length; i++){
			
			// �摜Viewer�ɓo�^�ς݂Ȃ�폜���Ȃ�
			if( searchFile(files[i].getName()) == -1){
				// �f�B���N�g���ł͂Ȃ��A�����ł���t�@�C��������
				if(!files[i].isDirectory()){
					long last_update_time = files[i].lastModified();
					
					// �ŏI�X�V��������폜���Ԉȏ�̎��Ԃ��߂��Ă����
					if( (now_time - last_update_time) > DELETE_TIME){
						deleteFile(files[i].getName());
					}
				}
			}
		}
	}
	
	// �t�@�C���ړ� *udptest->remotecamera �p*
	private void moveFile(){
		
		// �ϐ��錾
		Context aodv_c = null;
		
		try {	
			// �ǂݍ��݌��̃R���e�L�X�g�쐬
			aodv_c = createPackageContext("jp.ac.ehime_u.cite.udptest",0);
			// �ǂݍ��݌��̃t�@�C���X�g���[�����쐬���A�`���l�����擾
			FileChannel in_channel = aodv_c.openFileInput(calling_file_name).getChannel();
			
			// ���l�ɏo�͐�
			FileChannel out_channel= context.openFileOutput(calling_file_name, MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE)
								.getChannel();
			
			// �]��
			in_channel.transferTo(0, in_channel.size(), out_channel);
			
			// �N���[�Y
			in_channel.close();
			out_channel.close();
			
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// �t�@�C���폜 *udptest��*
	private void deleteAodvFile(String file_name){
		
		// �폜���߃C���e���g�𔭍s
		// AODV�ɖ����I�C���e���g�𓊂���
        Intent intent = new Intent();
        intent.setClassName(
                "jp.ac.ehime_u.cite.udptest",
                "jp.ac.ehime_u.cite.udptest.AODV_Activity");
        intent.setAction(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("path:"+file_name));
        intent.putExtra("PACKAGE","jp.ac.ehime_u.cite.remotecamera");
        intent.putExtra("ID", send_intent_id);
        startActivity(intent);
        
        send_intent_id++;
	}
	
	// �ȑO�̃f�t�H���g�l�[����Ԃ�
	// ������Ύ��g�̃A�h���X�𗘗p
    private CharSequence getPreDefaultFileName(String ip_address) {
    	
    	try {
			InputStream in = openFileInput("fileName.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String s = reader.readLine();
			
			reader.close();
			in.close();
			return s;
			
		} catch (Exception e) {	// IOException+FileNotFoundException+...
			return ip_address.replaceAll("\\.", "_");
		}
	}
    private String getPreDefaultFileName() {
    	
    	try {
			InputStream in = openFileInput("fileName.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			String s = reader.readLine();
			
			reader.close();
			in.close();
			return s;
			
		} catch (Exception e) {	// IOException+FileNotFoundException+...
			try {
				return getIPAddress().replaceAll("\\.", "_");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return null;
	}
    
    // �f�t�H���g�l�[���Ƃ��ă��[�J���t�@�C���ɕێ�
    private void setPreDefaultFileName(String name){
		try {
			OutputStream out = openFileOutput("fileName.txt",MODE_PRIVATE);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(out,"UTF-8"));
			writer.write(name);
			
			writer.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    // WatchingLoop�̃`�F�b�N�擾
    // �߂�l�FString�^��"1"(�I��),"0"(�I�t)
    private String getWatchingLoopCheck(){
    	if(checkBoxWatching.isChecked()){
    		return "1";
    	}
    	return "0";
    }
    
    // �摜Viewer�p ���X�g�Ɋ��ɑ��݂��邩�`�F�b�N
	private int searchFile(String name){
		for(int i=0;i<image_name_list.size();i++){
			if(image_name_list.get(i).equals(name)){
				return i;
			}
		}
		return -1;
	}
	
	// �摜Viewer�p ���X�g�Ɋ��ɑ��݂��邩�`�F�b�N
	private int searchAddress(String address){
		for(int i=0;i<address_list.size();i++){
			if(address_list.get(i).equals(address)){
				return i;
			}
		}
		return -1;
	}
    
}