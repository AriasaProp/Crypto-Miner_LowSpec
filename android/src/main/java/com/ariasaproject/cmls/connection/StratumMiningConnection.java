package com.ariasaproject.cmls.connection;

import android.os.AsyncTask;

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

import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.MessageSendListener;
import com.ariasaproject.cmls.StratumMiningWork;
import com.ariasaproject.cmls.stratum.StratumJson;
import com.ariasaproject.cmls.stratum.StratumJsonMethodGetVersion;
import com.ariasaproject.cmls.stratum.StratumJsonMethodMiningNotify;
import com.ariasaproject.cmls.stratum.StratumJsonMethodReconnect;
import com.ariasaproject.cmls.stratum.StratumJsonMethodSetDifficulty;
import com.ariasaproject.cmls.stratum.StratumJsonMethodShowMessage;
import com.ariasaproject.cmls.stratum.StratumJsonResult;
import com.ariasaproject.cmls.stratum.StratumJsonResultStandard;
import com.ariasaproject.cmls.stratum.StratumJsonResultSubscribe;
import com.ariasaproject.cmls.stratum.StratumSocket;
import com.ariasaproject.cmls.stratum.StratumWorkBuilder;
import com.ariasaproject.cmls.worker.IMiningWorker;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;

public class StratumMiningConnection extends Observable implements IMiningConnection {
    private class SubmitOrder {
        public SubmitOrder(long i_id, StratumMiningWork i_work, int i_nonce) {
            id = i_id;
            work = i_work;
            nonce = i_nonce;
            return;
        }

        public final long id;
        public final MiningWork work;
        public final int nonce;
    }
    private class AsyncRxSocketThread extends Thread {
        private ArrayList<SubmitOrder> _submit_q = new ArrayList<SubmitOrder>();
        private ArrayList<StratumJson> _json_q = new ArrayList<StratumJson>();
        private StratumMiningConnection _parent;

        public AsyncRxSocketThread(StratumMiningConnection i_parent) throws SocketException {
            _parent = i_parent;
            _parent._sock.setSoTimeout(100);
        }

