package com.ariasaproject.cmls;

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

import static com.ariasaproject.cmls.MinerService.MSG_STATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;

import static com.ariasaproject.cmls.MinerService.MSG_STATE_ONSTOP;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_SPEED;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_ACC;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_REJECT;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_STATUS;

public class SingleMiningChief implements Observer {
    private static final long DEFAULT_SCAN_TIME = 5000;
    private static final long DEFAULT_RETRY_PAUSE = 30000;

    private IMiningConnection mc;
    private IMiningWorker imw;
    private long lastWorkTime;
    public int priority=1;
    private final MessageSendListener MSL;
    public IMiningConnection _connection;
    public IMiningWorker _worker;
    private EventListener _eventlistener;

    public class EventListener extends Observable implements IConnectionEvent,IWorkerEvent {
        private int _number_of_accept;
        private int _number_of_all;

        EventListener() {
            this.resetCounter();
        }
        public void resetCounter() {
            this._number_of_accept = this._number_of_all = 0;
        }

        @Override
        public void onNewWork(MiningWork i_work) {
            try {
                SingleMiningChief.this.MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "New work detected!");
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_BLOCK_DETECTED);
                setChanged();
                notifyObservers(IMiningWorker.Notification.NEW_WORK);
                SingleMiningChief.this._worker.doWork(i_work);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        @Override
        public void onSubmitResult(MiningWork i_listener, int i_nonce,boolean i_result) {
            this._number_of_accept+=(i_result?1:0);
            this._number_of_all++;
            setChanged();
            notifyObservers(i_result ? IMiningWorker.Notification.POW_TRUE : IMiningWorker.Notification.POW_FALSE);
        }
        public boolean onDisconnect() {
            return false;
        }
        @Override
        public void onNonceFound(MiningWork i_work, int i_nonce) {
            try {
                SingleMiningChief.this.MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Event: Nonce found "+i_nonce);
                SingleMiningChief.this._connection.submitWork(i_work,i_nonce);
            } catch (Exception e) {
                e.printStackTrace();
                SingleMiningChief.this.MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Event: Nonce submit error  "+e.getMessage());
            }
        }

    }
    public SingleMiningChief(IMiningConnection i_connection,IMiningWorker i_worker, MessageSendListener msl) throws Exception {
        MSL = msl;
        this._connection=i_connection;
        this._worker=i_worker;
        this._eventlistener=new EventListener();
        this._connection.addListener(this._eventlistener);
        this._worker.addListener(this._eventlistener);
    }
    public void startMining() throws Exception {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Starting worker thread, priority: "+priority);
        ((StratumMiningConnection)_connection).addObserver(this);
        MiningWork first_work=this._connection.connect();
        this._eventlistener.resetCounter();
        if(first_work!=null){
            this._worker.doWork(first_work);
        }
    }
    public void stopMining() throws Exception {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner Worker stopping... cooling down \nThis can take a few minutes");
        this._connection.disconnect();
        this._worker.stopWork();
    }

    public void update(Observable o, Object arg) {
        IMiningWorker.Notification n = (IMiningWorker.Notification) arg;
        switch (n) {
        case PERMISSION_ERROR:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Permission error");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
            break;
        case TERMINATED:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Worker terminated");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_TERMINATED);
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0,new Float(0));
            MSL.sendMessage(MSG_STATE, MSG_STATE_ONSTOP, 0, null);
            break;
        case CONNECTING:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Worker connecting");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_CONNECTING);
            break;
        case AUTHENTICATION_ERROR:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Authentication error");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
            break;
        case CONNECTION_ERROR:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Connection error");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
            break;
        case COMMUNICATION_ERROR:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Communication error");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
            break;
        case LONG_POLLING_FAILED:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Long polling failed");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_NOT_MINING);
            break;
        case LONG_POLLING_ENABLED:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Long polling enabled");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Speed updates as work is completed");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
            break;
        case NEW_BLOCK_DETECTED:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Detected new block");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
            break;
        case POW_TRUE:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: PROOF OF WORK RESULT: true");
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_ACC, 0, null);
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
            break;
        case POW_FALSE:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_REJECT, 0, null);
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
            break;
        case NEW_WORK:
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, String.format("Miner: %d Hashes", _worker.getNumberOfHash()));
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
            lastWorkTime = System.currentTimeMillis();
            break;
        default:
            break;
        }
    }
}