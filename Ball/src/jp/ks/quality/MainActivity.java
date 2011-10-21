package jp.ks.quality;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // 無操作で暗くなるのを防ぐ
        Window window = getWindow();
        window.addFlags(
        	WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}