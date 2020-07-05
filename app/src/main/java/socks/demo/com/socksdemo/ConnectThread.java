package socks.demo.com.socksdemo;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import socks.demo.com.socksdemo.protocol.HttpImpl;
import socks.demo.com.socksdemo.protocol.HttpsImpl;
import socks.demo.com.socksdemo.protocol.Protocol;


public class ConnectThread extends Thread {

    private static final String TAG = ConnectThread.class.getSimpleName();

    private final Selector selector;

    private final int capacity = 120;

    public ConnectThread(Selector selector) {
        this.selector = selector;
    }

    @Override
    public void run() {
        super.run();
        try {
            while (selector.select() > 0) {
                Set<SelectionKey> keySelectors = selector.selectedKeys();
                Iterator<SelectionKey> keys = keySelectors.iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    //socket连接建立，创建一个远程客户端与之对应
                    if (key.isAcceptable()) {//新的连接建立请求
                        //获取客户端建立连接的请求，返回 SocketChannel 对象
                        SocketChannel localSocket = ((ServerSocketChannel) key.channel()).accept();
                        //设置为非阻塞状态
                        localSocket.configureBlocking(false);
                        //将SocketChannel注册到通道，可读时获取到的是SocketChannel对象
                        localSocket.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(capacity));

                    } else if (key.isReadable()) {//可读
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();
                        if (buffer != null) {
                            buffer.clear();
                            channel.read(buffer);
                            byte[] bytes = buffer.array();
                            String requestData = new String(bytes);
                            Socket socket = channel.socket();

                            Socket remote = getRemoteSocket(requestData,socket);
                            int port = socket.getPort();
                            Log.i(TAG, "receive data :" + requestData);
                            channel.configureBlocking(false);
                            channel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        } else {
                            channel.configureBlocking(false);
                            channel.register(key.selector(), SelectionKey.OP_WRITE);
                        }
                    } else if (key.isWritable()) {//可写
                        /*SocketChannel channel = (SocketChannel) key.channel();
                        //判断是否存在回话。若存在，不需要重复生成回话
                        Socket localSocket = channel.socket();
                        Socket remote = getSession(localSocket.getPort());
                        //读取数据
                        if (remote != null && remote.isConnected()) {
                            byte[] data = new byte[1024];
                            int length;
                            while ((length = remote.getInputStream().read(data)) > 0){
                                ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);
                                channel.write(buffer);
                            }
                            localSocket.close();
                            release(remote);
                        }else {
                            channel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                        }*/
                    }
                    keys.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取代理服务器连接
     *
     * @param requestData
     * @return
     * @throws IOException
     */
    private Socket getRemoteSocket(String requestData, final Socket localSocket) throws IOException {
        Socket remoteSocket = new Socket();
        remoteSocket.connect(new InetSocketAddress("47.102.125.88", 20000));
        //若连接建立，对照协议进行交互
        if (remoteSocket.isConnected()) {
            //根据第一包获取协议类型
            Protocol protocol = getProtocolImpl(requestData, localSocket, remoteSocket);
            //身份验证
            boolean checkIdentity = protocol.checkIdentity();
            if (!checkIdentity) {
                Log.e(TAG, "身份验证失败");
                return null;
            }
            //建立连接
            boolean checkConnect = protocol.checkConnect();
            if (!checkConnect) {
                Log.e(TAG, "远端连接建立失败");
                return null;
            }
            //完成连接
            protocol.finishConnect();
            //交换数据
            protocol.swapData();
        } else {
            release(localSocket);
        }
        return remoteSocket;
    }

    private void release(Socket socket) throws IOException {
        if (socket != null && socket.isConnected()) {
            socket.close();
        }
    }

    /**
     * 根据首包数据获取当前连接的方式
     *
     * @param requestData
     * @return
     */
    public Protocol getProtocolImpl(String requestData, Socket localSocket, Socket remoteSocket) {
        if (requestData.contains("CONNECT")) {
            return new HttpsImpl(remoteSocket, localSocket, requestData);
        } else {
            return new HttpImpl(remoteSocket, localSocket, requestData);
        }
    }

}