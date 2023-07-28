package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;

import java.security.GeneralSecurityException;
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

    volatile MiningWork current_work;

    @Override
    public synchronized boolean doWork(MiningWork i_work) throws Exception {
        if (workers.activeCount() > 0) {
            workers.interrupt();
        }
        System.gc();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads starting");
        current_work = i_work;
        hashes = 0;
        hashes_per_sec = 0;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        worker_saved_time = System.currentTimeMillis();
        lastNonce = 0;
        generate_worker();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads started");
        return true;
    }

    @Override
    public synchronized void stopWork() {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Killing threads");
        if (workers.activeCount() > 0) {
            workers.interrupt();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Killed threads");
    }

    @Override
    public synchronized long getNumberOfHash() {
        return hashes;
    }

    public boolean getThreadsStatus() {
        return workers.activeCount() > 0;
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();

    private synchronized void invokeNonceFound(int i_nonce) {
        stopWork();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Nonce found! " + i_nonce);
        for (IWorkerEvent i : _as_listener) i.onNonceFound(current_work, i_nonce);
    }

    public synchronized void addListener(IWorkerEvent i_listener) throws GeneralSecurityException {
        this._as_listener.add(i_listener);
    }

    private static final int nonceStep = 4094;
    private volatile int lastNonce = 0;

    synchronized void generate_worker() {
        byte[] target, header;
        synchronized (CPUMiningWorker.this) {
            target = current_work.target.refHex();
            header = current_work.header.refHex();
        }
        while ((lastNonce >= 0)
                && (Runtime.getRuntime().availableProcessors() - workers.activeCount()) > 0) {
            final int _start = lastNonce;
            int en = lastNonce + nonceStep;
            final int _end = (en <= 0) ? Integer.MAX_VALUE : en;
            new Thread(
                            workers,
                            () -> {
                                try {
                                    long hasher = Constants.initHasher();
                                    for (int nonce = _start; nonce <= _end; nonce++) {
                                        byte[] hash =
                                                Constants.nativeHashing(hasher, header, nonce);
                                        for (int i = hash.length - 1; i >= 0; i--) {
                                            int a = hash[i] & 0xff, b = target[i] & 0xff;
                                            if (a != b) {
                                                if (a < b) {
                                                    invokeNonceFound(nonce);
                                                    return;
                                                }
                                                break;
                                            }
                                        }
                                        calcSpeedPerThread();
                                        Thread.sleep(1L);
                                    }
                                    Thread.sleep(1L);
                                    Constants.destroyHasher(hasher);
                                    generate_worker();
                                } catch (InterruptedException e) {
                                    // ignore
                                }
                            })
                    .start();
            lastNonce = _end + 1;
        }
    }
}
