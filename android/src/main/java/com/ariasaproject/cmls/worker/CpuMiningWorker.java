package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import java.util.ArrayList;

public class CpuMiningWorker implements IMiningWorker {
    private final int _number_of_thread;
    private final MessageSendListener MSL;
    private final ThreadGroup workers = new ThreadGroup("CPU_Miner");

    public CpuMiningWorker(int i_number_of_thread, MessageSendListener msl) {
        MSL = msl;
        _number_of_thread = i_number_of_thread;
    }

    private volatile long hashes = 0;
    private volatile long hashes_per_sec = 0;
    private volatile long worker_saved_time = 0;

    private synchronized void calcSpeedPerThread() {
        hashes++;
        hashes_per_sec++;
        long curr_time = System.currentTimeMillis();
        long delta = curr_time - worker_saved_time;
        if (delta < 1000) return;
        float _speed = (hashes_per_sec * 1000.0f) / (float) delta;
        worker_saved_time = curr_time;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
        hashes_per_sec = 0;
    }

    @Override
    public synchronized boolean doWork(MiningWork i_work) {
        stopWork();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads starting");
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        hashes = 0;
        hashes_per_sec = 0;
        worker_saved_time = System.currentTimeMillis();
        lastStart = 0;
        for (int i = 0; i < _number_of_thread; i++) {
            new Thread(workers, () -> {
                long hasher = Constants.initHasher();
                int nonce = i;
                while(!Thread.interrupted()) {
                    if (Constants.nativeHashing(hasher, i_work.header.refHex(), nonce, i_work.target.refHex())) {
                        invokeNonceFound(nonce);
                        break;
                    }
                    calcSpeedPerThread();
                    nonce += _number_of_thread;
                }
                Constants.destroyHasher(hasher);
            }).start();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads started");
        return true;
    }

    @Override
    public synchronized void stopWork() {
        try {
            if (workers.activeCount() <= 0) return;
        } catch (InterruptedException e) {}
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker Stopping");
        workers.interrupt();
        try {
            Thread[] gr = new Thread[_number_of_thread];
            workers.enumerate(gr);
            for (Thread g : gr)
                g.join();
        } catch (InterruptedException e) {}
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker Stopped");
        System.gc();
    }

    @Override
    public synchronized long getNumberOfHash() {
        return hashes;
    }

    public synchronized boolean getThreadsStatus() {
        return threadCount > 0;
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();

    private synchronized void invokeNonceFound(int i_nonce) {
        workers.interrupt();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Nonce found! " + i_nonce + ". Now, wait new job");
        for (IWorkerEvent i : _as_listener) i.onNonceFound(i_work, i_nonce);
    }

    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }

}
