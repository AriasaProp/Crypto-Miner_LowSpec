package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

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

    volatile MiningWork current_work;
    AtomicBoolean onMine = new AtomicBoolean(false);

    @Override
    public boolean doWork(MiningWork i_work) {
        stopWork();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads starting");
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        synchronized(this) {
            current_work = i_work;
            hashes = 0;
            hashes_per_sec = 0;
            worker_saved_time = System.currentTimeMillis();
        }
        lastStart.set(0);
        onMine.set(true);
        generate_worker();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads started");
        return true;
    }

    @Override
    public void stopWork() {
        if (onMine.get()) {
            onMine.set(false);
        }
        if (ThreadCount.get() > 0) {
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Killing threads");
            synchronized (this) {
                try {
                    while (ThreadCount.get() > 0) wait();
                } catch (InterruptedException e) {}
            }
            System.gc();
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Killed threads");
        }
    }

    @Override
    public synchronized long getNumberOfHash() {
        return hashes;
    }

    public boolean getThreadsStatus() {
        return ThreadCount.get() > 0;
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();

    private synchronized void invokeNonceFound(int i_nonce) {
        onMine.set(false);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Nonce found! " + i_nonce);
        for (IWorkerEvent i : _as_listener) i.onNonceFound(current_work, i_nonce);
    }

    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }

    private static final int maxCore = Runtime.getRuntime().availableProcessors();
    private static final int maxStart = 0xffff;
    private AtomicInteger lastStart = new AtomicInteger(0), ThreadCount = new AtomicInteger(0);

    void generate_worker() {
        while (onMine.get() && (lastStart.get() <= maxStart) && (maxCore > ThreadCount.get())) {
            final int _start = lastStart.getAndIncrement() << 16;
            ThreadCount.incrementAndGet();
            new Thread(
                    workers,
                    () -> {
                        long hasher = Constants.initHasher();
                        int nonce = _start;
                        while(onMine.get()) {
                            if (Constants.nativeHashing(hasher, current_work.header.refHex(), nonce, current_work.target.refHex())) {
                                invokeNonceFound(nonce);
                                break;
                            }
                            calcSpeedPerThread();
                            if((++nonce & 0xffff) == 0) break;
                        }
                        Constants.destroyHasher(hasher);
                        ThreadCount.decrementAndGet();
                        CpuMiningWorker.this.notify();
                        generate_worker();
                    })
            .start();
        }
    }

}
