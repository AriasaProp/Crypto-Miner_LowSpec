package com.ariasaroject.cmls;

import com.ariasaroject.cmls.HexArray;
import com.ariasaroject.cmls.hasher.Hasher;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }
    /*
    @Test
    public void hashingTest() throws Exception {
        String input_hex = "02000000b73417169f9055d2a9e143faa078e0cf97a13c40a5fc117a6b85a2240e5a7d1adc39618a259aa142f2c01e3640f19a68848a7494e75f22a4f068e24ae5a4d8b1b1e518530eb6101b003a269b";
        HexArray hin = new HexArray(input_hex);
        Hasher h = new Hasher();
        for (int nonce = 0; nonce < 65535; nonce++) {
            byte[] res = h.hash(hin.refHex(), nonce);
            for (int i2 = res.length - 1; i2 >= 0; i2--) {
                if ((hash[i2] & 0xff) > (target[i2] & 0xff)) {
                    break;
                }
                if ((hash[i2] & 0xff) < (target[i2] & 0xff)) {
                    this._parent.invokeNonceFound(work,nonce);
                    break;
                }
            }
        }
        output_hex = "000000000003f406a8163444623935c2c4320a7d5508773b3c2de8fee4b14068"
    }
    */
}