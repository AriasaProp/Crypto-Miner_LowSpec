package com.ariasaproject.cmls;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.Date;
import java.lang.Object;

import static com.ariasaproject.cmls.MinerService.MSG_UPDATE_SERVICE_STATUS;
import static com.ariasaproject.cmls.MinerService.MSG_ARG1_UPDATE_CONSOLE;

public class Console {
    final Handler sHandler;

    public Console(Handler h) {
        sHandler = h;
    }

    public void write (String s) {
        if(s!=null)
            sHandler.sendMessage(sHandler.obtainMessage(MSG_UPDATE_SERVICE_STATUS, MSG_ARG1_UPDATE_CONSOLE, 0, s));
    }
}