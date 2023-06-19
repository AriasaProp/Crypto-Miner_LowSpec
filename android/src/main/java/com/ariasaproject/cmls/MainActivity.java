package com.ariasaproject.cmls;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.stratum.StratumSocket;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import static android.R.id.edit;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.ariasaproject.cmls.Constants.DEFAULT_BACKGROUND;
import static com.ariasaproject.cmls.Constants.DEFAULT_SCREEN;
import static com.ariasaproject.cmls.Constants.PREF_BACKGROUND;
import static com.ariasaproject.cmls.Constants.PREF_NEWS_RUN_ONCE;
import static com.ariasaproject.cmls.Constants.PREF_PASS;
import static com.ariasaproject.cmls.Constants.PREF_SCREEN;
import static com.ariasaproject.cmls.Constants.PREF_THREAD;
import static com.ariasaproject.cmls.Constants.PREF_TITLE;
import static com.ariasaproject.cmls.Constants.PREF_URL;
import static com.ariasaproject.cmls.Constants.PREF_USER;

public class MainActivity extends AppCompatActivity {
    static {
      System.loadLibrary("ext");
    }
    EditText et_serv;
    EditText et_user;
    EditText et_pass;
    CheckBox cb_service;
    CheckBox cb_screen_awake;

    int baseThreadCount;
    MinerService mService = null;

    public int curScreenPos=0;

    public ServiceConnection mConnection = new ServiceConnection() {

        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            MinerService.LocalBinder binder = (MinerService.LocalBinder) service;
            mService = binder.getService();
            notifyAll();
        }

