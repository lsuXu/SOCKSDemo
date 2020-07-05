package socks.demo.com.socksdemo.tools;

import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import socks.demo.com.socksdemo.ReadRunnable;

/**
 * Created by 12852 on 2020/6/14.
 */

public class MultProxyPool {

    private static final String TAG = MultProxyPool.class.getSimpleName();

    private static ExecutorService executors = Executors.newFixedThreadPool(30);

    /**
     * 交换数据
     * @param localSocket
     * @param remoteSocket
     * @throws IOException
     */
    public static void translatorData(Socket localSocket ,Socket remoteSocket ,boolean isHttp) throws IOException {

        SocketGroup socketGroup = new SocketGroup(localSocket,remoteSocket);
        executors.execute(new ReadRunnable(localSocket.getInputStream(), remoteSocket.getOutputStream(), socketGroup, 1));
        executors.execute(new ReadRunnable(remoteSocket.getInputStream(),localSocket.getOutputStream(),socketGroup,2));
    }

    /**
     * 使用线层池运行
     * @param runnable
     */
    public static void execute(Runnable runnable){
        executors.execute(runnable);
    }

    public static class SocketGroup{

        private boolean localFinish = false ,remoteFinish = false ;

        private Socket localSocket ;

        private Socket remoteSocket ;

        public SocketGroup(Socket localSocket, Socket remoteSocket) {
            this.localSocket = localSocket;
            this.remoteSocket = remoteSocket;
        }

        public void notifyReadFinish(int type){
            if(type == 1){
                localFinish = true ;
            }else if(type == 2){
                remoteFinish = true ;
            }

            if(localFinish && remoteFinish){
                try {
                    localSocket.close();
                    remoteSocket.close();
                    Log.i(TAG,String.format("release socket local : %s remote %s",localSocket.getPort(),remoteSocket.getLocalPort()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
