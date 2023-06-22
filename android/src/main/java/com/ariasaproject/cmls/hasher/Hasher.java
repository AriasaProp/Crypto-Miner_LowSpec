package com.ariasaproject.cmls.hasher;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static java.lang.Integer.rotateLeft;
import static java.lang.System.arraycopy;

public class Hasher {
    private Mac mac;
    private byte[] H = new byte[32];
    private byte[] B = new byte[132]; //128 + 4
    private int[] X = new int[32];
    private int[] V = new int[32768]; //32 * 1024
    int i, j, k;

    public Hasher() throws GeneralSecurityException {
        mac = Mac.getInstance("HmacSHA256");
    }
    public byte[] hash(byte[] header, int nonce) throws GeneralSecurityException {
        arraycopy(header, 0, B, 0, 76);
        B[76] = (byte) (nonce >> 24);
        B[77] = (byte) (nonce >> 16);
        B[78] = (byte) (nonce >>  8);
        B[79] = (byte) (nonce >>  0);
        mac.init(new SecretKeySpec(B, 0, 80, "HmacSHA256"));
        B[80] = 0;
        B[81] = 0;
        B[82] = 0;
        for (i = 0; i < 4; i++) {
            B[83] = (byte) (i + 1);
            mac.update(B, 0, 84);
            mac.doFinal(H, 0);

            for (j = 0; j < 8; j++) {
                X[i * 8 + j]  = (H[j * 4 + 0] & 0xff) << 0
                        | (H[j * 4 + 1] & 0xff) << 8
                        | (H[j * 4 + 2] & 0xff) << 16
                        | (H[j * 4 + 3] & 0xff) << 24;
            }
        }

        for (i = 0; i < 1024; i++) {
            arraycopy(X, 0, V, i * 32, 32);
            xorSalsa82();
        }
        for (i = 0; i < 1024; i++) {
            k = (X[16] & 1023) * 32;
            for (j = 0; j < 32; j++)
                X[j] ^= V[k + j];
            xorSalsa82();
        }

        for (i = 0; i < 32; i++) {
            B[i * 4 + 0] = (byte) (X[i] >>  0);
            B[i * 4 + 1] = (byte) (X[i] >>  8);
            B[i * 4 + 2] = (byte) (X[i] >> 16);
            B[i * 4 + 3] = (byte) (X[i] >> 24);
        }
        // 128 + 4 - 1
        B[131] = 1;
        mac.update(B, 0, 132);//128+4
        mac.doFinal(H, 0);
        return H;
    }
    int[] xs = new int[16];
    private void xorSalsa82() {
        int di = 0, xi = 16;
        {
            xs[0] = (X[di +  0] ^= X[xi +  0]);
            xs[1] = (X[di +  1] ^= X[xi +  1]);
            xs[2] = (X[di +  2] ^= X[xi +  2]);
            xs[3] = (X[di +  3] ^= X[xi +  3]);
            xs[4] = (X[di +  4] ^= X[xi +  4]);
            xs[5] = (X[di +  5] ^= X[xi +  5]);
            xs[6] = (X[di +  6] ^= X[xi +  6]);
            xs[7] = (X[di +  7] ^= X[xi +  7]);
            xs[8] = (X[di +  8] ^= X[xi +  8]);
            xs[9] = (X[di +  9] ^= X[xi +  9]);
            xs[10] = (X[di + 10] ^= X[xi + 10]);
            xs[11] = (X[di + 11] ^= X[xi + 11]);
            xs[12] = (X[di + 12] ^= X[xi + 12]);
            xs[13] = (X[di + 13] ^= X[xi + 13]);
            xs[14] = (X[di + 14] ^= X[xi + 14]);
            xs[15] = (X[di + 15] ^= X[xi + 15]);
            for (int i = 0; i < 8; i += 2) {
                xs[4] ^= rotateLeft(xs[0]+xs[12], 7);  xs[8] ^= rotateLeft(xs[4]+xs[0], 9);
                xs[12] ^= rotateLeft(xs[8]+xs[4],13);  xs[0] ^= rotateLeft(xs[12]+xs[8],18);
                xs[9] ^= rotateLeft(xs[5]+xs[1], 7);  xs[13] ^= rotateLeft(xs[9]+xs[5], 9);
                xs[1] ^= rotateLeft(xs[13]+xs[9],13);  xs[5] ^= rotateLeft(xs[1]+xs[13],18);
                xs[14] ^= rotateLeft(xs[10]+xs[6], 7);  xs[2] ^= rotateLeft(xs[14]+xs[10], 9);
                xs[6] ^= rotateLeft(xs[2]+xs[14],13);  xs[10] ^= rotateLeft(xs[6]+xs[2],18);
                xs[3] ^= rotateLeft(xs[15]+xs[11], 7);  xs[7] ^= rotateLeft(xs[3]+xs[15], 9);
                xs[11] ^= rotateLeft(xs[7]+xs[3],13);  xs[15] ^= rotateLeft(xs[11]+xs[7],18);
                xs[1] ^= rotateLeft(xs[0]+xs[3], 7);  xs[2] ^= rotateLeft(xs[1]+xs[0], 9);
                xs[3] ^= rotateLeft(xs[2]+xs[1],13);  xs[0] ^= rotateLeft(xs[3]+xs[2],18);
                xs[6] ^= rotateLeft(xs[5]+xs[4], 7);  xs[7] ^= rotateLeft(xs[6]+xs[5], 9);
                xs[4] ^= rotateLeft(xs[7]+xs[6],13);  xs[5] ^= rotateLeft(xs[4]+xs[7],18);
                xs[11] ^= rotateLeft(xs[10]+xs[9], 7);  xs[8] ^= rotateLeft(xs[11]+xs[10], 9);
                xs[9] ^= rotateLeft(xs[8]+xs[11],13);  xs[10] ^= rotateLeft(xs[9]+xs[8],18);
                xs[12] ^= rotateLeft(xs[15]+xs[14], 7);  xs[13] ^= rotateLeft(xs[12]+xs[15], 9);
                xs[14] ^= rotateLeft(xs[13]+xs[12],13);  xs[15] ^= rotateLeft(xs[14]+xs[13],18);
            }
            X[di +  0] += xs[0];
            X[di +  1] += xs[1];
            X[di +  2] += xs[2];
            X[di +  3] += xs[3];
            X[di +  4] += xs[4];
            X[di +  5] += xs[5];
            X[di +  6] += xs[6];
            X[di +  7] += xs[7];
            X[di +  8] += xs[8];
            X[di +  9] += xs[9];
            X[di + 10] += xs[10];
            X[di + 11] += xs[11];
            X[di + 12] += xs[12];
            X[di + 13] += xs[13];
            X[di + 14] += xs[14];
            X[di + 15] += xs[15];
        }
        di = 16, xi = 0;
        {
            xs[0] = (X[di +  0] ^= X[xi +  0]);
            xs[1] = (X[di +  1] ^= X[xi +  1]);
            xs[2] = (X[di +  2] ^= X[xi +  2]);
            xs[3] = (X[di +  3] ^= X[xi +  3]);
            xs[4] = (X[di +  4] ^= X[xi +  4]);
            xs[5] = (X[di +  5] ^= X[xi +  5]);
            xs[6] = (X[di +  6] ^= X[xi +  6]);
            xs[7] = (X[di +  7] ^= X[xi +  7]);
            xs[8] = (X[di +  8] ^= X[xi +  8]);
            xs[9] = (X[di +  9] ^= X[xi +  9]);
            xs[10] = (X[di + 10] ^= X[xi + 10]);
            xs[11] = (X[di + 11] ^= X[xi + 11]);
            xs[12] = (X[di + 12] ^= X[xi + 12]);
            xs[13] = (X[di + 13] ^= X[xi + 13]);
            xs[14] = (X[di + 14] ^= X[xi + 14]);
            xs[15] = (X[di + 15] ^= X[xi + 15]);
            for (int i = 0; i < 8; i += 2) {
                xs[4] ^= rotateLeft(xs[0]+xs[12], 7);  xs[8] ^= rotateLeft(xs[4]+xs[0], 9);
                xs[12] ^= rotateLeft(xs[8]+xs[4],13);  xs[0] ^= rotateLeft(xs[12]+xs[8],18);
                xs[9] ^= rotateLeft(xs[5]+xs[1], 7);  xs[13] ^= rotateLeft(xs[9]+xs[5], 9);
                xs[1] ^= rotateLeft(xs[13]+xs[9],13);  xs[5] ^= rotateLeft(xs[1]+xs[13],18);
                xs[14] ^= rotateLeft(xs[10]+xs[6], 7);  xs[2] ^= rotateLeft(xs[14]+xs[10], 9);
                xs[6] ^= rotateLeft(xs[2]+xs[14],13);  xs[10] ^= rotateLeft(xs[6]+xs[2],18);
                xs[3] ^= rotateLeft(xs[15]+xs[11], 7);  xs[7] ^= rotateLeft(xs[3]+xs[15], 9);
                xs[11] ^= rotateLeft(xs[7]+xs[3],13);  xs[15] ^= rotateLeft(xs[11]+xs[7],18);
                xs[1] ^= rotateLeft(xs[0]+xs[3], 7);  xs[2] ^= rotateLeft(xs[1]+xs[0], 9);
                xs[3] ^= rotateLeft(xs[2]+xs[1],13);  xs[0] ^= rotateLeft(xs[3]+xs[2],18);
                xs[6] ^= rotateLeft(xs[5]+xs[4], 7);  xs[7] ^= rotateLeft(xs[6]+xs[5], 9);
                xs[4] ^= rotateLeft(xs[7]+xs[6],13);  xs[5] ^= rotateLeft(xs[4]+xs[7],18);
                xs[11] ^= rotateLeft(xs[10]+xs[9], 7);  xs[8] ^= rotateLeft(xs[11]+xs[10], 9);
                xs[9] ^= rotateLeft(xs[8]+xs[11],13);  xs[10] ^= rotateLeft(xs[9]+xs[8],18);
                xs[12] ^= rotateLeft(xs[15]+xs[14], 7);  xs[13] ^= rotateLeft(xs[12]+xs[15], 9);
                xs[14] ^= rotateLeft(xs[13]+xs[12],13);  xs[15] ^= rotateLeft(xs[14]+xs[13],18);
            }
            X[di +  0] += xs[0];
            X[di +  1] += xs[1];
            X[di +  2] += xs[2];
            X[di +  3] += xs[3];
            X[di +  4] += xs[4];
            X[di +  5] += xs[5];
            X[di +  6] += xs[6];
            X[di +  7] += xs[7];
            X[di +  8] += xs[8];
            X[di +  9] += xs[9];
            X[di + 10] += xs[10];
            X[di + 11] += xs[11];
            X[di + 12] += xs[12];
            X[di + 13] += xs[13];
            X[di + 14] += xs[14];
            X[di + 15] += xs[15];
        }
    }
}