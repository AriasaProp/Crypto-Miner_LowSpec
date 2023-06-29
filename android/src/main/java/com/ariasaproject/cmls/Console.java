package com.ariasaproject.cmls;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.lang.Object;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE;
import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_CONSOLE;

public class Console {
    final Handler sHandler;

    public Console(Handler h) {
        sHandler = h;
    }

    public void write (String s) {
        if(s!=null)
            sHandler.sendMessage(sHandler.obtainMessage(MSG_UPDATE, MSG_UPDATE_CONSOLE, 0, s));
    }
}