package jp.ac.ehime_u.cite.image;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImageTestActivity extends Activity {
	
	static ImageView image;
	static Bitmap bmp;
	static Bitmap bmp_copy;
	static FileOutputStream in;
	static long count=0;
	static int sum = 0;
	static int flag =0;
	int non_flag = 0;
	static Context context;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        count++;

//        if(count > 1){
//        	flag += 1;
//        	non_flag += 1;
//        	
//        	Log.d("image","finish()");
//        	//finish();
//        	
//        }
        Log.d("image","onCreate()");
        
        context = this;
        
        setContentView(R.layout.main);
        
//        Button b = (Button)findViewById(R.id.button1);
//		b.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {count++;
//				non_flag++;
//				setTitle("count:"+count+",flag:"+flag+",non_flag:"+non_flag);
//				//finish();
//			}
//		});
		
		
		setTitle("MANET");
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.d("image","onPause()");
    }
    
    @Override
    public void onStop(){
    	super.onStop();
    	Log.d("image","onStop()");
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	
    	Log.d("image","onDestroy()");
    	count--;
    }
    
    @Override
    public void onStart(){
    	super.onStart();
    	
    	Log.d("image","onStart()");
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
    	Log.d("image","onResume()");
    	//setTitle("count:"+count+",flag:"+flag);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	
    	if( event.getAction() == MotionEvent.ACTION_DOWN){count++;
    	
        Intent it = new Intent();it.setAction(Intent.ACTION_SEND);
        it.setType("text/plain"); 
//        it.setData(Uri.parse("mailto:" + "yamada@com"));
//        it.putExtra(Intent.EXTRA_SUBJECT, "わーい");
//        it.putExtra(Intent.EXTRA_TEXT, "本文");
        startActivity(it);
//    		TextView e = (TextView)findViewById(R.id.Edit);
//    		e.append("\nRoute_Not_Found");
//    		
//			try {
//				new Thread(new Runnable() {
//					int count = 3;
//					
//					@Override
//					public void run() {
//						
//						while(true){
//		    	            Intent intent = new Intent();
//		    	            intent.setAction(Intent.ACTION_VIEW);
//		    	            intent.setData(Uri.parse("Files:"+"test.jpg"));
//		    	            intent.putExtra("SOURCE_ADDRESS",
//		    	            		"111.11.111.11");
//		    	            intent.putExtra("PACKAGE", "jp.ac.ehime_u.cite.udptest");
//		    	            intent.putExtra("ID", count);
//				            intent.setClassName(
//				                    "jp.ac.ehime_u.cite.remotecamera",
//				                    "jp.ac.ehime_u.cite.remotecamera.RemoteCameraActivity");
//		    	            context.startActivity(intent);
//		    	            
//		    	            //Toast.makeText(context, "Go!!", Toast.LENGTH_SHORT).show();
//		    	            
//		    	            count++;
//		    	            
//		    	            try {
//								Thread.sleep(3000);
//							} catch (InterruptedException e) {
//								// TODO 自動生成された catch ブロック
//								e.printStackTrace();
//							}
//						}
//					}
//				}).start();
//	
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//
    	}
    	return true;
    }
}