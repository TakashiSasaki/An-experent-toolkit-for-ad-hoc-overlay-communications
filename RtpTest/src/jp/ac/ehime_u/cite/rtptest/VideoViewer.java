package jp.ac.ehime_u.cite.rtptest;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoViewer extends Activity {

	private VideoView video;
	private MediaController mc;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.video);
        
        video = (VideoView) findViewById(R.id.videoView1);
        video.requestFocus();
        //コントローラーを設置したい場合はMediaControllerで。
        MediaController m = new MediaController(this);
        video.setMediaController(m);
        //ビデオの準備が整ったときはOnPreparedイベントが発生
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        	@Override
        	public void onPrepared(MediaPlayer mp) {
        		// TODO Auto-generated method stub
        		mp.start();
        		//mp.seekTo(100);
        		//mp.stop();
        	}
        });
        //ビデオの再生が完了したらOnCompletionイベントが発生
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
        	@Override
        	public void onCompletion(MediaPlayer mp) {
        		// TODO Auto-generated method stub
        		mp.stop();
        		mp.release();
        		mp = null;
        	}
        });
        try{
        	//再生したい動画ファイルのパスを設定
        	video.setVideoPath("/data/data/" + this.getPackageName() + "/files/test.mp4");
        }catch (Exception e) {
        	// TODO: handle exception
        }
        
//        Button button = (Button) findViewById(R.id.play);
//        button.setOnClickListener(new View.OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				// this.mcとはできない?thisは無名クラスを表すので。
//				// mcがstaticなら、Media.mcと参照できる。
//				RtpTestActivity.this.mc.show();
//			}
//		});
//        this.video = (VideoView)findViewById(R.id.video);
//        this.video.setVideoPath("/data/data/" + this.getPackageName() + "/files/test.mp4");
//        this.mc = new MediaController(this);
//        this.mc.setMediaPlayer(video);
//        this.video.setMediaController(mc);
//        this.video.requestFocus();
    }

    @Override
    public void onPause(){
    	
    	super.onPause();
    }
}