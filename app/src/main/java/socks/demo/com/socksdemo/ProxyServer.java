package socks.demo.com.socksdemo;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import socks.demo.com.socksdemo.tools.MultProxyPool;

/**
 * 代理核心服务，运行在后台，监听端口
 */
public class ProxyServer extends Service {

    private static final String TAG = ProxyServer.class.getSimpleName() ;

    //默认监听端口
    private static final int LISTENER_PORT = 8090 ;

    //本地监听地址
    private InetSocketAddress localAddress ;

    private ServerSocket serverSocket ;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //如果API在26以上即版本为 O 则调用startForeground()方法启动服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setForegroundService();
        }
        try {
            initServerSocket();
            MultProxyPool.execute(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            Socket socket = serverSocket.accept();
                            MultProxyPool.execute(new SocketTask(socket));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            closeServerSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化socketServer，开启监听
     * @throws IOException
     */
    private void initServerSocket() throws IOException{
        if(localAddress == null){
            localAddress = new InetSocketAddress(LISTENER_PORT);
        }
        if(serverSocket == null){
            serverSocket = new ServerSocket();
            serverSocket.bind(localAddress);
        }
    }


    /**
     * 关闭监听器
     */
    private void closeServerSocket() throws IOException{
        if(serverSocket != null){
            serverSocket.close();
            serverSocket = null ;
        }
    }


    /**
     *通过通知启动服务
     */
    @TargetApi(Build.VERSION_CODES.O)
    public void  setForegroundService() {
        //设定的通知渠道名称
        String channelName = "代理服务";
        String channelId = "APP_SYSTEM_SERVICE";
        //设置通知的重要程度
        int importance = NotificationManager.IMPORTANCE_LOW;
        //构建通知渠道
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription("代理核心服务，保证代理正常工作");
        //在创建的通知渠道上发送通知
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher) //设置通知图标
                .setContentTitle("代理服务")//设置通知标题
                .setContentText("代理核心服务")//设置通知内容
                .setAutoCancel(false) //用户触摸时，自动关闭
                .setOngoing(true);//设置处于运行状态
        //向系统注册通知渠道，注册后不能改变重要性以及其他通知行为
        NotificationManager notificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
        //将服务置于启动状态 NOTIFICATION_ID指的是创建的通知的ID
        startForeground(0x99,builder.build());
    }

}
