package com.ariasaproject.cmls;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

import static com.ariasaproject.cmls.MinerService.MSG_STATE_NONE;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_ONSTART;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_RUNNING;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_ONSTOP;

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

public class MainActivity extends AppCompatActivity implements Handler.Callback, ServiceConnection {
    
    static final String PREF_URL="URL";
    static final String PREF_PORT="PORT";
    static final String PREF_USER= "USER";
    static final String PREF_PASS= "PASS";
    static final String PREF_THREAD= "THREAD";
    
    static final String DEFAULT_URL="stratum+tcp://us2.litecoinpool.org";
    static final int DEFAULT_PORT=3333;
    static final String DEFAULT_USER="Ariasa.test";
    static final String DEFAULT_PASS="123";
    
    final static String KEY_CONSOLE_ITEMS = "console_log";
    static {
      System.loadLibrary("ext");
    }
    TextView tv_speed, tv_accepted, tv_rejected, tv_status;
    EditText et_serv, et_port, et_user, et_pass;
    SeekBar sb_thread;
    CheckBox cb_screen_awake;

    MinerService mService = null;
    static final int MSG_STATUS = 1;
    static final int STATUS_SPEED = 1;
    static final int STATUS_ACCEPTED = 2;
    static final int STATUS_REJECTED = 3;
    static final int STATUS_STATUS = 4;
    
    static final int MSG_CONSOLE = 2;
    
    static final int MSG_STATE = 3;
    
    private static int updateDelay = 400; // 0.4 sec
    
    private static final int MAX_LOG_COUNT = 25;
    private ArrayList<ConsoleItem> logList = new ArrayList<ConsoleItem>(MAX_LOG_COUNT);
    RecyclerView.Adapter adpt;
    
