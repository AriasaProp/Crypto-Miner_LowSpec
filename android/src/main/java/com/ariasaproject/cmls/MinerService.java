package com.ariasaproject.cmls;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.ariasaproject.cmls.MiningStatusService;
import com.ariasaproject.cmls.MiningStatusService.ConsoleItem;
import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;

import static com.ariasaproject.cmls.MainActivity.PREF_URL;
import static com.ariasaproject.cmls.MainActivity.PREF_PORT;
import static com.ariasaproject.cmls.MainActivity.PREF_USER;
import static com.ariasaproject.cmls.MainActivity.PREF_PASS;
import static com.ariasaproject.cmls.MainActivity.PREF_THREAD;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static com.ariasaproject.cmls.Constants.DEFAULT_RETRYPAUSE;
import static com.ariasaproject.cmls.Constants.DEFAULT_SCANTIME;
import static com.ariasaproject.cmls.Constants.DEFAULT_THREAD;
import static com.ariasaproject.cmls.Constants.DEFAULT_THROTTLE;

public class MinerService extends Service {
    public static final int MSG_TERMINATED = 1;
    public static final int MSG_UPDATE_SERVICE_STATUS = 2;
    
    public static final int MSG_ARG1_UPDATE_SPEED = 1;
    public static final int MSG_ARG1_UPDATE_ACC = 2;
    public static final int MSG_ARG1_UPDATE_REJECT = 3;
    public static final int MSG_ARG1_UPDATE_STATUS = 4;
    public static final int MSG_ARG1_UPDATE_CONSOLE = 5;
    
    public static final int MINING_NONE = 0;
    public static final int MINING_ONSTART = 1;
    public static final int MINING_RUNNING = 2;
    public static final int MINING_ONSTOP = 3;
    
    static final String DEFAULT_URL="stratum+tcp://us2.litecoinpool.org";
    static final int DEFAULT_PORT=3333;
    static final String DEFAULT_USER="Ariasa.test";
    static final String DEFAULT_PASS="123";
    
    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    public final Console console;
    int state = MINING_NONE;
    public final MiningStatusService status = new MiningStatusService();
    Handler serviceHandler = new Handler(Looper.getMainLooper(), (msg) -> {
        synchronized (status) {
            switch (msg.what) {
            default:
                break;
            case MSG_UPDATE_SERVICE_STATUS:
                switch (msg.arg1) {
                    case MSG_ARG1_UPDATE_SPEED:
                        status.new_speed |= true;
                        status.speed = (Float) msg.obj;
                        break;
                    case MSG_ARG1_UPDATE_ACC:
                        status.new_accepted |= true;
                        status.accepted = (Long) msg.obj;
                        break;
                    case MSG_ARG1_UPDATE_REJECT:
                        status.new_rejected |= true;
                        status.rejected = (Long) msg.obj;
                        break;
                    case MSG_ARG1_UPDATE_STATUS:
                        status.new_status |= true;
                        status.status = (String) msg.obj;
                        break;
                    case MSG_ARG1_UPDATE_CONSOLE:
                        status.console.add(new ConsoleItem((String)msg.obj));
                        break;
                    default:
                        break;
                }
                break;
            case MSG_TERMINATED:
                stopSelf();
                break;
            }
            status.notifyAll();
        }
        return true;
    });
    // Binder given to clients
    private final LocalBinder mBinder = new LocalBinder();
    public MinerService() {
        console = new Console(serviceHandler);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        state = MINING_ONSTART;
        status.reSet();
        console.write("Service: Start mining");
        String url = intent.getStringExtra(PREF_URL);
        if (url==null || url.isEmpty()) url = DEFAULT_URL;
        int port = intent.getIntExtra(PREF_PORT, DEFAULT_PORT);
        String user = intent.getStringExtra(PREF_USER);
        if (user==null || user.isEmpty()) user = DEFAULT_USER;
        String pass = intent.getStringExtra(PREF_PASS);
        if (pass==null || pass.isEmpty()) pass = DEFAULT_PASS;
        int nThread = intent.getIntExtra(PREF_THREAD, 1);
        try {
            mc = new StratumMiningConnection(String.format("%s:%d",url, port),user,pass);
            imw = new CpuMiningWorker(nThread,DEFAULT_RETRYPAUSE,DEFAULT_PRIORITY,console);
            smc = new SingleMiningChief(mc,imw,console,serviceHandler);
            smc.startMining();
            state = MINING_RUNNING;
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        state = MINING_ONSTOP;
        console.write("Service: Stopping mining");
        Toast.makeText(this,"Worker cooling down, this can take a few minutes",Toast.LENGTH_LONG).show();
        try {
            smc.stopMining();
            smc = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        state = MINING_NONE;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    public class LocalBinder extends Binder {
        MinerService getService() {
            return MinerService.this;
        }
    }
}