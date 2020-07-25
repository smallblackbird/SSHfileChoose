package com.thl.demo;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class TcpClient {
    private static final String TAG = "TcpClient";
    private Process process = null;
    private DataOutputStream os = null;
    private DataInputStream in = null;



    public  TcpClient (){

    }

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private SocketThread mSocketThread;
    private ReadThread mStart;
    private boolean isStop = false;//thread flag
    private  boolean isConnet = false;
    private boolean isKeepLive = false;
    private  boolean isUpOver = false;
    private static final int CONNECT_SUCCESS = 1001;
    private static final int OVER_SUCCESS = 1002;
    private SocketThread socketThread;
    private ReadThread readThread;

    private final ExecutorService exec = new ThreadPoolExecutor(4,10,0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingDeque<Runnable>(16), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());


    /**
     * 128 - 数据按照最长接收，一次性
     * */
    private class SocketThread extends Thread {
        private String ip;
        private int port;
        public SocketThread(String ip, int port){
            this.ip = ip;
            this.port = port;
        }
        @Override
        public void run() {
            Log.d(TAG,"SocketThread start ");

               try {
                    if(mSocket!=null) {
                        mSocket.close();
                        mSocket = null;
                    }
                   InetAddress inetAddress = InetAddress.getByName(ip);
                   //connect
                   Log.d(TAG, "___________________");
                   Log.d(TAG, "run: 正在连接");

                        mSocket = new Socket(inetAddress, port);
                        Log.d(TAG, ip+port);
                        mSocket.setSoTimeout(1000);
                        //设置不延时发送
                        mSocket.setTcpNoDelay(true);
                        //设置输入输出缓冲流大小
                        mSocket.setSendBufferSize(8 * 1024);
                        mSocket.setReceiveBufferSize(8 * 1024);

                        mSocket.setReuseAddress(true);
                        if (mSocket.isConnected()) {
                            mOutputStream = mSocket.getOutputStream();
                            mInputStream = mSocket.getInputStream();
                            isConnet = true;
                            isStop = true;
                            Log.d(TAG, "连接成功 ");
                            handler.sendEmptyMessage(CONNECT_SUCCESS);
                        }
                       // Thread.sleep(1000);
                        //SystemClock.sleep(1000);



                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }

    private class ReadThread extends Thread{
        private String path;
        public ReadThread(String paths){
            this.path = paths;
        }
        @Override
        public void run() {
            try {
                File file = new File(path);
                Log.d(TAG, file.exists() + "");
                InputStream reader = new FileInputStream(file);
                int readbyte = 0;
                byte[] buf = null;
                if((file.length()>0)&&(file.length()<1024*100)){
                    buf = new byte[1024];
                } else if((file.length()>=1024*100)&&(file.length()<1024*1000)){
                    buf = new byte[1024*3];
                }else if((file.length()>=1024*1000)&&(file.length()<1024*10000)) {
                    buf = new byte[1024 * 30];
                }else {
                    buf = new byte[1024 * 50];
                }

                while ((readbyte = reader.read(buf)) != -1) {
                    Log.d(TAG, "readbyte" + readbyte);
                    mOutputStream.write(buf);
                    mOutputStream.flush();
                }
                //mOutputStream.flush();

                if (readbyte == -1) {
                    Log.d(TAG, "复制完毕");
                   handler.sendEmptyMessage(OVER_SUCCESS);

                }

                reader.close();
                mOutputStream.close();
                mInputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    private  Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
           switch (msg.what){
               case CONNECT_SUCCESS:
                   isConnet = mSocket.isConnected();
                   isConnect(isConnet);
                   break;
               case OVER_SUCCESS:
                   isUpOver = true;
                   isUploadOver(isUpOver);
                   break;
                   default:
                       break;
           }
        }
    };

    public void connect(String ip, int port){
        exec.submit(new SocketThread(ip,port));


    }


    public void disConnect(){
        try {
            if(mSocket!=null){
                mSocket.close();
                mSocket = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        exec.shutdownNow();
    }

    public void start(String path){
        isStop = false;
        exec.submit(new ReadThread(path));

    }


    public  void isConnect(boolean isconnect){}
    public  void isUploadOver(boolean isOver){}

}
