package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class CpuMiningWorker implements IMiningWorker {
    private final int _number_of_thread;
    private final MessageSendListener MSL;
    private final Thread[] workers;

    public CpuMiningWorker(int i_number_of_thread, MessageSendListener msl) {
        MSL = msl;
        _number_of_thread = i_number_of_thread;
        workers = new Thread[i_number_of_thread];
    }

    private volatile long hashes = 0;
    private volatile long hashes_per_sec = 0;
    private volatile long worker_saved_time = 0;
    private volatile int threadCountDown = 0;
    private AtomicBoolean onMine = new AtomicBoolean(false);

    private synchronized void calcSpeedPerThread() {
        hashes++;
        hashes_per_sec++;
        long curr_time = System.currentTimeMillis();
        long delta = curr_time - worker_saved_time;
        if (delta < 1000) return;
        worker_saved_time = curr_time;
        float _speed = (hashes_per_sec * 1000.0f) / (float) delta;
        hashes_per_sec = 0;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
    }

    @Override
    public synchronized boolean doWork(MiningWork i_work) {
        stopWork();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads starting");
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        hashes = 0;
        hashes_per_sec = 0;
        onMine.set(true);
        worker_saved_time = System.currentTimeMillis();
        for (int i = 0; i < _number_of_thread; i++) {
            final int _start = i;
            workers[i] = new Thread(() -> {
                synchronized (CpuMiningWorker.this) {
                    threadCountDown++;
                }
                long hasher = Constants.initHasher();
                int nonce = _start;
                do {
                    if (Constants.nativeHashing(hasher, i_work.header.refHex(), nonce, i_work.target.refHex())) {
                        invokeNonceFound(i_work,nonce);
                        break;
                    }
                    calcSpeedPerThread();
                    nonce += _number_of_thread;
                } while(onMine.get());
                Constants.destroyHasher(hasher);
                synchronized (CpuMiningWorker.this) {
                    threadCountDown--;
                    CpuMiningWorker.this.notify();
                }
            });
            workers[i].start();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads started");
        return true;
    }

    @Override
    public synchronized void stopWork() {
        if (!onMine.get()) return;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker Stopping");
        onMine.set(false);
        try {
            do {
                wait();
            } while (threadCountDown > 0);
            for (int i = 0; i < _number_of_thread; i++)
                workers[i] = null;
        } catch (InterruptedException e) {}
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker Stopped");
        System.gc();
    }

    @Override
    public synchronized long getNumberOfHash() {
        return hashes;
    }

    public synchronized boolean getThreadsStatus() {
        return onMine.get();
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();

    private synchronized void invokeNonceFound(MiningWork mw, int n) {
        onMine.set(false);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Nonce found! " + n + ". Now, wait new job");
        for (IWorkerEvent i : _as_listener) i.onNonceFound(mw, n);
    }

    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }

}
