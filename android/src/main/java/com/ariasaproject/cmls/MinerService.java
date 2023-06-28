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

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

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

public class MinerService extends Service implements Handler.Callback{
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
    
    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    public Console console;
    int state = MINING_NONE;
    public final MiningStatusService status = new MiningStatusService();
    @Override
    public synchronized boolean handleMessage(Message msg) {
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
            changedState(MINING_ONSTOP);
            stopMining();
            break;
        }
        notifyAll();
        return true;
        
    }
    final Handler serviceHandler = new Handler(Looper.getMainLooper(), this);
    // Binder given to clients
    private final LocalBinder mBinder = new LocalBinder();
    ExecutorService es;
    public void onCreate() {
        console = new Console(serviceHandler);
        es = Executors.newFixedThreadPool(1);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    public void startMining(String url, int port, String user, String pass, int nThread) {
        es.execute(() -> {
            if (smc != null) {
                try {
                    smc.stopMining();
                } catch (Exception e) {}
            }
            console.write("Service: Start mining");
            try {
                mc = new StratumMiningConnection(String.format("%s:%d",url, port),user,pass);
                imw = new CpuMiningWorker(nThread,DEFAULT_RETRYPAUSE,DEFAULT_PRIORITY,console);
                smc = new SingleMiningChief(mc,imw,console,serviceHandler);
                smc.startMining();
                changedState(MINING_RUNNING);
            } catch (Exception e) {
                e.printStackTrace();
                changedState(MINING_NONE);
                smc = null;
            }
            console.write("Service: Started mining");
        });
    }
    public void stopMining() {
        es.execute(() -> {
            if (smc == null) return;
            console.write("Service: Stop mining");
            try {
                smc.stopMining();
                smc = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            changedState(MINING_NONE);
            console.write("Service: Stopped mining");
        });
    }
    public synchronized void changedState(int s) {
         this.state = s;
         notifyAll();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        es.shutdown();
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