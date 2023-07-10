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
    private final MessageSendListener MSL;
    private final AtomicLong hashes = new AtomicLong(0);
    private final ExecutorService es;
    public CpuMiningWorker(int i_number_of_thread, int priority, MessageSendListener msl) {
        MSL = msl;
        _number_of_thread=i_number_of_thread;
        es = Executors.newFixedThreadPool(_number_of_thread);
    }
    public synchronized void calcSpeedPerThread() {
        long delta = System.currentTimeMillis() - worker_saved_time.get();
        if (delta < 1000) return;
        if (hashes.get() < 0) 
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: hashes acumulator error");
        float _speed = (hashes.get() * 1000.0f) / (float)delta;
        worker_saved_time.set(curr_time);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
    }
    private final AtomicBoolean findingNonce = new AtomicBoolean(true);
    private final AtomicLong worker_saved_time = new AtomicLong(0);
    @Override
    public synchronized boolean doWork(MiningWork i_work) throws Exception {
        if (!es.isTerminated()){
            es.shutdown();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads starting");
        hashes.set(0);
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
        worker_saved_time.set(System.currentTimeMillis());
        for (int i = 0; i < _number_of_thread; i++) {
            es.execute(new Work(i_work, i));
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads started");
        return true;
    }
    @Override
    public synchronized void stopWork() {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killing threads");
        if(!es.isTerminated())
            es.shutdownNow();
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killed threads");
    }

    @Override
    public long getNumberOfHash() {
        return hashes.get();
    }
    
    public boolean getThreadsStatus() {
        return es.isTerminated();
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();
    public synchronized void invokeNonceFound(MiningWork i_work, int i_nonce) {
        stopWork();
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
    class Worker implements Runnable {
        final int _start;
        final MiningWork _work;
        public Worker(MiningWork i_work, int i_start) {
            this._start=i_start;
            this._work=i_work;
        }
        
        @Override
        public void run() {
            final int step = CpuMiningWorker.this._number_of_thread;
            try{
                final MiningWork work = _work;
                Hasher hasher = new Hasher();
                byte[] target = work.target.refHex();
                for(int nonce = _start; nonce >= _start; nonce += step){
                    byte[] hash = hasher.hash(work.header.refHex(), nonce);
                    hashes.incrementAndGet();
                    for (int i = hash.length - 1; i >= 0; i--) {
                        int a = hash[i] & 0xff, b = target[i] & 0xff;
                        if (a != b) {
                            if (a < b) {
                                invokeNonceFound(work,nonce);
                                return;
                            }
                            break;
                        }
                    }
                    calcSpeedPerThread(cur_time);
                    Thread.sleep(10L);
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
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }
}