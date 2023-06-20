package com.ariasaproject.cmls;

import java.lang.Object;
import java.util.ArrayList;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import android.os.Parcelable;
import android.os.Parcel;

public class MiningStatusService extends Object{
    private static final DateFormat logDateFormat = new SimpleDateFormat("[HH:mm:ss] ");
    public static class ConsoleItem extends Object implements Parcelable {
        public final String time, msg;
        public ConsoleItem(String m) {
            time = logDateFormat.format(new Date());
            msg = m;
        }
        protected ConsoleItem(Parcel in) {
            String[] strings = new String[2];
            in.readStringArray(strings);
            time = strings[0];
            msg = strings[1];
        }
        public static final Parcelable.Creator<ConsoleItem> CREATOR = new Parcelable.Creator<ConsoleItem>() {
            @Override
            public ConsoleItem createFromParcel(Parcel in) {
                return new ConsoleItem(in);
            }
            @Override
            public ConsoleItem[] newArray(int size) {
                return new ConsoleItem[size];
            }
        };
        @Override
        public int describeContents() {
            return 0;
        }
        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringArray(new String[] { text1, text2 });
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

