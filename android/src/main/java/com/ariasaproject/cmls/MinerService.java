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
import android.os.Binder;

import java.util.ArrayList;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;
import com.ariasaproject.cmls.MainActivity.ConsoleItem;

import static com.ariasaproject.cmls.MainActivity.PREF_URL;
import static com.ariasaproject.cmls.MainActivity.PREF_PORT;
import static com.ariasaproject.cmls.MainActivity.PREF_USER;
import static com.ariasaproject.cmls.MainActivity.PREF_PASS;
import static com.ariasaproject.cmls.MainActivity.PREF_THREAD;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;

public class MinerService extends Service implements Handler.Callback{
    public static final int MSG_STATE = 1;
    public static final int MSG_UPDATE = 2;
    
    public static final int MSG_STATE_NONE = 0;
    public static final int MSG_STATE_ONSTART = 1;
    public static final int MSG_STATE_RUNNING = 2;
    public static final int MSG_STATE_ONSTOP = 3;
    
    public static final int MSG_UPDATE_SPEED = 1;
    public static final int MSG_UPDATE_ACC = 2;
    public static final int MSG_UPDATE_REJECT = 3;
    public static final int MSG_UPDATE_STATUS = 4;
    public static final int MSG_UPDATE_CONSOLE = 5;
    
    public static final int MINING_NONE = 0;
    public static final int MINING_ONSTART = 1;
    public static final int MINING_RUNNING = 2;
    public static final int MINING_ONSTOP = 3;
    
    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    int state = MINING_NONE;
    Handler serviceHandler;
    // Binder given to clients
    private final LocalBinder status = new LocalBinder();
    public void onCreate() {
        serviceHandler = new Handler(Looper.getMainLooper(), this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    final MessageSendListener workerMsg = (what, arg1, arg2, o) -> serviceHandler.sendMessage(serviceHandler.obtainMessage(what, arg1, arg2, o));
    
    @Override
    public synchronized boolean handleMessage(Message msg) {
        switch (msg.what) {
        default: break;
        case MSG_UPDATE:
            switch (msg.arg1) {
                default: break;
                case MSG_UPDATE_SPEED:
                    status.new_speed |= true;
                    status.speed = (Float) msg.obj;
                    break;
                case MSG_UPDATE_ACC:
                    status.new_accepted |= true;
                    status.accepted++;
                    break;
                case MSG_UPDATE_REJECT:
                    status.new_rejected |= true;
                    status.rejected++;
                    break;
                case MSG_UPDATE_STATUS:
                    status.console.add(new ConsoleItem("Mining State: " + (String)msg.obj));
                    break;
                case MSG_UPDATE_CONSOLE:
                    status.console.add(new ConsoleItem((String)msg.obj));
                    break;
            }
            break;
        case MSG_STATE:
            switch (msg.arg1) {
                default: break;
                case MSG_STATE_NONE:
                    status.new_accepted |= true;
                    status.new_rejected |= true;
                    status.accepted = 0;
                    status.rejected = 0;
                    break;
                case MSG_STATE_ONSTART:
                    if (state == MSG_STATE_NONE) {
                        new Thread(() -> {
                            status.console.add(new ConsoleItem("Service: Start mining"));
                            try {
                                mc = new StratumMiningConnection(String.format("%s:%d",url, port),user,pass);
                                imw = new CpuMiningWorker(nThread, 1, workerMsg);
                                smc = new SingleMiningChief(mc,imw,workerMsg);
                                smc.startMining();
                                status.console.add(new ConsoleItem("Service: Started mining"));
                                MinerService.this.serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE,MSG_STATE_RUNNING, 0));
                            } catch (Exception e) {
                                e.printStackTrace();
                                smc = null;
                                status.console.add(new ConsoleItem("Service: Error " + e.getMessage() + "\n Started mining is failed"));
                                MinerService.this.serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE,MSG_STATE_NONE, 0));
                            }
                        }).start();
                    }
                    break;
                case MSG_STATE_RUNNING:
                    break;
                case MSG_STATE_ONSTOP:
                    if (state == MSG_STATE_RUNNING) {
                        new Thread(() -> {
                            status.console.add(new ConsoleItem("Service: Stop mining"));
                            try {
                                smc.stopMining();
                                smc = null;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            status.console.add(new ConsoleItem("Service: Stopped mining"));
                            serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE,MSG_STATE_NONE, 0));
                        }).start();
                    }
                    break;
            }
            state = msg.arg1;
            break;
        }
        notifyAll();
        return true;
        
    }
    String url, user, pass;
    int port, nThread;
    public void startMining(String u, int p, String user, String pass, int n) {
        this.url = u;
        this.user = user;
        this.pass = pass;
        this.port = p;
        this.nThread = n;
        serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE,MSG_STATE_ONSTART, 0));
    }
    public void stopMining() {
        serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE,MSG_STATE_ONSTOP, 0));
    }
    @Override
    public synchronized void onDestroy() {
        try {
            while (state != MINING_NONE) {
                stopMining();
                wait();
            }
        } catch (Exception e) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return status;
    }
    
    public class LocalBinder extends Binder {
        MinerService getService() {
            return MinerService.this;
        }
        public boolean new_speed = false;
        public float speed = 0;
        public boolean new_accepted = false;
        public long accepted = 0;
        public boolean new_rejected = false;
        public long rejected = 0;
        public ArrayList<ConsoleItem> console = new ArrayList<ConsoleItem>();
    }
}

