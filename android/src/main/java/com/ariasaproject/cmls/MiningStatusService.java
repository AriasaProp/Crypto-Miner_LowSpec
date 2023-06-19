package com.ariasaproject.cmls;

import java.lang.Object;
import java.util.ArrayList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MiningStatusService extends Object{
    private static final DateFormat logDateFormat = new SimpleDateFormat("[HH:mm:ss] ");
    public static class ConsoleItem extends Object{
        public final String time, msg;
        public ConsoleItem(String m) {
            time = logDateFormat.format(new Date());
            msg = m;
        }
    }
    public boolean new_speed = false;
    public float speed = 0;
    public boolean new_accepted = false;
    public long accepted = 0;
    public boolean new_rejected = false;
    public long rejected = 0;
    public boolean new_status = false;
    String status = "None";
    ArrayList<ConsoleItem> console = new ArrayList<ConsoleItem>();
    
    public synchronized void reSet() {
        new_speed = false;
        speed = 0;
        new_accepted = false;
        accepted = 0;
        new_rejected = false;
        rejected = 0;
        new_status = false;
        status = "None";
    }
    
    public synchronized boolean hasNew() {
        return new_speed||new_accepted||new_rejected||new_status||(!console.isEmpty());
    }
}

