package com.ariasaproject.cmls.worker;

import static com.ariasaproject.cmls.Constants.MSG_STATE;
import static com.ariasaproject.cmls.Constants.MSG_STATE_ONSTOP;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;

import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.hasher.Hasher;

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
    private volatile long worker_saved_time = 0;

    public synchronized void calcSpeedPerThread() {
        hashes++;
        long curr_time = System.currentTimeMillis();
        long delta = curr_time - worker_saved_time;
        if (delta < 1000) return;
        if (hashes < 0)
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: hashes acumulator error");
        float _speed = (hashes * 1000.0f) / (float) delta;
        worker_saved_time = curr_time;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
    }

    @Override
    public synchronized boolean doWork(MiningWork i_work) throws Exception {
        if (workers.activeCount() > 0) {
            workers.interrupt();
        }
        System.gc();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Worker: Threads starting");
        hashes = 0;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        worker_saved_time = System.currentTimeMillis();
        lastNonce = 0;
        generate_worker(i_work);
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

    public synchronized void invokeNonceFound(MiningWork i_work, int i_nonce) {
        if (workers.activeCount() > 0) {
            workers.interrupt();
        }
        MSL.sendMessage(
                MSG_UPDATE,
                MSG_UPDATE_CONSOLE,
                0,
                "Mining: Nonce found! +" + ((0xffffffffffffffffL) & i_nonce));
        if (i_nonce < _number_of_thread)
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Mining: Weired Nonce");
        for (IWorkerEvent i : _as_listener) {
            i.onNonceFound(i_work, i_nonce);
        }
    }

    public synchronized void addListener(IWorkerEvent i_listener) throws GeneralSecurityException {
        this._as_listener.add(i_listener);
    }
    private static final int nonceStep = 4094;
    private volatile int lastNonce = 0;
    
    synchronized void generate_worker(MiningWork work) {
        while ((lastNonce >= 0) && (Runtime.getRuntime().availableProcessors() - workers.activeCount()) > 0) {
            final int _start = lastNonce;
            int en = lastNonce + nonceStep;
            final int _end = (en <= 0) ? Integer.MAX_VALUE : en;
            new Thread(workers, () -> {
                try {
                    long hasher = Hasher.initialize();
                    byte[] target = work.target.refHex();
                    for (int nonce = _start; nonce <= _end; nonce++) {
                        byte[] hash = Hasher.nativeHashing(hasher, work.header.refHex(), nonce);
                        for (int i = hash.length - 1; i >= 0; i--) {
                            int a = hash[i] & 0xff, b = target[i] & 0xff;
                            if (a != b) {
                                if (a < b) {
                                    invokeNonceFound(work, nonce);
                                    return;
                                }
                                break;
                            }
                        }
                        calcSpeedPerThread();
                        Thread.sleep(1L);
                    }
                    Thread.sleep(1L);
                    Hasher.deinitialize(hasher);
                    generate_worker(work);
                } catch (InterruptedException e) {
                    // ignore
                }
            }).start();
            lastNonce = _end + 1;
        }
    }
}