    final String unit = " hash/sec";
    final DecimalFormat df = new DecimalFormat("#.##");
    final Handler statusHandler = new Handler(Looper.getMainLooper(),
    );
    final Thread updateThread = new Thread (() -> {
            try {
                for (;;)	{
                    synchronized (mService) {
                        statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATE, mService.state, 0));
                        //Thread.sleep(updateDelay);
                        if (!mService.status.console.isEmpty()) {
                            for (ConsoleItem c : mService.status.console) {
                                logList.add(0, c);
                            }
                            while (logList.size() > MAX_LOG_COUNT)
                                logList.remove(logList.size() - 1);
                            mService.status.console.clear();
                            statusHandler.sendEmptyMessage(MSG_CONSOLE);
                        }
                        if (mService.status.new_speed) {
                            statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_SPEED, 0, mService.status.speed));
                            mService.status.new_speed = false;
                        }
                        if (mService.status.new_accepted) {
                            statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_ACCEPTED, 0, mService.status.accepted));
                            mService.status.new_accepted = false;
                        }
                        if (mService.status.new_rejected) {
                            statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_REJECTED, 0, mService.status.rejected));
                            mService.status.new_rejected = false;
                        }
                        if (mService.status.new_status) {
                            statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_STATUS, 0, mService.status.status));
                            mService.status.new_status = false;
                        }
                        mService.wait();
                    }
                }
            } catch (InterruptedException e) {}
        });
    public ServiceConnection sc = new ServiceConnection() {
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean serviceWasRunning = false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MinerService.class.getName().equals(service.service.getClassName())) {
                serviceWasRunning = true;
                break;
            }
        }
        if (!serviceWasRunning) {
            Intent intent = new Intent(this, MinerService.class);
            bindService(intent, sc, Context.BIND_AUTO_CREATE);
            startService(intent);
        }
        if (savedInstanceState != null) {
            logList = savedInstanceState.getParcelableArrayList(KEY_CONSOLE_ITEMS);
        }
        //text status
        tv_speed = (TextView) findViewById(R.id.status_textView_speed);
        tv_accepted = (TextView) findViewById(R.id.status_textView_accepted);
        tv_rejected = (TextView) findViewById(R.id.status_textView_rejected);
        tv_status = (TextView) findViewById(R.id.status_textView_status);
        //editable
        et_serv = (EditText) findViewById(R.id.server_et);
        et_port = (EditText) findViewById(R.id.port_et);
        et_user = (EditText) findViewById((R.id.user_et));
        et_pass = (EditText) findViewById(R.id.password_et);
        sb_thread = (SeekBar)findViewById(R.id.threadSeek);
        cb_screen_awake = (CheckBox) findViewById(R.id.settings_checkBox_keepscreenawake);
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        et_serv.setText(settings.getString(PREF_URL, DEFAULT_URL));
        et_port.setText(String.valueOf(settings.getInt(PREF_PORT, DEFAULT_PORT)));
        et_user.setText(settings.getString(PREF_USER, DEFAULT_USER));
        et_pass.setText(settings.getString(PREF_PASS, DEFAULT_PASS));
        final Window window = getWindow();
        cb_screen_awake.setChecked((window.getAttributes().flags&FLAG_KEEP_SCREEN_ON) != 0);
        cb_screen_awake.setOnCheckedChangeListener((cb, check) -> {
            if ((window.getAttributes().flags&FLAG_KEEP_SCREEN_ON) == 0) {
                window.addFlags(FLAG_KEEP_SCREEN_ON);
            } else {
                window.clearFlags(FLAG_KEEP_SCREEN_ON);
            }
        });
        int t = Runtime.getRuntime().availableProcessors();
        if (t < 1) t = 1;
        sb_thread.setMax(t);
        final TextView thread_view = (TextView)findViewById(R.id.thread_view);
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
        sb_thread.setProgress(settings.getInt(PREF_THREAD, 1)); //old
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
        //check feature 
        checkBatteryOptimizations();
    }
    
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
    
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        default: break;
        case MSG_STATUS: // status update
            switch (msg.arg1) {
            default: break;
            case STATUS_SPEED:
                tv_speed.setText(df.format((float)msg.obj)+unit);
                break;
            case STATUS_ACCEPTED:
                tv_accepted.setText(String.valueOf((long)msg.obj));
                break;
            case STATUS_REJECTED:
                tv_rejected.setText(String.valueOf((long)msg.obj));
                break;
            case STATUS_STATUS:
                tv_status.setText((String)msg.obj);
                break;
            }
            break;
        case MSG_CONSOLE: // console update
            adpt.notifyDataSetChanged();
            break;
        case MSG_STATE: // button mining update
            MiningStateUpdate(msg.arg1);
            break;
        }
        return true;
    }
    
    
    private static final int REQUEST_BATTERY_OPTIMIZATIONS = 1001;
    private void checkBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
            // Jika izin tidak diizinkan, tampilkan dialog untuk meminta izin
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_BATTERY_OPTIMIZATIONS);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        default: break;
        case REQUEST_BATTERY_OPTIMIZATIONS:
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                // Izin diberikan, lanjutkan dengan operasi normal
            } else {
                // Izin ditolak, berikan pengguna instruksi lebih lanjut atau tindakan yang sesuai
            }
            break;
        }
    }

    final StringBuilder sb = new StringBuilder();
    //int lastServiceState = -1;
    void MiningStateUpdate(int state)  {
        //if (state == lastServiceState) return;
        final Button b = (Button) findViewById(R.id.status_button_startstop);
        switch (state) {
        default: break;
        case MSG_STATE_NONE:
            b.setText(getString(R.string.main_button_start));
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
                editor.commit();
                
                mService.startMining(url,port,user,pass, sb_thread.getProgress());
            });
            b.setEnabled(true);
            b.setClickable(true);
            tv_speed.setText("0 hash/sec");
            tv_accepted.setText("0");
            tv_rejected.setText("0");
            tv_status.setText("Not Mining");
            break;
        case MSG_STATE_ONSTART:
            b.setText(getString(R.string.main_button_onstart));
            b.setOnClickListener(null);
            b.setEnabled(false);
            b.setClickable(false);
            break;
        case MSG_STATE_RUNNING:
            b.setText(getString(R.string.main_button_stop));
            b.setOnClickListener(v -> {
                mService.stopMining();
            });
            b.setEnabled(true);
            b.setClickable(true);
            break;
        case MSG_STATE_ONSTOP:
            b.setText(getString(R.string.main_button_onstop));
            b.setOnClickListener(null);
            b.setEnabled(false);
            b.setClickable(false);
            break;
        }
        //lastServiceState = state;
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
            adpt.notifyDataSetChanged();
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
        if (isFinishing()) {
            unbindService(sc);
            stopService(new Intent(this, MinerService.class));
        }
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


