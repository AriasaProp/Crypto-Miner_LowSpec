package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import androidx.annotation.Keep;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CpuMiningWorker implements IMiningWorker {
    private final int _number_of_thread;
    private final MessageSendListener MSL;

    public CpuMiningWorker(int i_number_of_thread, MessageSendListener msl) {
        MSL = msl;
        _number_of_thread = i_number_of_thread;
    }
    
    MiningWork mw;
    @Override
    public synchronized boolean doWork(MiningWork i_work) {
        mw = i_work; 
        nativeJob(_number_of_thread, i_work.header.refHex(), i_work.target.refHex());
        return true;
    }
    private native boolean nativeJob(int step, byte[] head, byte[] target);
    private native void nativeStop();
    
    @Override
    public synchronized void stopWork() {
        nativeStop();
        System.gc();
    }

    @Override
    public synchronized long getNumberOfHash() {
        return getHashesTotal();
    }

    private native int getHashesTotal();
    public native boolean getStatus();

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();

    @Keep
    private void msl_sendMessage(int a, int b, int c, Object d) {
        MSL.sendMessage(a, b, c, d);
    }
    
    @Keep
    private void invokeNonceFound(int n) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Nonce found! " + n + ". Now, wait new job");
        for (IWorkerEvent i : _as_listener) i.onNonceFound(mw, n);
    }

    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }
}
