package com.yeelight.testnewux;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.os.AsyncTask;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static android.content.ContentValues.TAG;


public class ControlActivity extends AppCompatActivity {
    private String TAG = "Control";

    private static final int MSG_CONNECT_SUCCESS = 0;
    private static final int MSG_CONNECT_FAILURE = 1;
    private static final String CMD_TOGGLE = "{\"id\":%id,\"method\":\"toggle\",\"params\":[]}\r\n" ;
    private static final String CMD_ON = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"on\",\"smooth\",500]}\r\n" ;
    private static final String CMD_OFF = "{\"id\":%id,\"method\":\"set_power\",\"params\":[\"off\",\"smooth\",500]}\r\n" ;
    private static final String CMD_CT = "{\"id\":%id,\"method\":\"set_ct_abx\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_HSV = "{\"id\":%id,\"method\":\"set_hsv\",\"params\":[%value, 100, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 200]}\r\n";
    private static final String CMD_BRIGHTNESS_SCENE = "{\"id\":%id,\"method\":\"set_bright\",\"params\":[%value, \"smooth\", 500]}\r\n";
    private static final String CMD_COLOR_SCENE = "{\"id\":%id,\"method\":\"set_scene\",\"params\":[\"cf\",1,0,\"100,1,%color,1\"]}\r\n";

    private int mCmdId;
    private Socket mSocket;
    private String mBulbIP;
    private int mBulbPort;
    private ProgressDialog mProgressDialog;
    private SeekBar mBrightness;
    private SeekBar mCT;
    private SeekBar mColor;
    private Button mBtnOn;
    private Button mBtnOff;
    private Button mBtnMusic;
    private BufferedOutputStream mBos;
    private BufferedReader mReader;
    private Handler mHandler = new Handler(){
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_CONNECT_FAILURE:
                    mProgressDialog.dismiss();
                    break;
                case MSG_CONNECT_SUCCESS:
                    mProgressDialog.dismiss();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        mBulbIP = getIntent().getStringExtra("ip");
        mBulbPort = Integer.parseInt(getIntent().getStringExtra("port"));
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Connecting...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        mBrightness = (SeekBar) findViewById(R.id.brightness);
        mColor = (SeekBar) findViewById(R.id.color);
        mCT = (SeekBar) findViewById(R.id.ct);
        mCT.setMax(4800);
        mColor.setMax(360);
        mBrightness.setMax(100);

        mBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                write(parseBrightnessCmd(seekBar.getProgress()));
            }
        });
        mCT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                write(parseCTCmd(seekBar.getProgress() + 1700));
            }
        });
        mColor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                write(parseColorCmd(seekBar.getProgress()));
            }
        });
        mBtnOn = (Button) findViewById(R.id.btn_on);
        mBtnOff = (Button) findViewById(R.id.btn_off);
        mBtnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(true));
            }
        });
        mBtnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                write(parseSwitch(false));
            }
        });
        connect();
    }

    private boolean cmd_run = true;
    private void connect(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cmd_run = true;
                    mSocket = new Socket(mBulbIP, mBulbPort);
                    mSocket.setKeepAlive(true);
                    mBos= new BufferedOutputStream(mSocket.getOutputStream());
                    mHandler.sendEmptyMessage(MSG_CONNECT_SUCCESS);
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                    while (cmd_run){
                        try {
                            String value = mReader.readLine();
                            Log.d(TAG, "value = "+value);
                        }catch (Exception e){

                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.sendEmptyMessage(MSG_CONNECT_FAILURE);
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            cmd_run = false;
            if (mSocket!=null)
                mSocket.close();
        }catch (Exception e){

        }

    }
    private String parseSwitch(boolean on){
        String cmd;
        if (on){
            cmd = CMD_ON.replace("%id", String.valueOf(++mCmdId));
        }else {
            cmd = CMD_OFF.replace("%id", String.valueOf(++mCmdId));
        }
        return cmd;
    }
    private String parseCTCmd(int ct){
        return CMD_CT.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(ct+1700));
    }
    private String parseColorCmd(int color){
        return CMD_HSV.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(color));
    }
    private String parseBrightnessCmd(int brightness){
        return CMD_BRIGHTNESS.replace("%id",String.valueOf(++mCmdId)).replace("%value",String.valueOf(brightness));
    }
    private void write(String cmd){
        if (mBos != null && mSocket.isConnected()){
/*
 To fix the `android.os.StrictMode$AndroidBlockGuardPolicy.onNetwork` error this should be removed
             and replaced with async call
            try {
                mBos.write(cmd.getBytes());
                mBos.flush();
            } catch (Exception e){
                e.printStackTrace();
            }
*/
            AsyncTask<String, Integer, Void> task = new SocketWriteTask(mBos, cmd).execute();
        } else {
            Log.d(TAG,"mBos = null or mSocket is closed");
        }
    }

}

class SocketWriteTask extends AsyncTask<String, Integer, Void> {
    String Cmd;
    BufferedOutputStream mBos;

    public SocketWriteTask(BufferedOutputStream bos, String cmd) {
        mBos = bos;
        Cmd = cmd;
    }

    @Override
    protected Void doInBackground(String... dataArray) {
        if (dataArray.length != 0) {
            Log.e(TAG, "wrong number of arguments to SocketWriteTask!");
            return null;
        }
        try {
            mBos.write(Cmd.getBytes());
            mBos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}