        public void run() {
            for (; ; ) {
                try {
                    JsonNode jn = _parent._sock.recvStratumJson();
                    if (jn == null) {
                        Thread.sleep(1);
                        continue;
                    }
                    try {
                        _parent._msl.sendMessage (MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "JSONRPC: " + new StratumJsonMethodGetVersion(jn).val);
                    } catch (Exception e) {}
                    try {
                        _parent.cbNewMiningNotify(new StratumJsonMethodMiningNotify(jn));
                    } catch (Exception e) {}
                    try {
                        StratumJsonMethodReconnect j = new StratumJsonMethodReconnect(jn);
                        _parent._msl.sendMessage (MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, "url: " + j.host + " port: " + j.port);
                    } catch (Exception e) {}
                    try {
                        _parent.cbNewMiningDifficulty(new StratumJsonMethodSetDifficulty(jn));
                    } catch (Exception e) {}
                    try {
                        _parent._msl.sendMessage (MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, new StratumJsonMethodShowMessage(jn).val);
                    } catch (Exception e) {
                    }
                    try {
                        synchronized (_json_q) {
                            _json_q.add(new StratumJsonResultSubscribe(jn));
                        }
                        semaphore.release();
                    } catch (Exception e) {
                    }
                    try {
                        StratumJsonResultStandard sjson = new StratumJsonResultStandard(jn);
                        SubmitOrder so = null;
                        synchronized (_submit_q) {
                            for (SubmitOrder i : _submit_q) {
                                if (i.id == sjson.id) {
                                    _submit_q.remove(i);
                                    so = i;
                                    break;
                                }
                            }
                        }
                        if (so != null) {
                            _parent.cbSubmitRecv(so, sjson);
                        }
                        synchronized (_json_q) {
                            _json_q.add(i_json);
                        }
                        semaphore.release();
                    } catch (Exception e) {
                    }
                } catch (SocketTimeoutException e) {
                    if (isInterrupted())
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private Semaphore semaphore = new Semaphore(0);

        public StratumJson waitForJsonResult(long i_id, Class<?> i_class, int i_wait_for_msec) {
            long time_out = i_wait_for_msec;
            do {
                long s = System.currentTimeMillis();
                try {
                    if (!semaphore.tryAcquire(time_out, TimeUnit.MILLISECONDS)) {
                        return null;
                    }
                } catch (InterruptedException e) {
                    return null;
                }
                synchronized (_json_q) {
                    for (StratumJson json : _json_q) {
                        if (!(json.getClass() == i_class)) {
                            continue;
                        }
                        StratumJsonResult jr = (StratumJsonResult) json;
                        if ((jr.id == null) || (jr.id != i_id)) continue;
                        _json_q.remove(json);
                        return json;
                    }
                }
                time_out -= (System.currentTimeMillis() - s);
            } while (time_out > 0);
            return null;
        }

        public void addSubmitOrder(SubmitOrder i_submit_id) {
            synchronized (_submit_q) {
                _submit_q.add(i_submit_id);
            }
        }
    }

    private final String CLIENT_NAME = "CMLS";
    private final String _uid;
    private final String _pass;
    private final URI _server;
    private final MessageSendListener _msl;
    private StratumSocket _sock = null;
    private final AsyncRxSocketThread _rx_thread = new AsyncRxSocketThread(this);

    public StratumMiningConnection(String i_url, String i_userid, String i_password, MessageSendListener msl) throws Exception {
        _uid = i_userid;
        _pass = i_password;
        _server = new URI(i_url);
        _msl = msl;
    }

    private StratumJsonMethodSetDifficulty _last_difficulty = null;
    private StratumJsonMethodMiningNotify _last_notify = null;

    public MiningWork connect() throws RuntimeException {
        setChanged();
        notifyObservers(IMiningWorker.Notification.CONNECTING);
        // Connect to host
        try {
            MiningWork ret = null;
            _sock = new StratumSocket(_server);
            
            _rx_thread.start();
            int i;

            // subscribe
            StratumJsonResultSubscribe subscribe = null;
            {
                for (i = 0; i < 3; i++) {
                    subscribe =
                            (StratumJsonResultSubscribe)
                                    _rx_thread.waitForJsonResult(
                                            _sock.subscribe(CLIENT_NAME),
                                            StratumJsonResultSubscribe.class,
                                            3000);
                    if (subscribe == null || subscribe.error != null) {
                        continue;
                    }
                    break;
                }
                if (i == 3) {
                    throw new RuntimeException("Stratum subscribe error.");
                }
            }

            // Authorize and make a 1st work.
            for (i = 0; i < 3; i++) {
                StratumJsonResultStandard auth =
                        (StratumJsonResultStandard)
                                _rx_thread.waitForJsonResult(
                                        _sock.authorize(_uid, _pass),
                                        StratumJsonResultStandard.class,
                                        3000);
                if (auth == null || auth.error != null) {
                    continue;
                }
                if (!auth.result) {
                    // autentications result error
                }
                synchronized (_data_lock) {
                    _work_builder = new StratumWorkBuilder(subscribe);
                    if (_last_difficulty != null) {
                        _work_builder.setDiff(_last_difficulty);
                    }
                    if (_last_notify != null) {
                        _work_builder.setNotify(_last_notify);
                    }
                    ret = _work_builder.buildMiningWork();
                }
                // Complete!
                return ret;
            }
            setChanged();
            notifyObservers(IMiningWorker.Notification.AUTHENTICATION_ERROR);
            throw new RuntimeException("Stratum authorize process failed.");
        } catch (RuntimeException|UnknownHostException e) {
            setChanged();
            notifyObservers(IMiningWorker.Notification.CONNECTION_ERROR);
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnect() throws RuntimeException {
        try {
            _rx_thread.interrupt();
            _rx_thread.join();
            _sock.close();
            _sock = null;
            setChanged();
            notifyObservers(IMiningWorker.Notification.TERMINATED);
            synchronized (_data_lock) {
                _work_builder = null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object _data_lock = new Object();
    private StratumWorkBuilder _work_builder = null;

    public MiningWork getWork() {
        MiningWork work = null;
        synchronized (_data_lock) {
            if (_work_builder == null) {
                return null;
            }
            try {
                work = _work_builder.buildMiningWork();
            } catch (Exception e) {
                return null;
            }
        }
        return work;
    }

    private ArrayList<IConnectionEvent> _as_listener = new ArrayList<IConnectionEvent>();

    public void addListener(IConnectionEvent i_listener) {
        _as_listener.add(i_listener);
        return;
    }

    private void cbNewMiningNotify(StratumJsonMethodMiningNotify i_notify) {
        synchronized (_data_lock) {
            if (_work_builder == null) {
                _last_notify = i_notify;
                return;
            }
        }
        try {
            _work_builder.setNotify(i_notify);
        } catch (RuntimeException e) {
        }
        MiningWork w = getWork();
        if (w == null) {
            return;
        }
        for (IConnectionEvent i : _as_listener) {
            i.onNewWork(w);
        }
    }

    private void cbNewMiningDifficulty(StratumJsonMethodSetDifficulty i_difficulty) {
        synchronized (_data_lock) {
            if (_work_builder == null) {
                _last_difficulty = i_difficulty;
                return;
            }
        }
        _work_builder.setDiff(i_difficulty);
        MiningWork w = getWork();
        if (w == null) {
            return;
        }
        for (IConnectionEvent i : _as_listener) {
            i.onNewWork(w);
        }
    }

    private void cbSubmitRecv(SubmitOrder so, StratumJsonResultStandard i_result) {
        for (IConnectionEvent i : _as_listener) {
            i.onSubmitResult(so.work, so.nonce, i_result.error == null);
        }
    }

    public void submitWork(MiningWork i_work, int i_nonce) throws RuntimeException {
        if (!(i_work instanceof StratumMiningWork)) {
            throw new RuntimeException();
        }
        StratumMiningWork w = (StratumMiningWork) i_work;
        String ntime = w.data.getStr(StratumMiningWork.INDEX_OF_NTIME, 4);
        try {
            long id = _sock.submit(i_nonce, _uid, w.job_id, w.xnonce2, ntime);
            SubmitOrder so = new SubmitOrder(id, w, i_nonce);
            _rx_thread.addSubmitOrder(so);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
