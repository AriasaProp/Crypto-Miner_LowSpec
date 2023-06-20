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
    
    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    public final Console console;
    boolean running=false;
    public final MiningStatusService status = new MiningStatusService();
    final Handler.Callback serviceHandlerCallback = new Handler.Callback () {
        @Override
        public boolean handleMessage(Message msg) {
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
                    running = false;
                    break;
                }
                status.notifyAll();
                
            }
            return true;
        }
    };
    Handler serviceHandler = new Handler(Looper.getMainLooper(), serviceHandlerCallback);
    // Binder given to clients
    private final LocalBinder mBinder = new LocalBinder();
    public MinerService() {
        console = new Console(serviceHandler);
    }
    public void startMiner(String url, String user, String pass, int nThread) {
        status.reSet();
        console.write("Service: Start mining");
        try {
            mc = new StratumMiningConnection(url,user,pass);
            imw = new CpuMiningWorker(nThread,DEFAULT_RETRYPAUSE,DEFAULT_PRIORITY,console);
            smc = new SingleMiningChief(mc,imw,console,serviceHandler);
            smc.startMining();
            running =true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMiner() {
        console.write("Service: Stopping mining");
        Toast.makeText(this,"Worker cooling down, this can take a few minutes",Toast.LENGTH_LONG).show();
        try {
            smc.stopMining();
        } catch (Exception e) {
            e.printStackTrace();
        }
        running=false;
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