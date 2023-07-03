package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.hasher.Hasher;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;

import static com.ariasaproject.cmls.Constants.DEFAULT_PRIORITY;
import static com.ariasaproject.cmls.Constants.DEFAULT_RETRYPAUSE;
import static java.lang.Thread.MIN_PRIORITY;
import static java.lang.Thread.activeCount;

public class CpuMiningWorker extends Observable implements IMiningWorker {
    private class Worker extends Thread implements Runnable {
        MiningWork _work;
        int _start;
        int _step;
        public long number_of_hashed;
        public Worker() {}
        public void setWork(MiningWork i_work,int i_start,int i_step) {
            this._work=i_work;
            this._start=i_start;
            this._step=i_step;
        }
        private final static int NUMBER_OF_ROUND=1; //Original: 100
        public volatile boolean running = false;

        @Override
        public void run() {
            running = true;
            number_of_hashed=0;
            try{
                int nonce = _start;
                MiningWork work = _work;
                Hasher hasher = new Hasher();
                byte[] target = work.target.refHex();
                for(;;){
                    for(long i=NUMBER_OF_ROUND-1;i>=0;i--){
                        byte[] hash = hasher.hash(work.header.refHex(), nonce);
                        for (int i2 = hash.length - 1; i2 >= 0; i2--) {
                            if ((hash[i2] & 0xff) > (target[i2] & 0xff)) {
                                break;
                            }
                            if ((hash[i2] & 0xff) < (target[i2] & 0xff)) {
                                CpuMiningWorker.this.invokeNonceFound(work,nonce);
                                break;
                            }
                        }
                        nonce += _step;
                    }
                    this.number_of_hashed+=NUMBER_OF_ROUND;
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
                IR.sendMessage("Thread killed. #Hashes="+this.number_of_hashed);
                calcSpeedPerThread(number_of_hashed);
                _last_time=System.currentTimeMillis();
            }
        }
    }
    private int _number_of_thread;
    private int _thread_priorirty;
    private Worker[] _workr_thread;
    private long _retrypause;
    private final InfoReceive IR;
    public CpuMiningWorker(int i_number_of_thread , long retry_pause, int priority, InfoReceive ir) {
        IR = ir;
        _thread_priorirty = priority;
        this._retrypause = retry_pause;
        this._number_of_thread=i_number_of_thread;
        this._workr_thread=new Worker[10];
        for(int i=this._number_of_thread-1;i>=0;i--){
            this._workr_thread[i]=new Worker();
        }

    }
    public void calcSpeedPerThread(long numOfHashes) {
        long curr_time =  System.currentTimeMillis();
        double delta_time = Math.max(1,curr_time-this._last_time)/1000.0;
        double _speed = ((double)numOfHashes/delta_time);
        IR.updateSpeed(_speed);
    }
    private long _last_time=0;
    private long _num_hashed=0;
    private long _tot_hashed=0;

    @Override
    public boolean doWork(MiningWork i_work) throws Exception {
        if(i_work!=null){
            this.stopWork();
            long hashes=0;
            for(int i=this._number_of_thread-1;i>=0;i--){
                hashes+=this._workr_thread[i].number_of_hashed;
            }
            _num_hashed = hashes;
            _tot_hashed += _num_hashed;
            double delta_time = Math.max(1,System.currentTimeMillis()-this._last_time)/1000.0;
            double _speed = ((double)_num_hashed/delta_time);
            IR.updateSpeed(_speed);
        }
        this._last_time=System.currentTimeMillis();
        for(int i=this._number_of_thread-1;i>=0;i--){
            this._workr_thread[i] = null;
            System.gc();
            this._workr_thread[i]=new Worker();
        }
        for(int i=this._number_of_thread-1;i>=0;i--){
            this._workr_thread[i].setWork(i_work,(int)i,this._number_of_thread);
            _workr_thread[i].setPriority(_thread_priorirty);
            if (_workr_thread[i].isAlive() == false) {
                try {
                    _workr_thread[i].start();
                } catch (IllegalThreadStateException e){
                    _workr_thread[i].interrupt();
                }
            }
        }
        return true;
    }
    @Override
    public void stopWork() throws Exception {
        for (Worker t : _workr_thread) {
            if (t != null) {
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
            if (t != null) {
                if (t.isAlive() == true) return true;
            }
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
        public abstract void updateSpeed(double d);
    }
}