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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ariasaproject.cmls.MiningStatusService;
import com.ariasaproject.cmls.MiningStatusService.ConsoleItem;
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
import java.util.ArrayList;

import static android.R.id.edit;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static com.ariasaproject.cmls.Constants.DEFAULT_SCREEN;
import static com.ariasaproject.cmls.Constants.PREF_PASS;
import static com.ariasaproject.cmls.Constants.PREF_SCREEN;
import static com.ariasaproject.cmls.Constants.PREF_THREAD;
import static com.ariasaproject.cmls.Constants.PREF_URL;
import static com.ariasaproject.cmls.Constants.PREF_USER;

public class MainActivity extends AppCompatActivity {
    
    static final String DEFAULT_URL="stratum+tcp://us2.litecoinpool.org:3333";
    static final String DEFAULT_USER="Ariasa.test";
    static final String DEFAULT_PASS="123";
    /*
    static final String PREF_URL="URL";
    static final String PREF_USER= "USER";
    static final String PREF_PASS= "PASS";
    static final String PREF_THREAD= "THREAD";
    static final String PREF_THROTTLE = "THROTTLE";
    static final String PREF_SCANTIME = "SCANTIME";
    static final String PREF_RETRYPAUSE = "RETRYPAUSE";
    static final String PREF_DONATE = "DONATE";
    static final String PREF_SERVICE = "SERVICE";
    static final String PREF_TITLE="SETTINGS";
    static final String PREF_PRIORITY="PRIORITY";
    static final String PREF_SCREEN="SCREEN_AWAKE";
    */
    final static String KEY_CONSOLE_ITEMS = "console";
    static {
      System.loadLibrary("ext");
    }
    EditText et_serv;
    EditText et_user;
    EditText et_pass;
    CheckBox cb_screen_awake;

    int baseThreadCount;
    MinerService mService = null;

    public int curScreenPos=0;

    public ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            MinerService.LocalBinder binder = (MinerService.LocalBinder) service;
            mService = binder.getService();
            notifyAll();
        }
        @Override
        public synchronized void onServiceDisconnected(ComponentName name) {
            mService = null;
            notifyAll();
        }
    };
    private static int updateDelay = 400; // 0.4 sec
    public volatile  boolean firstRunFlag = true;
    public volatile  boolean ShutdownStarted = false;
    public volatile  boolean StartShutdown = false;
    
    final DecimalFormat df = new DecimalFormat("#.##");
    private Thread updateThread;
    private static final int MAX_LOG_COUNT = 25;
    private ArrayList<ConsoleItem> logList = new ArrayList<ConsoleItem>(MAX_LOG_COUNT);
    
    int threads_use = 1;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Intent intent = new Intent(getApplicationContext(), MinerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (savedInstanceState != null) {
            logList = savedInstanceState.getParcelableArrayList(KEY_CONSOLE_ITEMS);
        }
        et_serv = (EditText) findViewById(R.id.server_et);
        et_user = (EditText) findViewById((R.id.user_et));
        et_pass = (EditText) findViewById(R.id.password_et);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        et_serv.setText(settings.getString(PREF_URL, DEFAULT_URL));
        et_user.setText(settings.getString(PREF_USER, DEFAULT_USER));
        et_pass.setText(settings.getString(PREF_PASS, DEFAULT_PASS));
        cb_screen_awake = (CheckBox) findViewById(R.id.settings_checkBox_keepscreenawake) ;
        cb_screen_awake.setChecked(DEFAULT_SCREEN);
        final SeekBar sb = (SeekBar)findViewById(R.id.threadSeek);
        sb.setMax(1);
        try {
            int t = Runtime.getRuntime().availableProcessors();
            final TextView sbT = (TextView)findViewById(R.id.thread_view);
            sb.setMax(t);
            sb.setProgress(settings.getInt(PREF_THREAD, 1)); //old
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int p = 1;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }
            
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
            
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    sbT.setText(String.valueOf(p = progress));
                    threads_use = p;
                }
            });
        }
        catch (Exception e){ }
        
        final RecyclerView consoleView = (RecyclerView)findViewById(R.id.console_view);
        consoleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        final RecyclerView.Adapter adpt = new RecyclerView.Adapter<ConsoleItemHolder>() {
            final LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
            @Override
            public ConsoleItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View itemView = inflater.inflate(R.layout.console_item, parent, false);
                return new ConsoleItemHolder(itemView);
            }
            @Override
            public void onBindViewHolder(ConsoleItemHolder holder, int position) {
                ConsoleItem c = logList.get(position);
                holder.bindLog(c.time, c.msg);
            }
            @Override
            public int getItemCount() {
                return logList.size();
            }
        };
        consoleView.setAdapter(adpt);
        
        //ui update threads
        final Handler.Callback statusHandlerCallback = new Handler.Callback() {
            final String unit = " h/s";
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    default: // ui update
                        synchronized (mService.status) {
                            if (!mService.status.console.isEmpty()) {
                                for (ConsoleItem c : mService.status.console) {
                                    logList.add(0, c);
                                }
                                while (logList.size() > MAX_LOG_COUNT)
                                    logList.remove(logList.size() - 1);
                                mService.status.console.clear();
                                adpt.notifyDataSetChanged();
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
                try {
                    synchronized (mConnection){
                        while (mService == null) mConnection.wait();
                    }
                    while (mService != null)	{
                        statusHandler.sendEmptyMessage(1);
                        if (!firstRunFlag && StartShutdown && !ShutdownStarted) {
                            ShutdownStarted = true;
                            mService.console.write("Cooling down...");
                            while (mService.running)
                                Thread.sleep(10);
                            statusHandler.sendEmptyMessage(2);
                        }
                        Thread.sleep(updateDelay);
                        if(mService.status.hasNew())
                            statusHandler.sendEmptyMessage(0);
                    }
                } catch (InterruptedException e) {}
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
           SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
           SharedPreferences.Editor editor = settings.edit();
           editor.putString(PREF_URL, url);
           editor.putString(PREF_USER, user);
           editor.putString(PREF_PASS, pass);
           editor.putInt(PREF_THREAD, threads_use);
           editor.putBoolean(PREF_SCREEN, cb_screen_awake.isChecked());
           editor.commit();
           
           if(settings.getBoolean(PREF_SCREEN, DEFAULT_SCREEN)) {
               getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
           }
           mService.startMiner(url, user, pass, threads_use);
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
        super.onStart();
        if(!updateThread.isAlive()) updateThread.start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_CONSOLE_ITEMS, logList);
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_CONSOLE_ITEMS)) {
            logList = savedInstanceState.getParcelableArrayList(KEY_CONSOLE_ITEMS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(updateThread.isAlive()) { updateThread.interrupt(); }
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
    public static class ConsoleItemHolder extends RecyclerView.ViewHolder {
        private TextView time;
        private TextView msg;
    
        public ConsoleItemHolder(View itemView) {
            super(itemView);
            time = itemView.findViewById(R.id.text1);
            msg = itemView.findViewById(R.id.text2);
        }
        public void bindLog(String t, String m) {
            time.setText(t);
            msg.setText(m);
        }
    }
}


