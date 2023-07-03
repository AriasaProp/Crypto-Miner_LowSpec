package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.hasher.Hasher;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.AtomicLong;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static com.ariasaproject.cmls.Constants.DEFAULT_RETRYPAUSE;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.activeCount;

public class CpuMiningWorker extends Observable implements IMiningWorker {
    private final int _number_of_thread;
    private final int _thread_priorirty;
    private final Worker[] _workr_thread;
    private final InfoReceive IR;
    private final AtomicLong hashed = new AtomicLong(0);
    public CpuMiningWorker(int i_number_of_thread, int priority, InfoReceive ir) {
        IR = ir;
        _thread_priorirty = priority;
        _number_of_thread=i_number_of_thread;
        _workr_thread = new Worker[_number_of_thread];
        for(int i = 0;i < _number_of_thread; i++){
            _workr_thread[i]=new Worker();
        }

    }
    public void calcSpeedPerThread() {
        long curr_time =  System.currentTimeMillis();
        float delta_time = Math.max(1,curr_time-_last_time)/1000.0f;
        float _speed = hashes.get()/delta_time;
        IR.updateSpeed(_speed);
    }
    private long _last_time=0;

    @Override
    public boolean doWork(MiningWork i_work) throws Exception {
        if(getThreadsStatus()){
            stopWork();
            calcSpeedPerThread();
        }
        hashes.set(0);
        _last_time = System.currentTimeMillis();
        for(int i = 0; i < _number_of_thread; i++){
            Worker workr = _workr_thread[i];
            workr.setWork(i_work, i, _number_of_thread);
            workr.setPriority(_thread_priorirty);
            if (!workr.isAlive()) {
                try {
                    workr.start();
                } catch (IllegalThreadStateException e){
                    workr.interrupt();
                }
            }
        }
        return true;
    }
    @Override
    public void stopWork() throws Exception {
        for (Worker t : _workr_thread) {
            if (t.isAlive()) {
                IR.sendMessage("Worker: Killing thread ID: " + t.getId());
                t.interrupt();
            }
        }
        IR.sendMessage("Worker: Threads killed");
    }

    @Override
    public long getNumberOfHash() {
        return _tot_hashed;
    }
    
    public boolean getThreadsStatus() {
        for (Worker t : _workr_thread) {
            if (t.isAlive()) return true;
        }
        return false;
    }

    public void ConsoleWrite(String c) {
        IR.sendMessage(c);
    }

    private ArrayList<IWorkerEvent> _as_listener = new ArrayList<IWorkerEvent>();
    public synchronized void invokeNonceFound(MiningWork i_work, int i_nonce) {
        IR.sendMessage("Mining: Nonce found! +"+((0xffffffffffffffffL)&i_nonce));
        for(IWorkerEvent i : _as_listener){
            i.onNonceFound(i_work,i_nonce);
        }
    }
    public synchronized void addListener(IWorkerEvent i_listener) {
        this._as_listener.add(i_listener);
    }
    public static interface InfoReceive {
        public abstract void sendMessage(String s);
        public abstract void updateSpeed(float d);
    }
    class Worker extends Thread implements Runnable {
        MiningWork _work;
        int _start;
        int _step;
        public Worker() {}
        public void setWork(MiningWork i_work,int i_start,int i_step) {
            this._work=i_work;
            this._start=i_start;
            this._step=i_step;
        }

        @Override
        public void run() {
            try{
                
                MiningWork work = _work;
                Hasher hasher = new Hasher();
                byte[] target = work.target.refHex();
                for(int nonce = _start; nonce > -1;nonce += _step){
                    byte[] hash = hasher.hash(work.header.refHex(), nonce);
                    for (int i = hash.length - 1; i >= 0; i--) {
                        if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                            break;
                        }
                        if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                            CpuMiningWorker.this.invokeNonceFound(work,nonce);
                            break;
                        }
                    }
                    
                    hashed.incrementAndGet();
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
                calcSpeedPerThread();
                _last_time=System.currentTimeMillis();
            }
        }
    }
}