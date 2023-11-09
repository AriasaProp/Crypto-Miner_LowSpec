package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;

public interface IMiningWorker {
    public enum Notification {
        CONNECTION_ERROR,
        AUTHENTICATION_ERROR,
        COMMUNICATION_ERROR,
        CONNECTING,
        POW_TRUE,
        POW_FALSE,
        TERMINATED
    };

    public void doWork(MiningWork i_work) throws Exception;

    public void stopWork();

    public long getNumberOfHash();

    public void addListener(IWorkerEvent i_listener) throws Exception;
}
