package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.hasher.Hasher;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_SPEED;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.activeCount;

public class CpuMiningWorker extends Observable implements IMiningWorker {
    private final int _number_of_thread;
    private final Worker[] _workr_thread;
    private final MessageSendListener MSL;
    private final AtomicLong hashes = new AtomicLong(0);
    private final ExecutorService es;
    public CpuMiningWorker(int i_number_of_thread, int priority, MessageSendListener msl) {
        es = Executors.newFixedThreadPool(i_number_of_thread);
        MSL = msl;
        _number_of_thread=i_number_of_thread;
        _workr_thread = new Worker[_number_of_thread];
        for(int i = 0;i < _number_of_thread; i++){
            _workr_thread[i] = new Worker(i);
            _workr_thread[i].setPriority(priority);
        }
    }
    public synchronized void calcSpeedPerThread(long curr_time) {
        long delta = curr_time-worker_saved_time.get();
        if (delta < 1000) return;
        float _speed = (hashes.get() * 1000.0f) / (float)delta;
        worker_saved_time.set(curr_time);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
    }
    private final AtomicBoolean findingNonce = new AtomicBoolean(true);
    private final AtomicLong worker_saved_time = new AtomicLong(0);
    @Override
    public synchronized boolean doWork(MiningWork i_work) throws Exception {
        while (findingNonce.get()){
            findingNonce.set(false);
            for (Worker t : _workr_thread)
                t.join();
        }
        findingNonce.set(true);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads starting");
        hashes.set(0);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        worker_saved_time.set(System.currentTimeMillis());
        for(Worker workr : _workr_thread){
            workr.setWork(i_work);
            workr.start();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads started");
        return true;
    }
    @Override
    public synchronized void stopWork() throws Exception {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killing threads");
        findingNonce.set(false);
        for (Worker t : _workr_thread)
            t.join();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killed threads");
    }

    @Override
    public long getNumberOfHash() {
        return hashes.get();
    }
    
    public boolean getThreadsStatus() {
        return findingNonce.get();
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();
    public synchronized void invokeNonceFound(MiningWork i_work, int i_nonce) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Mining: Nonce found! +"+((0xffffffffffffffffL)&i_nonce));
        if (i_nonce < _number_of_thread)
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Mining: Weired Nonce");
        for(IWorkerEvent i : _as_listener){
            i.onNonceFound(i_work,i_nonce);
        }
    }
    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }
    class Worker extends Thread {
        final int _start;
        public Worker(int i_start) {
            this._start=i_start;
        }
        MiningWork _work;
        public void setWork(MiningWork i_work) {
            this._work=i_work;
        }
        @Override
        public void run() {
            final int step = CpuMiningWorker.this._number_of_thread;
            try{
                MiningWork work = _work;
                Hasher hasher = new Hasher();
                byte[] target = work.target.refHex();
                for(int nonce = _start; (nonce >= _start) && findingNonce.get(); nonce += step){
                    byte[] hash = hasher.hash(work.header.refHex(), nonce);
                    hashes.incrementAndGet();
                    for (int i = hash.length - 1; i >= 0; i--) {
                        int a = hash[i] & 0xff, b = target[i] & 0xff;
                        if (a != b) {
                            if (a < b) {
                                findingNonce.set(false);
                                CpuMiningWorker.this.invokeNonceFound(work,nonce);
                                return;
                            }
                            break;
                        }
                    }
                    long cur_time = System.currentTimeMillis();
                    if ( (cur_time - worker_saved_time.get()) >= 1000) {
                        calcSpeedPerThread(cur_time);
                    }
                }
            } catch (GeneralSecurityException e){
                e.printStackTrace();
                setChanged();
                notifyObservers(Notification.SYSTEM_ERROR);
                try {
                    stopWork();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}