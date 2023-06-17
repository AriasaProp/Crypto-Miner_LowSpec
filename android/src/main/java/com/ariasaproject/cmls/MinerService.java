package com.ariasaproject.cmls;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;

import static com.ariasaproject.cmls.Constants.DEFAULT_DONATE;
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
    //int baseThreadCount = Thread.activeCount();

    Handler serviceHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle=msg.getData();
            Log.i("LC", "Service: handleMessage() "+msg.arg1);

            if(msg.arg1==MSG_CONSOLE_UPDATE) { cString = bundle.getString("console"); }
            else if(msg.arg1==MSG_SPEED_UPDATE) { speed = bundle.getFloat("speed"); }
            else if(msg.arg1==MSG_STATUS_UPDATE) { status = bundle.getString("status"); }
            else if(msg.arg1==MSG_ACCEPTED_UPDATE) { accepted = (int) bundle.getLong("accepted"); }
            else if(msg.arg1==MSG_REJECTED_UPDATE) { rejected = (int) bundle.getLong("rejected"); }
            else if(msg.arg1==MSG_TERMINATED) {	running=false; }
            super.handleMessage(msg);
        }
    };
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        MinerService getService() {
            return MinerService.this;
        }
    }

    public MinerService() {
        Log.i("LC", "Service: MinerService()");
    }


    public void startMiner()
    {
        console = new Console(serviceHandler);
        Log.i("LC", "MinerService:startMiner()");
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
        } catch (MinyaException e) {
            e.printStackTrace();
        }
    }

    public void stopMiner()
    {
        Log.i("LC", "Service: onBind()");
        console.write("Service: Stopping mining");
        Toast.makeText(this,"Worker cooling down, this can take a few minutes",Toast.LENGTH_LONG).show();
        running=false;
        try {
            smc.stopMining();
        } catch (MinyaException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("LC", "Service: onBind()");

        return mBinder;
    }



}