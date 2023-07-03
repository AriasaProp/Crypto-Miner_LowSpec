package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;

public interface IMiningWorker {
    public enum Notification {
        SYSTEM_ERROR,
        PERMISSION_ERROR,
        CONNECTION_ERROR,
        AUTHENTICATION_ERROR,
        COMMUNICATION_ERROR,
        LONG_POLLING_FAILED,
        LONG_POLLING_ENABLED,
        CONNECTING,
        NEW_BLOCK_DETECTED,
        NEW_WORK,
        POW_TRUE,
        POW_FALSE,
        TERMINATED
    };
    public boolean doWork(MiningWork i_work) throws Exception;
    public void stopWork() throws Exception;
    public long getNumberOfHash();
    public void addListener(IWorkerEvent i_listener) throws Exception;
}
