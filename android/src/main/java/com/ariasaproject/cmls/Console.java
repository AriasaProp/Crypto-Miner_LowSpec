package com.ariasaproject.cmls;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.ariasaproject.cmls.Constants.MSG_CONSOLE_UPDATE;

public class Console {
    private static final DateFormat logDateFormat = new SimpleDateFormat("[HH:mm:ss] ");
    StringBuilder sb=new StringBuilder();
    boolean c_new=false;
    String[] console_a = new String[20];
    final Handler sHandler;

    public Console(Handler h) {
        console_a=new String[20];
        for (int i = 0; i < 20; i++) {console_a[i]="";}
        sHandler = h;
    }

    public void write (String s) {
        Message msg = sHandler.obtainMessage();
        if(s!=null) {
            for (int i = 19; i>0; i--) {console_a[i]=console_a[i-1]; }
            console_a[0] = logDateFormat.format(new Date()) + s;
        }
        Bundle bundle = new Bundle();
        bundle.putString(MSG_CONSOLE_UPDATE, getConsole());
        msg.setData(bundle);
        sHandler.sendMessage(msg);
    }
    public String getConsole() {
        sb=new StringBuilder();
        for (int i=0; i<20; i++)
            sb.append(console_a[i]+'\n');
        return sb.toString();
    }

}