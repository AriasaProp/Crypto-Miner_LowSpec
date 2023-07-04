package com.ariasaroject.cmls;

import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.HexArray;
import com.ariasaproject.cmls.hasher.Hasher;

import java.security.GeneralSecurityException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

public class MiningUnitTest {
    @Test
    public void HasherTest() throws Exception {
        HexArray refHeader = new HexArray("01000000f615f7ce3b4fc6b8f61e8f89aedb1d0852507650533a9e3b10b9bbcc30639f279fcaa86746e1ef52d3edb3c4ad8259920d509bd073605c9bf1d59983752a6b06b817bb4ea78e011d012d59d4");
        Hasher h = new Hasher();
        byte[] hash = h.hash(refHeader.refHex());
        assertEquals(
          "d9eb8663ffec241c2fb118adb7de97a82c803b6ff46d57667935c81001000000",
          new HexArray(hash).toStr()
        );
    }
    @Test
    public void HashingTest() throws Exception {
        HexArray refHeader = new HexArray("00000000FFFF0000000000000000000000000000000000000000000000000000");
        
        HexArray refTarget = new HexArray("01000000f615f7ce3b4fc6b8f61e8f89aedb1d0852507650533a9e3b10b9bbcc30639f279fcaa86746e1ef52d3edb3c4ad8259920d509bd073605c9bf1d59983752a6b06b817bb4ea78e011d012d59d4");
        final byte[] header = refHeader.refHex(), target = refTarget.refHex();
        AtomicBoolean findNonce = new AtomicBoolean(true);
        ExecutorService es = Executors.newFixedThreadPool(5);
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>(5);
        for (int a = 0; a < 5; a++) {
            final int b = a;
            calls.add(Executors.callable(() -> {
                try {
                    try {
                    Hasher h = new Hasher();
                    for (int nonce = b; (nonce > -1) && findNonce.get(); nonce+=5) {
                        byte[] hash = h.hash(header, nonce);
                        for (int i = hash.length - 1; i >= 0; i--) {
                            if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                                break;
                            }
                            if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                                findNonce.set(false);
                                break;
                            }
                        }
                    }
                } catch (GeneralSecurityException e) {}
            }));
        }
        es.invokeAll(calls);
        assertFalse(findNonce.get());
    }
      
  	boolean meetsTarget(int nonce, Hasher hasher, byte[] header) throws GeneralSecurityException {
      	byte[] hash = hasher.hash(header, nonce);
      	for (int i = hash.length - 1; i >= 0; i--) {
          	if ((hash[i] & 0xff) > (target[i] & 0xff))
          	    return false;
          	if ((hash[i] & 0xff) < (target[i] & 0xff))
          	    return true;
      	}
      	return true;
  	}
}
