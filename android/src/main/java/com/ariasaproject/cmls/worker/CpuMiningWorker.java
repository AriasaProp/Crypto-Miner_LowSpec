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

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_SPEED;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.activeCount;

public class CpuMiningWorker extends Observable implements IMiningWorker {
    private final int _number_of_thread;
    private final int _thread_priorirty;
    private final Worker[] _workr_thread;
    private final MessageSendListener MSL;
    private final AtomicLong hashes = new AtomicLong(0);
    public CpuMiningWorker(int i_number_of_thread, int priority, MessageSendListener msl) {
        MSL = msl;
        _thread_priorirty = priority;
        _number_of_thread=i_number_of_thread;
        _workr_thread = new Worker[_number_of_thread];
        for(int i = 0;i < _number_of_thread; i++){
            _workr_thread[i] = new Worker();
        }

    }
    long last_check = 0;
    public void calcSpeedPerThread() {
        calcSpeedPerThread(System.currentTimeMillis());
    }
    public void calcSpeedPerThread(long curr_time) {
        float delta_time = Math.max(1,curr_time-_last_time)/1000.0f;
        float _speed = (float)hashes.get()/delta_time;
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, _speed);
    }
    private long _last_time=0;
    @Override
    public boolean doWork(MiningWork i_work) throws Exception {
        if(getThreadsStatus()){
            stopWork();
        }
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads started");
        hashes.set(0);
        _last_time = System.currentTimeMillis();
        for(int i = 0; i < _number_of_thread; i++){
            Worker workr = _workr_thread[i];
            workr.setWork(i_work, i);
            workr.setPriority(_thread_priorirty);
            if (!workr.isAlive()) {
                try {
                    workr.start();
                    MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Threads started ID: " + workr.getId());
                } catch (IllegalThreadStateException e){
                    workr.interrupt();
                }
            }
        }
        return true;
    }
    @Override
    public void stopWork() throws Exception {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killing threads");
        for (Worker t : _workr_thread) {
            if (t.isAlive()) {
                t.interrupt();
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Worker: Killed thread ID: " + t.getId());
            }
        }
    }

    @Override
    public long getNumberOfHash() {
        return hashes.get();
    }
    
    public boolean getThreadsStatus() {
        for (Worker t : _workr_thread) {
            if (t.isAlive()) return true;
        }
        return false;
    }

    public void ConsoleWrite(String c) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();
    public synchronized void invokeNonceFound(MiningWork i_work, int i_nonce) {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Mining: Nonce found! +"+((0xffffffffffffffffL)&i_nonce));
        for(IWorkerEvent i : _as_listener){
            i.onNonceFound(i_work,i_nonce);
        }
    }
    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }
    class Worker extends Thread implements Runnable {
        MiningWork _work;
        int _start;
        public Worker() {}
        public void setWork(MiningWork i_work,int i_start) {
            this._work=i_work;
            this._start=i_start;
        }

        @Override
        public void run() {
            long saved_time = System.currentTimeMillis();
            final int step = CpuMiningWorker.this._number_of_thread;
            try{
                MiningWork work = _work;
                Hasher hasher = new Hasher();
                byte[] target = work.target.refHex();
                boolean wasFound = false;
                while(!wasFound) {
                    for(int nonce = _start; nonce >= _start;nonce += step){
                        byte[] hash = hasher.hash(work.header.refHex(), nonce);
                        for (int i = hash.length - 1; i >= 0; i--) {
                            if ((hash[i] & 0xff) > (target[i] & 0xff))
                                break;
                            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                                CpuMiningWorker.this.invokeNonceFound(work,nonce);
                                wasFound = true;
                                break;
                            }
                        }
                        hashes.incrementAndGet();
                        Thread.sleep(10L);
                        long cur_time = System.currentTimeMillis();
                        if ( (cur_time - saved_time) >= 1000) {
                            calcSpeedPerThread(cur_time);
                            saved_time = cur_time;
                        }
                    }
                    if (!wasFound) MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0,"Nonce was not fulfill the Hash");
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
                calcSpeedPerThread();
                _last_time = System.currentTimeMillis();
            }
        }
    }
}