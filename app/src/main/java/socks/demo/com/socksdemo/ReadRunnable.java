package socks.demo.com.socksdemo;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import socks.demo.com.socksdemo.tools.MultProxyPool;

/**
 * Created by 12852 on 2020/3/23.
 */

public class ReadRunnable implements Runnable{

    private static final String TAG = ReadRunnable.class.getSimpleName();

    private final InputStream inputStream ;

    private final OutputStream outputStream ;

    private MultProxyPool.SocketGroup socketGroup ;

    private int awaitTime = 0 ;

    int type ;

    public ReadRunnable(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public ReadRunnable(InputStream inputStream, OutputStream outputStream, MultProxyPool.SocketGroup socketGroup, int i) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.socketGroup = socketGroup ;
        this.type = i ;
    }

    @Override
    public void run() {
        int length = 0;
        byte[] data = new byte[1024] ;
        //读取交换的数据
        try {
            do{
                length = inputStream.read(data);
                outputStream.write(data, 0, length);
                outputStream.flush();
            }while (length > 0);
           /* while(true) {
                if(inputStream.available() > 0){
                    length = inputStream.read(data);
                    outputStream.write(data, 0, length);
                    outputStream.flush();

                }else{
                    awaitTime ++ ;
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(awaitTime > 15){
                        break;
                    }
                }

            }*/
            if(socketGroup != null){
                socketGroup.notifyReadFinish(type);
            }
            Log.i(TAG,String.format("read finish " ));
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
