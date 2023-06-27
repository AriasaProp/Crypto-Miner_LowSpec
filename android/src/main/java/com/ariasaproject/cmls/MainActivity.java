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

import static com.ariasaproject.cmls.MinerService.MINING_NONE;
import static com.ariasaproject.cmls.MinerService.MINING_ONSTART;
import static com.ariasaproject.cmls.MinerService.MINING_RUNNING;
import static com.ariasaproject.cmls.MinerService.MINING_ONSTOP;

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

public class MainActivity extends AppCompatActivity {
    static final boolean DEFAULT_SCREEN = true;
    
    static final String PREF_URL="URL";
    static final String PREF_PORT="PORT";
    static final String PREF_USER= "USER";
    static final String PREF_PASS= "PASS";
    static final String PREF_THREAD= "THREAD";
    static final String PREF_SCREEN="SCREEN_AWAKE";
    /*
    static final String PREF_THROTTLE = "THROTTLE";
    static final String PREF_SCANTIME = "SCANTIME";
    static final String PREF_RETRYPAUSE = "RETRYPAUSE";
    static final String PREF_PRIORITY="PRIORITY";
    */
    final static String KEY_CONSOLE_ITEMS = "console";
    static {
      System.loadLibrary("ext");
    }
    EditText et_serv, et_port, et_user, et_pass;
    SeekBar sb_thread;
    CheckBox cb_screen_awake;

    public ServiceConnection mConnection = new ServiceConnection() {
        MinerService mService = null;
        final String unit = " h/s";
        final DecimalFormat df = new DecimalFormat("#.##");
        final Handler statusHandler = new Handler(Looper.getMainLooper(), msg -> {
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
            case 1: // button mining update
                MainActivity.this.MiningStateUpdate(mService.state);
                break;
            }
            return true;
        });
        final Thread updateThread = new Thread (() -> {
            try {
                for (;;)	{
                    statusHandler.sendEmptyMessage(1);
                    Thread.sleep(updateDelay);
                    if(mService.status.hasNew())
                        statusHandler.sendEmptyMessage(0);
                }
            } catch (InterruptedException e) {}
        });
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MinerService.LocalBinder binder = (MinerService.LocalBinder) service;
            mService = binder.getService();
            if (!updateThread.isAlive()) updateThread.start();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            updateThread.interrupt();
        }
    };
    private static int updateDelay = 400; // 0.4 sec
    
    private static final int MAX_LOG_COUNT = 25;
    private ArrayList<ConsoleItem> logList = new ArrayList<ConsoleItem>(MAX_LOG_COUNT);
    RecyclerView.Adapter adpt;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Intent intent = new Intent(this, MinerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        if (savedInstanceState != null) {
            logList = savedInstanceState.getParcelableArrayList(KEY_CONSOLE_ITEMS);
        }
        et_serv = (EditText) findViewById(R.id.server_et);
        et_port = (EditText) findViewById(R.id.port_et);
        et_user = (EditText) findViewById((R.id.user_et));
        et_pass = (EditText) findViewById(R.id.password_et);
        sb_thread = (SeekBar)findViewById(R.id.threadSeek);
        final TextView thread_view = (TextView)findViewById(R.id.thread_view);
        cb_screen_awake = (CheckBox) findViewById(R.id.settings_checkBox_keepscreenawake);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        et_serv.setText(settings.getString(PREF_URL, ""));
        et_port.setText(String.valueOf(settings.getInt(PREF_PORT, 0)));
        et_user.setText(settings.getString(PREF_USER, ""));
        et_pass.setText(settings.getString(PREF_PASS, ""));
        cb_screen_awake.setChecked(settings.getBoolean(PREF_SCREEN, false));
        int t = Runtime.getRuntime().availableProcessors();
        sb_thread.setMax(t);
        sb_thread.setProgress(settings.getInt(PREF_THREAD, 1)); //old
        thread_view.setText(String.format("%02d", sb_thread.getProgress()));
        sb_thread.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                thread_view.setText(String.format("%02d", progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
        
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        //log Adapter
        final RecyclerView consoleView = (RecyclerView)findViewById(R.id.console_view);
        consoleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        adpt = new RecyclerView.Adapter<ConsoleItemHolder>() {
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
        
    }
    final StringBuilder sb = new StringBuilder();
    void MiningStateUpdate(int state)  {
        final Button b = (Button) findViewById(R.id.status_button_startstop);
        switch (state) {
        default: break;
        case MINING_NONE:
            b.setText(getString(R.string.main_button_start));
            b.setEnabled(true);
            b.setClickable(true);
            b.setOnClickListener(v -> {
               String url = sb.append(et_serv.getText()).toString();
               sb.setLength(0);
               int port = Integer.parseInt(et_port.getText().toString());
               sb.setLength(0);
               String user = sb.append(et_user.getText()).toString();
               sb.setLength(0);
               String pass = sb.append(et_pass.getText()).toString();
               sb.setLength(0);
               SharedPreferences settings = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
               SharedPreferences.Editor editor = settings.edit();
               editor.putString(PREF_URL, url);
               editor.putInt(PREF_PORT, port);
               editor.putString(PREF_USER, user);
               editor.putString(PREF_PASS, pass);
               editor.putInt(PREF_THREAD, sb_thread.getProgress());
               editor.putBoolean(PREF_SCREEN, cb_screen_awake.isChecked());
               editor.commit();
               
               if(settings.getBoolean(PREF_SCREEN, DEFAULT_SCREEN)) {
                   MainActivity.this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
               }
               Intent intent = new Intent(MainActivity.this, MinerService.class);
               intent.putExtra(PREF_URL, url);
               intent.putExtra(PREF_PORT, port);
               intent.putExtra(PREF_USER, user);
               intent.putExtra(PREF_PASS, pass);
               intent.putExtra(PREF_THREAD, sb_thread.getProgress());
               startService(intent);
            });
            break;
        case MINING_ONSTART:
            b.setText(getString(R.string.main_button_onstart));
            b.setEnabled(false);
            b.setClickable(false);
            b.setOnClickListener(null);
            break;
        case MINING_RUNNING:
            b.setEnabled(true);
            b.setClickable(true);
            b.setText(getString(R.string.main_button_stop));
            b.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, MinerService.class);
                stopService(intent);
            });
            break;
        case MINING_ONSTOP:
            b.setText(getString(R.string.main_button_onstop));
            b.setEnabled(false);
            b.setClickable(false);
            b.setOnClickListener(null);
            break;
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
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


