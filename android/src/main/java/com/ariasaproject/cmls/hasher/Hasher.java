package com.ariasaproject.cmls.hasher;

import static java.lang.Integer.rotateLeft;
import static java.lang.System.arraycopy;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Hasher {
    private Mac mac;
    private byte[] H = new byte[32];
    private int[] X = new int[32];
    private int[] V = new int[32768]; // 32 * 1024
    int[] xs = new int[16];

    public Hasher() throws GeneralSecurityException {
        mac = Mac.getInstance("HmacSHA256");
    }

    private byte[] B = new byte[132]; // 128 + 4
    private int i, j, k, l;

    public byte[] hash(byte[] header) throws GeneralSecurityException {
        return hash(header, header[76] | header[77] << 8 | header[78] << 16 | header[79] << 24);
    }

    public byte[] hash2(byte[] header) {
        return nativeHashing(
                header, header[76] | header[77] << 8 | header[78] << 16 | header[79] << 24);
    }

    public byte[] hash(byte[] header, int nonce) throws GeneralSecurityException {
        arraycopy(header, 0, B, 0, 76);
        B[76] = (byte) (nonce >> 24);
        B[77] = (byte) (nonce >> 16);
        B[78] = (byte) (nonce >> 8);
        B[79] = (byte) (nonce >> 0);
        mac.init(new SecretKeySpec(B, 0, 80, "HmacSHA256"));
        B[80] = 0;
        B[81] = 0;
        B[82] = 0;
        for (i = 0; i < 4; i++) {
            B[83] = (byte) (i + 1);
            mac.update(B, 0, 84);
            mac.doFinal(H, 0);
            for (j = 0; j < 8; j++) {
                X[i * 8 + j] =
                        (H[j * 4 + 0] & 0xff) << 0
                                | (H[j * 4 + 1] & 0xff) << 8
                                | (H[j * 4 + 2] & 0xff) << 16
                                | (H[j * 4 + 3] & 0xff) << 24;
            }
        }
        for (i = 0; i < 32768; i += 32) { // 1024*32
            arraycopy(X, 0, V, i, 32);
            // xorSalsa8
            xorSalsa82();
        }
        for (i = 0; i < 1024; i++) {
            k = (X[16] & 1023) * 32;
            for (j = 0; j < 32; j++) X[j] ^= V[k + j];
            // xorSalsa8
            xorSalsa82();
        }
        for (i = 0; i < 32; i++) {
            B[i * 4 + 0] = (byte) (X[i] >> 0);
            B[i * 4 + 1] = (byte) (X[i] >> 8);
            B[i * 4 + 2] = (byte) (X[i] >> 16);
            B[i * 4 + 3] = (byte) (X[i] >> 24);
        }

        // 128 + 4 - 1
        B[131] = 1;
        mac.update(B, 0, 132); // 128+4
        mac.doFinal(H, 0);
        return H;
    }

    private final void xorSalsa82() {
        xs[0] = (X[0] ^= X[16]);
        xs[1] = (X[1] ^= X[17]);
        xs[2] = (X[2] ^= X[18]);
        xs[3] = (X[3] ^= X[19]);
        xs[4] = (X[4] ^= X[20]);
        xs[5] = (X[5] ^= X[21]);
        xs[6] = (X[6] ^= X[22]);
        xs[7] = (X[7] ^= X[23]);
        xs[8] = (X[8] ^= X[24]);
        xs[9] = (X[9] ^= X[25]);
        xs[10] = (X[10] ^= X[26]);
        xs[11] = (X[11] ^= X[27]);
        xs[12] = (X[12] ^= X[28]);
        xs[13] = (X[13] ^= X[29]);
        xs[14] = (X[14] ^= X[30]);
        xs[15] = (X[15] ^= X[31]);
        for (l = 0; l < 4; l++) { // 8/2
            xs[4] ^= rotateLeft(xs[0] + xs[12], 7);
            xs[8] ^= rotateLeft(xs[4] + xs[0], 9);
            xs[12] ^= rotateLeft(xs[8] + xs[4], 13);
            xs[0] ^= rotateLeft(xs[12] + xs[8], 18);
            xs[9] ^= rotateLeft(xs[5] + xs[1], 7);
            xs[13] ^= rotateLeft(xs[9] + xs[5], 9);
            xs[1] ^= rotateLeft(xs[13] + xs[9], 13);
            xs[5] ^= rotateLeft(xs[1] + xs[13], 18);
            xs[14] ^= rotateLeft(xs[10] + xs[6], 7);
            xs[2] ^= rotateLeft(xs[14] + xs[10], 9);
            xs[6] ^= rotateLeft(xs[2] + xs[14], 13);
            xs[10] ^= rotateLeft(xs[6] + xs[2], 18);
            xs[3] ^= rotateLeft(xs[15] + xs[11], 7);
            xs[7] ^= rotateLeft(xs[3] + xs[15], 9);
            xs[11] ^= rotateLeft(xs[7] + xs[3], 13);
            xs[15] ^= rotateLeft(xs[11] + xs[7], 18);
            xs[1] ^= rotateLeft(xs[0] + xs[3], 7);
            xs[2] ^= rotateLeft(xs[1] + xs[0], 9);
            xs[3] ^= rotateLeft(xs[2] + xs[1], 13);
            xs[0] ^= rotateLeft(xs[3] + xs[2], 18);
            xs[6] ^= rotateLeft(xs[5] + xs[4], 7);
            xs[7] ^= rotateLeft(xs[6] + xs[5], 9);
            xs[4] ^= rotateLeft(xs[7] + xs[6], 13);
            xs[5] ^= rotateLeft(xs[4] + xs[7], 18);
            xs[11] ^= rotateLeft(xs[10] + xs[9], 7);
            xs[8] ^= rotateLeft(xs[11] + xs[10], 9);
            xs[9] ^= rotateLeft(xs[8] + xs[11], 13);
            xs[10] ^= rotateLeft(xs[9] + xs[8], 18);
            xs[12] ^= rotateLeft(xs[15] + xs[14], 7);
            xs[13] ^= rotateLeft(xs[12] + xs[15], 9);
            xs[14] ^= rotateLeft(xs[13] + xs[12], 13);
            xs[15] ^= rotateLeft(xs[14] + xs[13], 18);
        }
        X[0] += xs[0];
        X[1] += xs[1];
        X[2] += xs[2];
        X[3] += xs[3];
        X[4] += xs[4];
        X[5] += xs[5];
        X[6] += xs[6];
        X[7] += xs[7];
        X[8] += xs[8];
        X[9] += xs[9];
        X[10] += xs[10];
        X[11] += xs[11];
        X[12] += xs[12];
        X[13] += xs[13];
        X[14] += xs[14];
        X[15] += xs[15];

        xs[0] = (X[16] ^= X[0]);
        xs[1] = (X[17] ^= X[1]);
        xs[2] = (X[18] ^= X[2]);
        xs[3] = (X[19] ^= X[3]);
        xs[4] = (X[20] ^= X[4]);
        xs[5] = (X[21] ^= X[5]);
        xs[6] = (X[22] ^= X[6]);
        xs[7] = (X[23] ^= X[7]);
        xs[8] = (X[24] ^= X[8]);
        xs[9] = (X[25] ^= X[9]);
        xs[10] = (X[26] ^= X[10]);
        xs[11] = (X[27] ^= X[11]);
        xs[12] = (X[28] ^= X[12]);
        xs[13] = (X[29] ^= X[13]);
        xs[14] = (X[30] ^= X[14]);
        xs[15] = (X[31] ^= X[15]);
        for (l = 0; l < 4; l++) { // 8/2
            xs[4] ^= rotateLeft(xs[0] + xs[12], 7);
            xs[8] ^= rotateLeft(xs[4] + xs[0], 9);
            xs[12] ^= rotateLeft(xs[8] + xs[4], 13);
            xs[0] ^= rotateLeft(xs[12] + xs[8], 18);
            xs[9] ^= rotateLeft(xs[5] + xs[1], 7);
            xs[13] ^= rotateLeft(xs[9] + xs[5], 9);
            xs[1] ^= rotateLeft(xs[13] + xs[9], 13);
            xs[5] ^= rotateLeft(xs[1] + xs[13], 18);
            xs[14] ^= rotateLeft(xs[10] + xs[6], 7);
            xs[2] ^= rotateLeft(xs[14] + xs[10], 9);
            xs[6] ^= rotateLeft(xs[2] + xs[14], 13);
            xs[10] ^= rotateLeft(xs[6] + xs[2], 18);
            xs[3] ^= rotateLeft(xs[15] + xs[11], 7);
            xs[7] ^= rotateLeft(xs[3] + xs[15], 9);
            xs[11] ^= rotateLeft(xs[7] + xs[3], 13);
            xs[15] ^= rotateLeft(xs[11] + xs[7], 18);
            xs[1] ^= rotateLeft(xs[0] + xs[3], 7);
            xs[2] ^= rotateLeft(xs[1] + xs[0], 9);
            xs[3] ^= rotateLeft(xs[2] + xs[1], 13);
            xs[0] ^= rotateLeft(xs[3] + xs[2], 18);
            xs[6] ^= rotateLeft(xs[5] + xs[4], 7);
            xs[7] ^= rotateLeft(xs[6] + xs[5], 9);
            xs[4] ^= rotateLeft(xs[7] + xs[6], 13);
            xs[5] ^= rotateLeft(xs[4] + xs[7], 18);
            xs[11] ^= rotateLeft(xs[10] + xs[9], 7);
            xs[8] ^= rotateLeft(xs[11] + xs[10], 9);
            xs[9] ^= rotateLeft(xs[8] + xs[11], 13);
            xs[10] ^= rotateLeft(xs[9] + xs[8], 18);
            xs[12] ^= rotateLeft(xs[15] + xs[14], 7);
            xs[13] ^= rotateLeft(xs[12] + xs[15], 9);
            xs[14] ^= rotateLeft(xs[13] + xs[12], 13);
            xs[15] ^= rotateLeft(xs[14] + xs[13], 18);
        }
        X[16] += xs[0];
        X[17] += xs[1];
        X[18] += xs[2];
        X[19] += xs[3];
        X[20] += xs[4];
        X[21] += xs[5];
        X[22] += xs[6];
        X[23] += xs[7];
        X[24] += xs[8];
        X[25] += xs[9];
        X[26] += xs[10];
        X[27] += xs[11];
        X[28] += xs[12];
        X[29] += xs[13];
        X[30] += xs[14];
        X[31] += xs[15];
    }

    public static native byte[] nativeHashing(byte[] header, int nonce);
}
