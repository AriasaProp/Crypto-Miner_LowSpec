package com.ariasaproject.cmls.connection;

import android.os.AsyncTask;

import com.ariasaproject.cmls.MiningWork;
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
import com.ariasaproject.cmls.stratum.StratumWorkBuilder;
import com.ariasaproject.cmls.worker.IMiningWorker;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
            //_parent._ids.setSoTimeout(100);
        }

        public void run() {
            for (; ; ) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jn = mapper.readTree(recv());
                    // parse method
                    try {
                        StratumJsonMethodGetVersion sjson = new StratumJsonMethodGetVersion(jn);
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        _parent.cbNewMiningNotify(new StratumJsonMethodMiningNotify(jn));
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        new StratumJsonMethodReconnect(jn);
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        _parent.cbNewMiningDifficulty(new StratumJsonMethodSetDifficulty(jn));
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        StratumJsonMethodShowMessage sjson = new StratumJsonMethodShowMessage(jn);
                        
                        break;
                    } catch (Exception e) {
                    }
                    try {
                        StratumJsonResultSubscribe sjson = new StratumJsonResultSubscribe(jn);
                        synchronized (_json_q) {
                            _json_q.add(sjson);
                        }
                        semaphore.release();
                        break;
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
                            _json_q.add(sjson);
                        }
                        semaphore.release();
                        break;
                    } catch (Exception e) {
                    }
                    Thread.sleep(1);
                    continue;
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
    private String _uid;
    private String _pass;
    private URI _server;
    private AtomicLong _ids = new AtomicLong(0);
    private AsyncRxSocketThread _rx_thread;

    public StratumMiningConnection(String i_url, String i_userid, String i_password)
            throws RuntimeException {
        _pass = i_password;
        _uid = i_userid;
        try {
            _server = new URI(i_url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private StratumJsonMethodSetDifficulty _last_difficulty = null;
    private StratumJsonMethodMiningNotify _last_notify = null;

    public MiningWork connect() throws RuntimeException {
        setChanged();
        notifyObservers(IMiningWorker.Notification.CONNECTING);
        // Connect to host
        try {
            MiningWork ret = null;
            try {
                if (!connectN(_server.getHost(), _server.getPort())) throw new RuntimeException("failed to connect");
                _ids.set(0);
            } catch (Exception e) {
                setChanged();
                notifyObservers(IMiningWorker.Notification.CONNECTION_ERROR);
                e.printStackTrace();
            }

            _rx_thread = new AsyncRxSocketThread(this);
            _rx_thread.start();
            int i;

            // subscribe
            StratumJsonResultSubscribe subscribe = null;
            for (i = 0; i < 3; i++) {
                long id = _ids.incrementAndGet();
                if(send("{\"id\": " + id + ", \"method\": \"mining.subscribe\", \"params\": [" + CLIENT_NAME + "]}\n")) {
                  subscribe = (StratumJsonResultSubscribe)_rx_thread.waitForJsonResult(id,StratumJsonResultSubscribe.class,3000);
                  if (subscribe == null || subscribe.error != null) 
                      continue;
                  break;
                }
            }
            if (i >= 3)
                throw new RuntimeException("Stratum subscribe error.");

            // Authorize and make a 1st work.
            for (i = 0; i < 3; i++) {
                long id = _ids.incrementAndGet();
                if(!send("{\"id\": " + id + ", \"method\": \"mining.authorize\", \"params\": [\"" + _uid + "\",\"" + _pass + "\"]}\n")) {
                  continue;
                }
                StratumJsonResultStandard auth = (StratumJsonResultStandard)_rx_thread.waitForJsonResult( id, StratumJsonResultStandard.class,3000);
                if (auth == null || auth.error != null)continue;
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
            throw new RuntimeException("Stratum authorize process failed.");
        } catch (Exception e) {
            setChanged();
            notifyObservers(IMiningWorker.Notification.CONNECTION_ERROR);
            throw new RuntimeException(e);
        }
    }

    public void disconnect() throws RuntimeException {
        try {
            _rx_thread.interrupt();
            _rx_thread.join();
            disconnectN();
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
            long id = _ids.incrementAndGet();
            String sn = String.format("%08x",
                            (((i_nonce & 0xff000000) >> 24)
                            | ((i_nonce & 0x00ff0000) >> 8)
                            | ((i_nonce & 0x0000ff00) << 8)
                            | ((i_nonce & 0x000000ff) << 24)));
            // {"method": "mining.submit", "params": ["nyajira.xa", "e4c", "00000000", "52b7a1a9", "79280100"], "id":4}
            if(!send("{\"id\": " + id + ", \"method\": \"mining.submit\", \"params\": [\"" + _uid + "\", \"" + w.job_id + "\",\"" + w.xnonce2 + "\",\"" + sn + "\",\"" + ntime + "\"]}\n"))
              new RuntimeException("Failed submit 3 times."); 
            SubmitOrder so = new SubmitOrder(id, w, i_nonce);
            _rx_thread.addSubmitOrder(so);
        } catch (IOException|RuntimeException e) {
            throw new RuntimeException(e);
        }
    }
    private native boolean connectN(String host, int port);
    private native boolean send(String msg);
    private native String recv();
    private native void disconnectN();
    
}
