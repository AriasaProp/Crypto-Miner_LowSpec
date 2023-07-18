package com.ariasaproject.cmls.hasher;

public class Hasher {
    public byte[] hash(byte[] header) {
        long en = initialize();
        byte[] r = nativeHashing(en, header, header[76] | header[77] << 8 | header[78] << 16 | header[79] << 24);
        deinitialize(en);
        return r;
    }
    public byte[] hash2(byte[] header) {
        long en = initialize();
        byte[] r = nativeHashing(en, header, header[76] | header[77] << 8 | header[78] << 16 | header[79] << 24);
        deinitialize(en);
        return r;
    }

    public native static long initialize();
    public native static byte[] nativeHashing(long o, byte[] header, int nonce);
    public native static void deinitialize(long o);
}
