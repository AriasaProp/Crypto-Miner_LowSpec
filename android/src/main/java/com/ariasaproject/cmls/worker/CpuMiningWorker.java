package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import androidx.annotation.Keep;

import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import java.util.ArrayList;

public class CpuMiningWorker implements IMiningWorker {
    private final int _number_of_thread;
    private final MessageSendListener MSL;

    public CpuMiningWorker(int i_number_of_thread, MessageSendListener msl) {
        MSL = msl;
        _number_of_thread = i_number_of_thread;
    }

    MiningWork mw;

    @Override
    public synchronized void doWork(MiningWork i_work) throws Exception {
        mw = i_work;
        if (!nativeJob(_number_of_thread, i_work.header.refHex(), i_work.target.refHex())) {
            throw new RuntimeException("Failed start Native Hasher!");
        }
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
    private void updateSpeed(float f) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, new Float(f));
    }

    @Keep
    private void updateConsole(String s) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, s);
    }

    @Keep
    private void invokeNonceFound(int n) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE,0,"Mining: Nonce found! " + n + ". Now, wait new job");
        for (IWorkerEvent i : _as_listener) i.onNonceFound(mw, n);
    }

    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }
}
