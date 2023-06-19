package com.ariasaproject.cmls;

import java.lang.Object;

public class MiningStatusService extends Object{
    public boolean new_speed = false;
    public float speed = 0;
    public boolean new_accepted = false;
    public long accepted = 0;
    public boolean new_rejected = false;
    public long rejected = 0;
    
    public boolean new_status = false;
    String status = "None";
    public boolean new_console = false;
    String console = "";
    
    public synchronized void reSet() {
        new_speed = false;
        speed = 0;
        new_accepted = false
        accepted = 0;
        new_rejected = false;
        rejected = 0;
        new_status = false;
        status = "None"
        new_console = false;
    }
    
    public synchronized boolean hasNew() {
        return new_speed||new_accepted||new_rejected||new_status||new_console;
    }
}

