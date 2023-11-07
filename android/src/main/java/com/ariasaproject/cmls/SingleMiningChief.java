package com.ariasaproject.cmls;

import static com.ariasaproject.cmls.Constants.MSG_STATE;
import static com.ariasaproject.cmls.Constants.MSG_STATE_ONSTOP;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_ACCEPTED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_CONSOLE;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_REJECTED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_SPEED;
import static com.ariasaproject.cmls.Constants.MSG_UPDATE_STATUS;
import static com.ariasaproject.cmls.Constants.STATUS_CONNECTING;
import static com.ariasaproject.cmls.Constants.STATUS_ERROR;
import static com.ariasaproject.cmls.Constants.STATUS_MINING;
import static com.ariasaproject.cmls.Constants.STATUS_TERMINATED;

import com.ariasaproject.cmls.connection.IConnectionEvent;
import com.ariasaproject.cmls.connection.IMiningConnection;
import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.worker.IMiningWorker;
import com.ariasaproject.cmls.worker.IWorkerEvent;

import java.util.EventListener;
import java.util.Observable;
import java.util.Observer;

public class SingleMiningChief implements Observer {

    private IMiningConnection mc;
    private IMiningWorker imw;
    private final MessageSendListener MSL;
    public IMiningConnection _connection;
    public IMiningWorker _worker;
    private EventListener _eventlistener;

    public class EventListener extends Observable implements IConnectionEvent, IWorkerEvent {
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
                MSL.sendMessage(
                        MSG_UPDATE,
                        MSG_UPDATE_CONSOLE,
                        0,
                        String.format(
                                "Miner: %d Hashes then New work detected",
                                _worker.getNumberOfHash()));
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_SPEED, 0, 0.0f);
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_MINING);
                _worker.doWork(i_work);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSubmitResult(MiningWork i_listener, int i_nonce, boolean i_result) {
            if (i_result) {
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_ACCEPTED, 0, null);
            } else {
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_REJECTED, 0, null);
            }
        }

        public boolean onDisconnect() {
            return false;
        }

        @Override
        public void onNonceFound(MiningWork i_work, int i_nonce) {
            MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Event: Nonce found " + i_nonce);
            try {
                _connection.submitWork(i_work, i_nonce);
            } catch (Exception e) {
                MSL.sendMessage(
                        MSG_UPDATE,
                        MSG_UPDATE_CONSOLE,
                        0,
                        "Event: Nonce submit error  " + e.getMessage());
            }
        }
    }

    public SingleMiningChief(
            IMiningConnection i_connection, IMiningWorker i_worker, MessageSendListener msl)
            throws Exception {
        MSL = msl;
        this._connection = i_connection;
        this._worker = i_worker;
        this._eventlistener = new EventListener();
        this._connection.addListener(this._eventlistener);
        this._worker.addListener(this._eventlistener);
    }

    public void startMining() throws Exception {
        MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Starting worker thread");
        ((StratumMiningConnection) _connection).addObserver(this);
        MiningWork first_work = this._connection.connect();
        this._eventlistener.resetCounter();
        if (first_work != null) {
            this._worker.doWork(first_work);
        }
    }

    public void stopMining() throws Exception {
        MSL.sendMessage(
                MSG_UPDATE,
                MSG_UPDATE_CONSOLE,
                0,
                "Miner Worker stopping... cooling down \nThis can take a few minutes");
        this._connection.disconnect();
        this._worker.stopWork();
    }

    public void update(Observable o, Object arg) {
        IMiningWorker.Notification n = (IMiningWorker.Notification) arg;
        switch (n) {
            case TERMINATED:
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Worker terminated");
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_TERMINATED);
                MSL.sendMessage(MSG_STATE, MSG_STATE_ONSTOP, 0, null);
                break;
            case CONNECTING:
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Worker connecting");
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_CONNECTING);
                break;
            case CONNECTION_ERROR:
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Connection error(host,port,subscribe,auth)");
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
                break;
            case COMMUNICATION_ERROR:
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "Miner: Communication error");
                MSL.sendMessage(MSG_UPDATE, MSG_UPDATE_STATUS, 0, STATUS_ERROR);
                break;
            default:
                break;
        }
    }
}
