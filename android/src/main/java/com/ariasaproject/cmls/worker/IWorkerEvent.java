package com.ariasaproject.cmls.worker;

import com.ariasaproject.cmls.MiningWork;

public interface IWorkerEvent
{
    public void onNonceFound(MiningWork i_work, int i_nonce);
}
