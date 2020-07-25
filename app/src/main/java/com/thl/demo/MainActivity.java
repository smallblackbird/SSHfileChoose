package com.thl.demo;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import com.thl.filechooser.FileChooser;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    SSHCONNECTION sshconnection=new SSHCONNECTION();
    static volatile int flag=0;//test if the information is right
    private static final String TAG = "MainActivity";
    private static String path = "";
    private static final String CDPHONE = "echo 0 > /sys/devices/platform/vcc5v0-host/state";//写数据到U盘
    private static final String CDWINDOW = "echo 1 > /sys/devices/platform/vcc5v0-host/state";//从U盘读数据到电脑
    private TextView result;

    public class TestConnectionThread extends Thread{
        @Override
        public void run(){
            if(!sshconnection.testLegal()){
                flag=1;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result =  findViewById(R.id.shellresult);
        result.setMovementMethod(ScrollingMovementMethod.getInstance());
        requestPermissins(new PermissionUtils.OnPermissionListener() {
            @Override
            public void onPermissionGranted() {
            }

            @Override
            public void onPermissionDenied(String[] deniedPermissions) {
                Toast.makeText(MainActivity.this, "未获取到存储权限", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissins(new PermissionUtils.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        FileChooser fileChooser = new FileChooser(MainActivity.this, new FileChooser.FileChoosenListener() {
                            @Override
                            public void onFileChoosen(String filePath) {
                                path = filePath;
                                ((TextView) findViewById(R.id.tv_msg)).setText(filePath);
                            }
                        });
                        fileChooser.open();
                    }

                    @Override
                    public void onPermissionDenied(String[] deniedPermissions) {
                        Toast.makeText(MainActivity.this, "未获取到存储权限", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tcpClient.connect("192.168.1.15", 6666);
            }
        });

        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tcpClient.disConnect();
                ((TextView) findViewById(R.id.is_connect)).setText("断开连接");
            }
        });

        findViewById(R.id.loginSSH).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sshconnection.init("root", "123456", "192.168.1.15", "22");
                try {
                    TestConnectionThread thread = new TestConnectionThread();
                    thread.start();
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (flag == 0) {
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "连接失败，请重试", Toast.LENGTH_LONG).show();
                    //change flag to original value
                    flag = 0;
                }
            }
        });
        if (flag == 0) {
            findViewById(R.id.cd_phone).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   Toast.makeText(MainActivity.this,"点击了",Toast.LENGTH_SHORT).show();
                   new Thread(new Runnable() {
                       @Override
                       public synchronized void run() {
                           sshconnection.init("root", "123456", "192.168.1.15", "22");
                           sshconnection.shellInit();
                           final ArrayList<String> msg=sshconnection.shellCommand(CDPHONE);
                           runOnUiThread(new Runnable() {
                               @Override
                               public void run() {
                                   Toast.makeText(MainActivity.this,"命令发送成功!",Toast.LENGTH_SHORT).show();
                                   result.setText("");
                                   for(String i:msg){
                                       result.append(i);
                                   }
                               }
                           });
                       }
                   }).start();
                }
            });
            findViewById(R.id.cd_window).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   Toast.makeText(MainActivity.this,"点击了",Toast.LENGTH_SHORT).show();
                    new Thread(new Runnable() {
                        @Override
                        public synchronized void run() {
                            sshconnection.init("root", "123456", "192.168.1.15", "22");
                            sshconnection.shellInit();
                            final ArrayList<String> msg=sshconnection.shellCommand(CDWINDOW);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this,"命令发送成功!",Toast.LENGTH_SHORT).show();
                                    result.setText("");
                                    for(String i:msg){
                                        result.append(i);
                                    }
                                }
                            });
                        }
                    }).start();
                }
            });
        }
        findViewById(R.id.btn_self).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sshconnection.shellEnd();
                Intent intent = new Intent(MainActivity.this,loginActivity.class);
                startActivity(intent);

            }
        });

    }

    private void requestPermissins(PermissionUtils.OnPermissionListener mOnPermissionListener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mOnPermissionListener.onPermissionGranted();
            return;
        }
        String[] permissions = { "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        PermissionUtils.requestPermissions(this, 0
                , permissions, mOnPermissionListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }


     private TcpClient tcpClient = new TcpClient(){
         @Override
         public void isConnect(boolean isconnect) {
            if(isconnect){
                ((TextView) findViewById(R.id.is_connect)).setText("已连接");
            }else {
                ((TextView) findViewById(R.id.is_connect)).setText("已经断开");
            }

         }

         @Override
         public void isUploadOver(boolean isOver) {
             if(isOver){
                 Toast.makeText(MainActivity.this,"上传完毕",Toast.LENGTH_SHORT).show();

             }
         }
     };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tcpClient.close();
        result.setText("");
        sshconnection.shellEnd();

    }



    @Override
    protected void onStart() {
        super.onStart();
        findViewById(R.id.start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tcpClient.start(path);
            }
        });
    }
}
