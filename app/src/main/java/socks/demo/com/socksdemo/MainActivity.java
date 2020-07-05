package socks.demo.com.socksdemo;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName() ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //启动核心代理服务
        startCoreService();
    }

    /**
     * 启动APP核心服务
     */
    private void startCoreService(){
        // Android 8.0使用startForegroundService在前台启动新服务
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this,ProxyServer.class));
        } else{
            startService(new Intent(this,ProxyServer.class));
        }
    }
}
