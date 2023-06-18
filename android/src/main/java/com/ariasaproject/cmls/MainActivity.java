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

    int  baseThreadCount;

    boolean mBound = false;
    MinerService mService;

    public int curScreenPos=0;

    public ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName name, IBinder service) {
            MinerService.LocalBinder binder = (MinerService.LocalBinder) service;
            mService = binder.getService();
            mBound=true;
        }

        public void onServiceDisconnected(ComponentName name) {  mBound=false;   }
    };


    public void startMining() {
        mService.startMiner();
    }

    public void stopMining()
    {
        mService.stopMiner();

    }
    private static int updateDelay=1000;
    String unit = " h/s";
    public volatile  boolean firstRunFlag = true;
    public volatile  boolean ShutdownStarted = false;
    public volatile  boolean StartShutdown = false;

    final DecimalFormat df = new DecimalFormat("#.##");
    final Handler statusHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    final TextView txt_console = (TextView) findViewById(R.id.status_textView_console);
                    txt_console.setText(mService.cString);
                    txt_console.invalidate();
                    final TextView tv_speed = (TextView) findViewById(R.id.status_textView_speed);
                    tv_speed.setText(df.format(mService.speed)+unit);
                    final TextView txt_accepted = (TextView) findViewById(R.id.status_textView_accepted);
                    txt_accepted.setText(String.valueOf(mService.accepted));
                    final TextView txt_rejected = (TextView) findViewById(R.id.status_textView_rejected);
                    txt_rejected.setText(String.valueOf(mService.rejected));
                    final TextView txt_status = (TextView) findViewById(R.id.status_textView_status);
                    txt_status.setText(mService.status);
                }
            };
    final Runnable buttonUpdate = new Runnable() {
        @Override
        public void run() {
            Button btn = (Button) findViewById(R.id.status_button_startstop);
            if(mService.running) {
                btn.setText(getString(R.string.main_button_stop));
                btn.setEnabled(true);
            } else {
                btn.setText(getString(R.string.main_button_start));
                if (firstRunFlag) {
                    btn.setEnabled(true);
                    btn.setClickable(true);
                }
                else if (StartShutdown) {
                    btn.setEnabled(false);
                    btn.setClickable(false);
                    if (!ShutdownStarted) {
                        ShutdownStarted = true;
                        CpuMiningWorker worker = (CpuMiningWorker)mService.imw;
                        ThreadStatusAsyncTask threadWaiter = new ThreadStatusAsyncTask();
                        threadWaiter.execute(worker);
                    }
                }
            }
        }
    };
    final Thread updateThread = new Thread () {
            @Override
            public void run() {
                while (!mBound) {
                    try {
                        sleep(50);
                    } catch (InterruptedException e) {}
                }
                statusHandler.post(buttonUpdate);
                while (mBound)	{
                    try {
                        sleep(updateDelay);
                    } catch (InterruptedException e) {}
                    statusHandler.sendMessage(statusHandler.obtainMessage(0));
                    statusHandler.post(buttonUpdate);
                }
            }
        };

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
        updateThread.start();
    }
    public void StartStopMining(View v)  {
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
           startMining();
           firstRunFlag = false;
           b.setText(getString(R.string.main_button_stop));
       }
       else{
           stopMining();
           StartShutdown = true;
           b.setText(getString(R.string.status_button_start));
       }
    }

    public void setButton (boolean flag) {
        Button btn = (Button) findViewById(R.id.status_button_startstop);
        if (flag) {
            btn.setEnabled(true);
            btn.setClickable(true);
        } else {
            btn.setEnabled(false);
            btn.setClickable(false);
        }
    }
    public class ThreadStatusAsyncTask extends AsyncTask<CpuMiningWorker,Integer,Boolean> {
        @Override
        protected Boolean doInBackground(CpuMiningWorker... params) {
            long lastTime = System.currentTimeMillis();
            long currTime;
            while (params[0].getThreadsStatus()) {
                currTime = System.currentTimeMillis();
                double deltaTime = (double)(currTime-lastTime)/1000.0;
                if (deltaTime>15.0) {
                    params[0].ConsoleWrite("Still cooling down...");
                    lastTime = currTime;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                setButton(true);
                ShutdownStarted = false;
                StartShutdown = false;
                firstRunFlag = true;
                Toast.makeText(MainActivity.this,"Cooldown finished",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub

        super.onPause();
    }


    @Override
    protected void onResume() {
        //Toast.makeText(this, callNative(), Toast.LENGTH_SHORT).show();
        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        if (settings.getBoolean(PREF_BACKGROUND, DEFAULT_BACKGROUND))
        {
            TextView tv_background = (TextView) findViewById(R.id.status_textView_background);
            tv_background.setText("RUN IN BACKGROUND");
        }
        super.onResume();
    }



    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        if(updateThread.isAlive()) { updateThread.interrupt(); }

        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        if(!settings.getBoolean(PREF_BACKGROUND,DEFAULT_BACKGROUND ))
        {
            if (mService != null && mService.running == true) { stopMining(); }
            Intent intent = new Intent(getApplicationContext(), MinerService.class);
            stopService(intent);
        }

        try {
            unbindService(mConnection);
        } catch (RuntimeException e) {
            //unbindService generates a runtime exception sometimes
            //the service is getting unbound before unBindService is called
            //when the window is dismissed by the user, this is the fix
        }

        super.onStop();
    }
    public native String callNative();
}
