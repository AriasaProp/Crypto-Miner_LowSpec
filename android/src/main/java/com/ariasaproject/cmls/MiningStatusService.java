package com.ariasaproject.cmls;

import android.os.Binder;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MiningStatusService extends Binder {
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

        public static final Parcelable.Creator<ConsoleItem> CREATOR =
                new Parcelable.Creator<ConsoleItem>() {
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
            dest.writeStringArray(new String[] {time, msg});
        }
    }

    public boolean new_speed = false;
    public float speed = 0;
    public boolean new_accepted = false;
    public long accepted = 0;
    public boolean new_rejected = false;
    public long rejected = 0;
    public ArrayList<ConsoleItem> console = new ArrayList<ConsoleItem>();
}
