package com.ariasaproject.cmls;

public class Constants {
    public static final int MSG_TERMINATED = 1;

    public static final String MSG_STATUS_UPDATE = "status_update";
    public static final String MSG_UIUPDATE = "ui_update";
    public static final String MSG_SPEED_UPDATE = "speed_update";
    public static final String MSG_ACCEPTED_UPDATE = "accepted_update";
    public static final String MSG_REJECTED_UPDATE = "rejected_update";
    public static final String MSG_CONSOLE_UPDATE = "console_update";

    public static final String PREF_URL="URL";
    public static final String PREF_USER= "USER";
    public static final String PREF_PASS= "PASS";
    public static final String PREF_THREAD= "THREAD";
    public static final String PREF_THROTTLE = "THROTTLE";
    public static final String PREF_SCANTIME = "SCANTIME";
    public static final String PREF_RETRYPAUSE = "RETRYPAUSE";
    public static final String PREF_DONATE = "DONATE";
    public static final String PREF_SERVICE = "SERVICE";
    public static final String PREF_TITLE="SETTINGS";
    public static final String PREF_PRIORITY="PRIORITY";
    public static final String PREF_BACKGROUND="BACKGROUND";
    public static final String PREF_SCREEN="SCREEN_AWAKE";
    public static final String PREF_NEWS_RUN_ONCE="NEWS_RUN_ONCE";

    public static final String DEFAULT_URL="stratum+tcp://us2.litecoinpool.org:3333";
    public static final String DEFAULT_USER="Ariasa.test";
    public static final String DEFAULT_PASS="123";

    public static final int 		DEFAULT_PRIORITY=1;
    public static final int 		DEFAULT_THREAD=1;
    public static final long 	DEFAULT_SCANTIME=500;
    public static final long 	DEFAULT_RETRYPAUSE=500;
    public static final float 	DEFAULT_THROTTLE=1;
    public static final boolean 	DEFAULT_BACKGROUND = false;
    public static final boolean 	DEFAULT_SCREEN = true;

    public static final String CLIENT_NAME_STRING="CMLS";

    public static final String STATUS_NOT_MINING = "Not Mining";
    public static final String STATUS_MINING = "Mining";
    public static final String STATUS_ERROR = "Error";
    public static final String STATUS_TERMINATED = "Terminated";
    public static final String STATUS_CONNECTING = "Connecting";


}