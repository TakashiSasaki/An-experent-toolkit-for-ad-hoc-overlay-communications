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
import android.widget.ImageView;
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
        
        if(count > 1){
        	flag += 1;
        	non_flag += 1;
        	
        	Log.d("image","finish()");
        	//finish();
        	
        }
        
        context = this;
        
        setContentView(R.layout.main);
        
        Button b = (Button)findViewById(R.id.button1);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setTitle("count:"+count+",flag:"+flag+",non_flag:"+non_flag);
			}
		});
		
		
		setTitle("count:"+count+",flag:"+flag);
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	
    	Log.d("image","onDestroy()");
    	count--;
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
    	Log.d("image","onResume()");
    	setTitle("count:"+count+",flag:"+flag);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	
    	if( event.getAction() == MotionEvent.ACTION_DOWN){
    		
            Intent intent = new Intent();
            intent.setClassName(
                    "jp.ac.ehime_u.cite.image",
                    "jp.ac.ehime_u.cite.image.ImageTestActivity");
            
            startActivity(intent);
    	}
    	return true;
    }
}