        public synchronized void onServiceDisconnected(ComponentName name) {
            mService = null;
            notifyAll();
        }
    };
    private static int updateDelay = 500;
    public volatile  boolean firstRunFlag = true;
    public volatile  boolean ShutdownStarted = false;
    public volatile  boolean StartShutdown = false;
    
    final DecimalFormat df = new DecimalFormat("#.##");
    private Thread updateThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Intent intent = new Intent(getApplicationContext(), MinerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        et_serv = (EditText) findViewById(R.id.server_et);
        et_user = (EditText) findViewById((R.id.user_et));
        et_pass = (EditText) findViewById(R.id.password_et);
        cb_service = (CheckBox) findViewById(R.id.settings_checkBox_background) ;
        cb_service.setChecked(DEFAULT_BACKGROUND);
        cb_screen_awake = (CheckBox) findViewById(R.id.settings_checkBox_keepscreenawake) ;
        cb_screen_awake.setChecked(DEFAULT_SCREEN);
        try {
            int t = Runtime.getRuntime().availableProcessors();
            String[] threadsAvailable = new String[t];
            for(int i = 0; i < t; i++) {
                threadsAvailable[i] = Integer.toString(i+1);
            }
            ((Spinner)findViewById(R.id.spinner1)).setAdapter(new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, threadsAvailable));
        }
        catch (Exception e){ }
        
        
        //ui update threads
        final Handler.Callback statusHandlerCallback = new Handler.Callback() {
            final String unit = " h/s";
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    default: // ui update
                        synchronized (mService.status) {
                            if (mService.status.new_console) {
                                final TextView txt_console = (TextView) findViewById(R.id.status_textView_console);
                                txt_console.setText(mService.status.console);
                                txt_console.invalidate();
                                mService.status.new_console = false;
                            }
                            if (mService.status.new_speed) {
                                final TextView tv_speed = (TextView) findViewById(R.id.status_textView_speed);
                                tv_speed.setText(df.format(mService.status.speed)+unit);
                                mService.status.new_speed = false;
                            }
                            if (mService.status.new_accepted) {
                                final TextView txt_accepted = (TextView) findViewById(R.id.status_textView_accepted);
                                txt_accepted.setText(String.valueOf(mService.status.accepted));
                                mService.status.new_accepted = false;
                            }
                            if (mService.status.new_rejected) {
                                final TextView txt_rejected = (TextView) findViewById(R.id.status_textView_rejected);
                                txt_rejected.setText(String.valueOf(mService.status.rejected));
                                mService.status.new_rejected = false;
                            }
                            if (mService.status.new_status) {
                                final TextView txt_status = (TextView) findViewById(R.id.status_textView_status);
                                txt_status.setText(mService.status.status);
                                mService.status.new_status = false;
                            }
                        }
                        break;
                    case 1: {// button mining update
                        final Button btn = (Button) findViewById(R.id.status_button_startstop);
                        if(mService.running) {
                            btn.setText(getString(R.string.main_button_stop));
                            btn.setEnabled(true);
                        } else {
                            btn.setText(getString(R.string.main_button_start));
                            if (firstRunFlag) {
                                btn.setEnabled(true);
                                btn.setClickable(true);
                            } else if (StartShutdown) {
                                btn.setEnabled(false);
                                btn.setClickable(false);
                            }
                        }
                        break;
                    }
                    case 2: {// button mining after Shutdown
                        final Button btn = (Button) findViewById(R.id.status_button_startstop);
                        btn.setEnabled(true);
                        btn.setClickable(true);
                        ShutdownStarted = false;
                        StartShutdown = false;
                        firstRunFlag = true;
                        Toast.makeText(MainActivity.this,"Cooldown finished",Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                return true;
            }
        };
        final Handler statusHandler = new Handler(Looper.getMainLooper(), statusHandlerCallback);
        updateThread = new Thread (new Runnable() {
            @Override
            public void run() {
                synchronized (mConnection){
                    try {
                        while (mService == null) mConnection.wait();
                    } catch (InterruptedException e) {}
                }
                while (mService != null)	{
                    statusHandler.sendEmptyMessage(1);
                    if (!firstRunFlag && StartShutdown && !ShutdownStarted) {
                        ShutdownStarted = true;
                        CpuMiningWorker worker = (CpuMiningWorker)mService.imw;
                        worker.ConsoleWrite("Cooling down...");
                        while (worker.getThreadsStatus()) Thread.sleep(10);
                        statusHandler.sendEmptyMessage(2);
                    }
                    try {
                        Thread.sleep(updateDelay);
                    } catch (InterruptedException e) {}
                    statusHandler.sendEmptyMessage(0);
                }
            }
        });
    }
    public void StartStopMining(View v)  {
        if (mService == null) return;
        final Button b = (Button) v;
        if (b.getText().equals(getString(R.string.status_button_start))){
           StringBuilder sb= new StringBuilder();
           sb = new StringBuilder();
           String url = sb.append(et_serv.getText()).toString();
           sb.setLength(0);
           String user = sb.append(et_user.getText()).toString();
           sb.setLength(0);
           String pass = sb.append(et_pass.getText()).toString();
           sb.setLength(0);
        
           Spinner threadList = (Spinner)findViewById(R.id.spinner1);
        
           int threads = Integer.parseInt(threadList.getSelectedItem().toString());
        
           SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
           SharedPreferences.Editor editor = settings.edit();
           settings = getSharedPreferences(PREF_TITLE, 0);
           editor = settings.edit();
           editor.putString(PREF_URL, url);
           editor.putString(PREF_USER, user);
           editor.putString(PREF_PASS, pass);
           editor.putInt(PREF_THREAD, threads);
           editor.putBoolean(PREF_BACKGROUND, cb_service.isChecked());
           editor.putBoolean(PREF_SCREEN, cb_screen_awake.isChecked());
           editor.commit();
           if(settings.getBoolean(PREF_SCREEN,DEFAULT_SCREEN )) {
               getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
           }
           mService.startMiner();
           firstRunFlag = false;
           b.setText(getString(R.string.main_button_stop));
        } else{
           mService.stopMiner();
           StartShutdown = true;
           b.setText(getString(R.string.status_button_start));
        }
    }
    
    @Override
    protected void onStart() {
        Toast.makeText(this,"onStart",Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        Toast.makeText(this,"onResume",Toast.LENGTH_SHORT).show();
        updateThread.start();
        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        if (settings.getBoolean(PREF_BACKGROUND, DEFAULT_BACKGROUND)) {
            TextView tv_background = (TextView) findViewById(R.id.status_textView_background);
            tv_background.setText("RUN IN BACKGROUND");
        }
        Toast.makeText(this,"Resumed",Toast.LENGTH_SHORT).show();
        super.onResume();
    }

    @Override
    protected void onPause() {
        if(updateThread.isAlive()) { updateThread.interrupt(); }
        super.onPause();
    }

    @Override
    protected void onStop() {
        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        if(!settings.getBoolean(PREF_BACKGROUND,DEFAULT_BACKGROUND )) {
            if (mService != null && mService.running == true) { mService.stopMiner(); }
            Intent intent = new Intent(getApplicationContext(), MinerService.class);
            stopService(intent);
        }
        try {
            unbindService(mConnection);
        } catch (RuntimeException e) {}

        super.onStop();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public native String callNative();
}
