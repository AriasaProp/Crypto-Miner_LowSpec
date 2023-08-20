package com.ariasaproject.cmls;

public class Constants {
    // preferences name
    public static final String PREF_URL = "URL";
    public static final String PREF_PORT = "PORT";
    public static final String PREF_USER = "USER";
    public static final String PREF_PASS = "PASS";
    public static final String PREF_CPU_USAGE = "CPU_USAGE";
    public static final String PREF_CONSOLE = "CONSOLE";

    // default value
    public static final String DEFAULT_URL = "stratum+tcp://us2.litecoinpool.org";
    public static final String DEFAULT_USER = "Ariasa.test";
    public static final String DEFAULT_PASS = "1234";

    public static final int DEFAULT_PORT = 3333;

    // Labeled String name
    public static final String STATUS_NOT_MINING = "Not Mining";
    public static final String STATUS_MINING = "Mining";
    public static final String STATUS_ERROR = "Error";
    public static final String STATUS_TERMINATED = "Terminated";
    public static final String STATUS_CONNECTING = "Connecting";

    // Message id for all Handler
    public static final int MSG_STATE = 1;
    public static final int MSG_UPDATE = 2;

    public static final int MSG_STATE_NONE = 0;
    public static final int MSG_STATE_ONSTART = 1;
    public static final int MSG_STATE_RUNNING = 2;
    public static final int MSG_STATE_ONSTOP = 3;

    public static final int MSG_UPDATE_SPEED = 1;
    public static final int MSG_UPDATE_ACCEPTED = 2;
    public static final int MSG_UPDATE_REJECTED = 3;
    public static final int MSG_UPDATE_STATUS = 4;
    public static final int MSG_UPDATE_CONSOLE = 5;

    // MinerService Status Miner ID
    public static final int MAX_STATUS = 4;
    public static final int STATUS_TYPE_SPEED = 0;
    public static final int STATUS_TYPE_ACCEPTED = 1;
    public static final int STATUS_TYPE_REJECTED = 2;
    public static final int STATUS_TYPE_CONSOLE = 3;

    // native hasher
    public static native byte[] hash(byte[] header);

    public static native long initHasher();

    public static native boolean nativeHashing(long o, byte[] header, int nonce, byte[] target);

    public static native void destroyHasher(long o);
}
