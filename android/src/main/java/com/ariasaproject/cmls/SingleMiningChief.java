package com.ariasaproject.cmls;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.ariasaproject.cmls.connection.IConnectionEvent;
import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.CpuMiningWorker;
import com.ariasaproject.cmls.worker.IMiningWorker;
import com.ariasaproject.cmls.worker.IWorkerEvent;

import java.util.EventListener;
import java.util.Observable;
import java.util.Observer;

import static com.ariasaproject.cmls.Constants.STATUS_CONNECTING;
import static com.ariasaproject.cmls.Constants.STATUS_ERROR;
import static com.ariasaproject.cmls.Constants.STATUS_MINING;
import static com.ariasaproject.cmls.Constants.STATUS_NOT_MINING;
import static com.ariasaproject.cmls.Constants.STATUS_TERMINATED;

import static com.ariasaproject.cmls.MinerService.MSG_TERMINATED;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_SERVICE_STATUS;

import static com.ariasaproject.cmls.MinerService.MSG_ARG1_UPDATE_SPEED;
import static com.ariasaproject.cmls.MinerService.MSG_ARG1_UPDATE_ACC;
import static com.ariasaproject.cmls.MinerService.MSG_ARG1_UPDATE_REJECT;
import static com.ariasaproject.cmls.MinerService.MSG_ARG1_UPDATE_STATUS;

import static com.ariasaproject.cmls.R.id.parent;

public class SingleMiningChief implements Observer {
    private static final long DEFAULT_SCAN_TIME = 5000;
    private static final long DEFAULT_RETRY_PAUSE = 30000;

    private IMiningConnection mc;
    private IMiningWorker imw;
    private long lastWorkTime;
    private long lastWorkHashes;
    private float speed=0;			//khash/s
    public long accepted=0;
    public long rejected=0;
    public int priority=1;
    private final Handler mainHandler;
    private Console console;
    public IMiningConnection _connection;
    public IMiningWorker _worker;
    private EventListener _eventlistener;

    public String status = STATUS_NOT_MINING;

    public class EventListener extends Observable implements IConnectionEvent,IWorkerEvent {
        private SingleMiningChief _parent;
        private int _number_of_accept;
        private int _number_of_all;

        EventListener(SingleMiningChief i_parent)
        {
            this._parent=i_parent;
            this.resetCounter();
        }
        public void resetCounter()
        {
            this._number_of_accept = this._number_of_all=0;
        }

        @Override
        public void onNewWork(MiningWork i_work)
        {
            try {
                console.write("New work detected!");
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_BLOCK_DETECTED);
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_WORK);
                synchronized(this){
                    this._parent._worker.doWork(i_work);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        @Override
        public void onSubmitResult(MiningWork i_listener, int i_nonce,boolean i_result)
        {
            this._number_of_accept+=(i_result?1:0);
            this._number_of_all++;
            setChanged();
            notifyObservers(i_result ? IMiningWorker.Notification.POW_TRUE : IMiningWorker.Notification.POW_FALSE);
        }
        public boolean onDisconnect()
        {
            return false;
        }
        @Override
        public void onNonceFound(MiningWork i_work, int i_nonce)
        {
            try {
                this._parent._connection.submitWork(i_work,i_nonce);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }
    public SingleMiningChief(IMiningConnection i_connection,IMiningWorker i_worker, Console cons, Handler h) throws Exception
    {
        status= STATUS_CONNECTING;
        speed=0.0f;
        mainHandler=h;
        this.console = cons;
        this._connection=i_connection;
        this._worker=i_worker;
        this._eventlistener=new EventListener(this);
        this._connection.addListener(this._eventlistener);
        this._worker.addListener(this._eventlistener);

    }
    public void startMining() throws Exception
    {
        console.write("Miner: Starting worker thread, priority: "+priority);
        ((StratumMiningConnection)_connection).addObserver(this);
        ((CpuMiningWorker)_worker).addObserver(this);
        MiningWork first_work=this._connection.connect();
        this._eventlistener.resetCounter();
        if(first_work!=null){
            synchronized(this){
                this._worker.doWork(first_work);
            }
        }
    }
    public void stopMining() throws Exception
    {
        console.write("Miner: Worker stopping...");
        console.write("Miner: Worker cooling down");
        console.write("Miner: This can take a few minutes");
        this._connection.disconnect();
        this._worker.stopWork();
        speed=0;
    }

    public void update(Observable o, Object arg) {
        IMiningWorker.Notification n = (IMiningWorker.Notification) arg;
        switch (n) {
        case SYSTEM_ERROR:
            console.write("Miner: System error");
            status = STATUS_ERROR;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case PERMISSION_ERROR:
            console.write("Miner: Permission error");
            status = STATUS_ERROR;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case TERMINATED:
            console.write("Miner: Worker terminated");
            status = STATUS_TERMINATED;
            mainHandler.sendEmptyMessage(MSG_TERMINATED);
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_SPEED, new Float(0)));
            break;
        case CONNECTING:
            console.write("Miner: Worker connecting");
            status = STATUS_CONNECTING;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case AUTHENTICATION_ERROR:
            console.write("Miner: Authentication error");
            status = STATUS_ERROR;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case CONNECTION_ERROR:
            console.write("Miner: Connection error");
            status = STATUS_ERROR;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case COMMUNICATION_ERROR:
            console.write("Miner: Communication error");
            status = STATUS_ERROR;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case LONG_POLLING_FAILED:
            console.write("Miner: Long polling failed");
            status = STATUS_NOT_MINING;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case LONG_POLLING_ENABLED:
            console.write("Miner: Long polling enabled");
            console.write("Miner: Speed updates as work is completed");
            status = STATUS_MINING;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case NEW_BLOCK_DETECTED:
            status = STATUS_MINING;
            console.write("Miner: Detected new block");
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case POW_TRUE:
            console.write("Miner: PROOF OF WORK RESULT: true");
            status = STATUS_MINING;
            accepted+=1;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_ACC, new Long(accepted)));
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case POW_FALSE:
            status = STATUS_MINING;
            rejected+=1;
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_REJECT, new Long(accepted)));
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            break;
        case SPEED:
            if (status.equals(STATUS_TERMINATED) || status.equals(STATUS_NOT_MINING))
                speed = 0;
            else
                speed = (float) ((CpuMiningWorker)_worker).get_speed();
            mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_SPEED, new Float(speed)));
            break;
        case NEW_WORK:
            if (lastWorkTime > 0L) {
                long hashes = _worker.getNumberOfHash() - lastWorkHashes;
                speed = (float) ((CpuMiningWorker)_worker).get_speed();
                status= STATUS_MINING;
                console.write(String.format("Miner: %d Hashes, %.6f Hash/s", hashes, speed));
                mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_SPEED, new Float(speed)));
                mainHandler.sendMessage(mainHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_STATUS, status));
            }
            lastWorkTime = System.currentTimeMillis();
            lastWorkHashes = _worker.getNumberOfHash();
            break;
        default:
            break;
        }
    }
}