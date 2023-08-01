package com.ariasaproject.cmls;

import static com.ariasaproject.cmls.Constants.MAX_STATUS;
import static com.ariasaproject.cmls.Constants.MSG_STATE;
import static com.ariasaproject.cmls.Constants.MSG_STATE_NONE;
import static com.ariasaproject.cmls.Constants.MSG_STATE_ONSTART;
import static com.ariasaproject.cmls.Constants.MSG_STATE_ONSTOP;
import static com.ariasaproject.cmls.Constants.MSG_STATE_RUNNING;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_ACCEPTED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_REJECTED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_STATUS;
import static com.ariasaproject.cmls.Constants.STATUS_TYPE_ACCEPTED;
import static com.ariasaproject.cmls.Constants.STATUS_TYPE_REJECTED;
import static com.ariasaproject.cmls.Constants.STATUS_TYPE_SPEED;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.ariasaproject.cmls.MainActivity.ConsoleItem;
import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;

import java.util.ArrayList;

public class MinerService extends Service implements Handler.Callback {
    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    int state = MSG_STATE_NONE;
    Handler serviceHandler;
    Object[] minerStatus = new Object[MAX_STATUS];
    public ArrayList<ConsoleItem> console = new ArrayList<ConsoleItem>();
    // Binder given to clients
    private final LocalBinder status = new LocalBinder();

    public void onCreate() {
        serviceHandler = new Handler(Looper.getMainLooper(), this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    final MessageSendListener workerMsg =
            (what, arg1, arg2, o) ->
                    serviceHandler.sendMessage(serviceHandler.obtainMessage(what, arg1, arg2, o));
    private long status_accepted = 0, status_rejected = 0;

    @Override
    public synchronized boolean handleMessage(Message msg) {
        switch (msg.what) {
            default:
                break;
            case MSG_UPDATE:
                switch (msg.arg1) {
                    default:
                        break;
                    case MSG_UPDATE_SPEED:
                        minerStatus[STATUS_TYPE_SPEED] = (Float) msg.obj;
                        break;
                    case MSG_UPDATE_ACCEPTED:
                        minerStatus[STATUS_TYPE_ACCEPTED] = (Long) (++status_accepted);
                        break;
                    case MSG_UPDATE_REJECTED:
                        minerStatus[STATUS_TYPE_REJECTED] = (Long) (++status_rejected);
                        break;
                    case MSG_UPDATE_STATUS:
                        console.add(new ConsoleItem("Mining State: " + (String) msg.obj));
                        break;
                    case MSG_UPDATE_CONSOLE:
                        console.add(new ConsoleItem((String) msg.obj));
                        break;
                }
                break;
            case MSG_STATE:
                switch (msg.arg1) {
                    default:
                    case MSG_STATE_NONE:
                        minerStatus[STATUS_TYPE_ACCEPTED] = (Long) (status_accepted = 0);
                        minerStatus[STATUS_TYPE_REJECTED] = (Long) (status_rejected = 0);
                        break;
                    case MSG_STATE_ONSTART:
                        if ((state == MSG_STATE_NONE) && (smc == null)) {
                            console.add(new ConsoleItem("Service: Start mining"));
                            try {
                                mc = new StratumMiningConnection(String.format("%s:%d", url, port), user, pass);
                                imw = new CpuMiningWorker(nThread, workerMsg);
                                smc = new SingleMiningChief(mc, imw, workerMsg);
                                smc.startMining();
                                console.add(new ConsoleItem("Service: Started mining"));
                                serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE, MSG_STATE_RUNNING, 0));
                            } catch (Exception e) {
                                e.printStackTrace();
                                smc = null;
                                console.add(new ConsoleItem("Service Error: " + e.getMessage()+ "\nStarted mining is failed"));
                                serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE, MSG_STATE_NONE, 0));
                            }
                        }
                        break;
                    case MSG_STATE_RUNNING:
                        break;
                    case MSG_STATE_ONSTOP:
                        if ((state == MSG_STATE_RUNNING) && (smc != null)) {
                            console.add(
                                    new ConsoleItem("Service: Stop mining"));
                            try {
                                smc.stopMining();
                                smc = null;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            console.add(new ConsoleItem("Service: Stopped mining"));
                            serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE, MSG_STATE_NONE, 0));
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
        serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE, MSG_STATE_ONSTART, 0));
    }

    public void stopMining() {
        serviceHandler.sendMessage(serviceHandler.obtainMessage(MSG_STATE, MSG_STATE_ONSTOP, 0));
    }

    @Override
    public synchronized void onDestroy() {
        try {
            while (state != MSG_STATE_NONE) {
                stopMining();
                wait();
            }
        } catch (Exception e) {
        }
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
    }
}
