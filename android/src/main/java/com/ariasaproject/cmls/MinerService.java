package com.ariasaproject.cmls;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;

import static com.ariasaproject.cmls.Constants.DEFAULT_PASS;
import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static com.ariasaproject.cmls.Constants.DEFAULT_RETRYPAUSE;
import static com.ariasaproject.cmls.Constants.DEFAULT_SCANTIME;
import static com.ariasaproject.cmls.Constants.DEFAULT_THREAD;
import static com.ariasaproject.cmls.Constants.DEFAULT_THROTTLE;
import static com.ariasaproject.cmls.Constants.DEFAULT_URL;
import static com.ariasaproject.cmls.Constants.DEFAULT_USER;

import static com.ariasaproject.cmls.Constants.MSG_ACCEPTED_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_CONSOLE_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_REJECTED_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_SPEED_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_STATUS_UPDATE;

import static com.ariasaproject.cmls.Constants.MSG_TERMINATED;

import static com.ariasaproject.cmls.Constants.PREF_DONATE;
import static com.ariasaproject.cmls.Constants.PREF_PASS;
import static com.ariasaproject.cmls.Constants.PREF_PRIORITY;
import static com.ariasaproject.cmls.Constants.PREF_RETRYPAUSE;
import static com.ariasaproject.cmls.Constants.PREF_SCANTIME;
import static com.ariasaproject.cmls.Constants.PREF_THREAD;
import static com.ariasaproject.cmls.Constants.PREF_THROTTLE;
import static com.ariasaproject.cmls.Constants.PREF_TITLE;
import static com.ariasaproject.cmls.Constants.PREF_URL;
import static com.ariasaproject.cmls.Constants.PREF_USER;
import static com.ariasaproject.cmls.Constants.STATUS_NOT_MINING;

public class MinerService extends Service {

    IMiningConnection mc;
    IMiningWorker imw;
    SingleMiningChief smc;
    //Miner miner;
    Console console;
   // String news=null;
    Boolean running=false;
    float speed=0;
    int accepted=0;
    int rejected=0;
    String status= STATUS_NOT_MINING;
    String cString="";
    
    final Handler.Callback serviceHandlerCallback = new Handler.Callback () {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            default:
                break;
            case MSG_TERMINATED:
                running = false;
                break;
            }
            Bundle bundle = msg.getData();
            if (bundle != null) {
                if (bundle.getFloat(MSG_SPEED_UPDATE, 0) != 0)
                    speed = bundle.getFloat(MSG_SPEED_UPDATE);
                if (bundle.getLong(MSG_ACCEPTED_UPDATE, 0) != 0)
                    accepted = (int) bundle.getLong(MSG_ACCEPTED_UPDATE);
                if (bundle.getLong(MSG_REJECTED_UPDATE, 0) != 0)
                    rejected = (int) bundle.getLong(MSG_REJECTED_UPDATE);
                if (bundle.getString(MSG_STATUS_UPDATE, "") != "")
                    status = bundle.getString(MSG_STATUS_UPDATE);
                if (bundle.getString(MSG_CONSOLE_UPDATE, "") != "")
                    cString = bundle.getString(MSG_CONSOLE_UPDATE);
            }
            return true;
        }
    };
    Handler serviceHandler = new Handler(Looper.getMainLooper(), serviceHandlerCallback);
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        MinerService getService() {
            return MinerService.this;
        }
    }

    public MinerService() {}
    public void startMiner() {
        console = new Console(serviceHandler);
        SharedPreferences settings = getSharedPreferences(PREF_TITLE, 0);
        String url, user, pass;
        speed=0;
        accepted=0;
        rejected=0;
        console.write("Service: Start mining");
        url = settings.getString(PREF_URL, DEFAULT_URL);
        user = settings.getString(PREF_USER, DEFAULT_USER);
        pass = settings.getString(PREF_PASS, DEFAULT_PASS);
        try {
            mc = new StratumMiningConnection(url,user,pass);
            int nThread =  settings.getInt(PREF_THREAD, DEFAULT_THREAD);
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
        running=false;
        try {
            smc.stopMining();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }



}