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
import android.os.Parcelable;
import android.os.Parcel;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.AppCompatEditText;

import static android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;


import static com.ariasaproject.cmls.MinerService.MSG_STATE_NONE;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_ONSTART;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_RUNNING;
import static com.ariasaproject.cmls.MinerService.MSG_STATE_ONSTOP;

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

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    
    static final String PREF_URL = "URL";
    static final String PREF_PORT = "PORT";
    static final String PREF_USER = "USER";
    static final String PREF_PASS = "PASS";
    static final String PREF_THREAD = "THREAD";
    static final String PREF_CONSOLE = "CONSOLE";
    
    static final String DEFAULT_URL="stratum+tcp://us2.litecoinpool.org";
    static final int DEFAULT_PORT=3333;
    static final String DEFAULT_USER="Ariasa.test";
    static final String DEFAULT_PASS="1234";
    
    private static final String KEYBUNDLE_CONSOLE = "bundle_console";
    private static final String KEYBUNDLE_TEXTS = "bundle_texts";
    private static final String KEYBUNDLE_INTS = "bundle_ints";
    
    static {
      System.loadLibrary("ext");
    }
    
    ViewGroup input_container, status_container;
    
    AppCompatTextView tv_speed, tv_accepted, tv_rejected;
    AppCompatTextView tv_showInput;
    AppCompatEditText et_serv, et_port, et_user, et_pass;
    AppCompatButton btn_startmine, btn_stopmine;
    AppCompatSeekBar sb_thread;
    AppCompatCheckBox cb_screen_awake;

    MinerService mService = null;
    
    static final int MSG_STATUS = 1;
    static final int STATUS_SPEED = 1;
    static final int STATUS_ACCEPTED = 2;
    static final int STATUS_REJECTED = 3;
    
    static final int MSG_CONSOLE = 2;
    
    static final int MSG_STATE = 3;
    
    
    private final StringBuilder sb = new StringBuilder();
    private static final int MAX_LOG_COUNT = 50;
    private ArrayList<ConsoleItem> logList;
    RecyclerView.Adapter adpt;
    int stateMiningUpdate;
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
        stateMiningUpdate = -1;
        Intent intent = new Intent(this, MinerService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
        if (!serviceWasRunning) {
            startService(intent);
        }
        //define section layout
        input_container = (ViewGroup) findViewById(R.id.input_container);
        status_container = (ViewGroup) findViewById(R.id.status_container);
        //define showInput
        tv_showInput = (AppCompatTextView) findViewById(R.id.show_userInput);
        //text status
        tv_speed = (AppCompatTextView) findViewById(R.id.speed_tv);
        tv_accepted = (AppCompatTextView) findViewById(R.id.accepted_tv);
        tv_rejected = (AppCompatTextView) findViewById(R.id.rejected_tv);
        //button
        btn_startmine = (AppCompatButton) findViewById(R.id.button_startmine);
        btn_stopmine = (AppCompatButton) findViewById(R.id.button_stopmine);
        //editable
        et_serv = (AppCompatEditText) findViewById(R.id.server_et);
        et_port = (AppCompatEditText) findViewById(R.id.port_et);
        et_user = (AppCompatEditText) findViewById(R.id.user_et);
        et_pass = (AppCompatEditText) findViewById(R.id.password_et);
        sb_thread = (AppCompatSeekBar)findViewById(R.id.threadSeek);
        final AppCompatTextView thread_view = (AppCompatTextView)findViewById(R.id.thread_view);
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
        int t = Runtime.getRuntime().availableProcessors();
        if (t < 1) t = 1;
        sb_thread.setMax(t);
        //checkbox
        cb_screen_awake = (AppCompatCheckBox) findViewById(R.id.settings_checkBox_keepscreenawake);
        if (savedInstanceState != null) {
            logList = savedInstanceState.getParcelableArrayList(KEYBUNDLE_CONSOLE);
            CharSequence[] texts = savedInstanceState.getCharSequenceArray(KEYBUNDLE_TEXTS);
            tv_speed.setText(texts[0]);
            tv_accepted.setText(texts[1]);
            tv_rejected.setText(texts[2]);
            tv_showInput.setText(texts[3]);
            et_serv.setText(texts[4]);
            et_port.setText(texts[5]);
            et_user.setText(texts[6]);
            et_pass.setText(texts[7]);
            int[] ints = savedInstanceState.getIntArray(KEYBUNDLE_INTS);
            sb_thread.setProgress(ints[0]); //old
        } else {
            logList = new ArrayList<ConsoleItem>(MAX_LOG_COUNT);
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            et_serv.setText(settings.getString(PREF_URL, DEFAULT_URL));
            et_port.setText(String.valueOf(settings.getInt(PREF_PORT, DEFAULT_PORT)));
            et_user.setText(settings.getString(PREF_USER, DEFAULT_USER));
            et_pass.setText(settings.getString(PREF_PASS, DEFAULT_PASS));
            sb_thread.setProgress(settings.getInt(PREF_THREAD, 1)); //old
        }
        final Window window = getWindow();
        cb_screen_awake.setChecked((window.getAttributes().flags&FLAG_KEEP_SCREEN_ON) != 0);
        cb_screen_awake.setOnCheckedChangeListener((cb, check) -> {
            if (check) {
                window.addFlags(FLAG_KEEP_SCREEN_ON);
            } else {
                window.clearFlags(FLAG_KEEP_SCREEN_ON);
            }
        });
        //log Adapter
        final RecyclerView consoleView = (RecyclerView)findViewById(R.id.console_view);
        consoleView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true));
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
    
    final String unit = " hash/sec";
    final DecimalFormat df = new DecimalFormat("#.##");
    MinerService.LocalBinder mBinder;
    final Handler statusHandler = new Handler(Looper.getMainLooper(), msg -> {
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
            }
            break;
        case MSG_CONSOLE: // console update
            adpt.notifyDataSetChanged();
            break;
        case MSG_STATE: // button mining update
            switch (msg.arg1) {
            default: break;
            case MSG_STATE_NONE:
                btn_stopmine.setVisibility(View.GONE);
                btn_stopmine.setEnabled(false);
                btn_startmine.setVisibility(View.VISIBLE);
                btn_startmine.setEnabled(true);
                tv_speed.setText("0 hash/sec");
                tv_accepted.setText("0");
                tv_rejected.setText("0");
                //enable all user Input
                input_container.setVisibility(View.VISIBLE);
                status_container.setVisibility(View.GONE);
                break;
            case MSG_STATE_ONSTART:
                btn_stopmine.setVisibility(View.GONE);
                btn_stopmine.setEnabled(false);
                btn_startmine.setVisibility(View.VISIBLE);
                btn_startmine.setEnabled(false);
                //disable all user Input
                input_container.setVisibility(View.GONE);
                status_container.setVisibility(View.VISIBLE);
                break;
            case MSG_STATE_RUNNING:
                btn_stopmine.setVisibility(View.VISIBLE);
                btn_stopmine.setEnabled(true);
                btn_startmine.setVisibility(View.GONE);
                btn_startmine.setEnabled(false);
                //disable all user Input
                input_container.setVisibility(View.GONE);
                status_container.setVisibility(View.VISIBLE);
                break;
            case MSG_STATE_ONSTOP:
                btn_stopmine.setVisibility(View.VISIBLE);
                btn_stopmine.setEnabled(false);
                btn_startmine.setVisibility(View.GONE);
                btn_startmine.setEnabled(false);
                //disable all user Input
                input_container.setVisibility(View.GONE);
                status_container.setVisibility(View.VISIBLE);
                break;
            }
            break;
        }
        return true;
    });
    final Thread updateThread = new Thread (() -> {
        try {
            for (;;)	{
                synchronized (mService) {
                    if (stateMiningUpdate != mService.state)
                        statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATE, stateMiningUpdate = mService.state, 0));
                    if (!mBinder.console.isEmpty()) {
                        logList.addAll(mBinder.console);
                        if (logList.size() > MAX_LOG_COUNT)
                            logList.removeRange(0, logList.size() - MAX_LOG_COUNT);
                        mBinder.console.clear();
                        statusHandler.sendEmptyMessage(MSG_CONSOLE);
                    }
                    if (mBinder.new_speed) {
                        statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_SPEED, 0, mBinder.speed));
                        mBinder.new_speed = false;
                    }
                    if (mBinder.new_accepted) {
                        statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_ACCEPTED, 0, mBinder.accepted));
                        mBinder.new_accepted = false;
                    }
                    if (mBinder.new_rejected) {
                        statusHandler.sendMessage(statusHandler.obtainMessage(MSG_STATUS, STATUS_REJECTED, 0, mBinder.rejected));
                        mBinder.new_rejected = false;
                    }
                    mService.wait();
                }
            }
        } catch (InterruptedException e) {}
    });
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = (MinerService.LocalBinder) service;
        mService = mBinder.getService();
        if (!updateThread.isAlive()) updateThread.start();
    }
    
    @Override
    public void onServiceDisconnected(ComponentName name) {
        if (updateThread.isAlive()) updateThread.interrupt();
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
    //button function
    public void toStartMining(View v) {
        String url = sb.append(et_serv.getText()).toString();
        sb.setLength(0);
        int port = Integer.parseInt(sb.append(et_port.getText()).toString());
        sb.setLength(0);
        String user = sb.append(et_user.getText()).toString();
        sb.setLength(0);
        String pass = sb.append(et_pass.getText()).toString();
        sb.setLength(0);
        tv_showInput.setText(String.format(
            "server -> %s:%d \nauth -> %s:%s\nuse %d threads",
            url, port, user, pass, sb_thread.getProgress()
        ));
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(PREF_URL, url);
        editor.putInt(PREF_PORT, port);
        editor.putString(PREF_USER, user);
        editor.putString(PREF_PASS, pass);
        editor.putInt(PREF_THREAD, sb_thread.getProgress());
        editor.commit();
        
        mService.startMining(url,port,user,pass, sb_thread.getProgress());
    }
    public void toStopMining(View v) {
        mService.stopMining();
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
        outState.putParcelableArrayList(KEYBUNDLE_CONSOLE, logList);
        CharSequence[] texts = new CharSequence[8];
        texts[0] = tv_speed.getText();
        texts[1] = tv_accepted.getText();
        texts[2] = tv_rejected.getText();
        texts[3] = tv_showInput.getText();
        texts[4] = et_serv.getText();
        texts[5] = et_port.getText();
        texts[6] = et_user.getText();
        texts[7] = et_pass.getText();
        outState.putCharSequenceArray(KEYBUNDLE_TEXTS, texts);
        int[] ints = new int[1];
        ints[0] = sb_thread.getProgress();
        outState.putIntArray(KEYBUNDLE_INTS, ints);
        
        
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            logList = savedInstanceState.getParcelableArrayList(KEYBUNDLE_CONSOLE);
            adpt.notifyDataSetChanged();
            CharSequence[] texts = savedInstanceState.getCharSequenceArray(KEYBUNDLE_TEXTS);
            tv_speed.setText(texts[0]);
            tv_accepted.setText(texts[1]);
            tv_rejected.setText(texts[2]);
            tv_showInput.setText(texts[3]);
            et_serv.setText(texts[4]);
            et_port.setText(texts[5]);
            et_user.setText(texts[6]);
            et_pass.setText(texts[7]);
            int[] ints = savedInstanceState.getIntArray(KEYBUNDLE_INTS);
            sb_thread.setProgress(ints[0]); //old
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
            stopService(new Intent(this, MinerService.class));
        }
        unbindService(this);
    }
    public native String callNative(); 
    public static class ConsoleItemHolder extends RecyclerView.ViewHolder {
        private AppCompatTextView time;
        private AppCompatTextView msg;
    
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
    private static final DateFormat logDateFormat = new SimpleDateFormat("[HH:mm:ss] ");
    public static class ConsoleItem extends Object implements Parcelable {
        public final String time, msg;
        public ConsoleItem(String m) {
            time = logDateFormat.format(new Date());
            msg = m;
        }
        protected ConsoleItem(Parcel in) {
            String[] strings = new String[2];
            in.readStringArray(strings);
            time = strings[0];
            msg = strings[1];
        }
        public static final Parcelable.Creator<ConsoleItem> CREATOR = new Parcelable.Creator<ConsoleItem>() {
            @Override
            public ConsoleItem createFromParcel(Parcel in) {
                return new ConsoleItem(in);
            }
            @Override
            public ConsoleItem[] newArray(int size) {
                return new ConsoleItem[size];
            }
        };
        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(new String[] { time, msg });
        }
    }
}


