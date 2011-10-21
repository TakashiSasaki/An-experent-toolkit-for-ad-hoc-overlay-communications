
package jp.ks.quality;
import java.util.*;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.os.IBinder;
import android.widget.RemoteViews;
     
//ホームウィジェットを制御するサービス
public class AppWidgetService extends Service {
    private static final String ACTION_BTNCLICK =
        "jp.ks.quality.AppWidgetService.ACTION_BTNCLICK";
    
    //サービス開始時に呼ばれる
    @Override
    public void onStart(Intent intent,int startId) {
        super.onStart(intent, startId);
        
        //リモートビューの取得
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        RemoteViews view=new RemoteViews(getPackageName(),R.layout.appwidget);
        if (ACTION_BTNCLICK.equals(intent.getAction())) {
            btnClicked(view);
        }
         
        //button1とボタンクリックイベントの関連付け
        Intent newintent=new Intent();
        newintent.setAction(ACTION_BTNCLICK);
        PendingIntent pending=PendingIntent.getService(this,0,newintent,0);
        view.setOnClickPendingIntent(R.id.button1,pending);
        
        //ホームウィジェットの更新
        ComponentName widget=new ComponentName(this,Saikoro.class);
        manager.updateAppWidget(widget,view);
    }
     
    //バインダーを返す
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
     
    //ボタンクリック時に呼ばれる
    public void btnClicked(RemoteViews view){
        int[] ids={
            R.drawable.dice1,R.drawable.dice2,R.drawable.dice3,
            R.drawable.dice4,R.drawable.dice5,R.drawable.dice6};
        int idx=rand(6);
        view.setImageViewResource(R.id.imageview1,ids[idx]);
    }
    
    //乱数の取得
    private static Random rand=new Random();
    public static int rand(int num) {
        return (rand.nextInt()>>>1)%num;
    }    
}