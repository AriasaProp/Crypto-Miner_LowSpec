package com.ariasaroject.cmls;

import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.HexArray;
import com.ariasaproject.cmls.hasher.Hasher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.*;

public class MiningUnitTest {
    /*
    private static final String DEFAULT_URL = "stratum+tcp://us2.litecoinpool.org:3333";
    private static final String DEFAULT_USER = "Ariasa.test";
    private static final String DEFAULT_PASS = "123";
    */
    @Test
    public void HashingTest() throws Exception {
        /*
        System.out.println("Start Test");
        
        System.out.println("Start Connection Test");
        StratumMiningConnection smc = new StratumMiningConnection(DEFAULT_URL,DEFAULT_USER,DEFAULT_PASS);
        MiningWork mw = smc.connect();
        try {
            
        } catch (Exception e) {
            System.out.println("Connection Test Success");
            
        }
        System.out.println("Connection Test Success");
        
        System.out.println("Hashing Test Start");
        HexArray refTarget = mw.target;
        HexArray refHeader = mw.header;
        System.out.println("Hashing Target to below: \n"+refTarget.getStr());
        System.out.println("Hashing Header to process: \n"+refHeader.getStr());
        */
        HexArray refTarget = new HexArray("01000000e4b883e5bda9e79baee4b98ee88081e4b8aae5a5bde4b889e5ada6e5ad90e69c80e6b19fe6b8afe4b88de5aeb9e4b8a8e8aebee99da2e6898be5aeb9e4b8a8e8aebe");
        HexArray refHeader = new HexArray("00000000FFFF0000000000000000000000000000000000000000000000000000");
        final byte[] header = refHeader.refHex(), target = refTarget.refHex();
        AtomicBoolean findNonce = new AtomicBoolean(true);
        ExecutorService es = Executors.newFixedThreadExecutor(5);
        List<Callable<Object>> calls = new ArrayList<Callable<Object>>(5);
        for (int a = 0; a < 5; a++) {
            final int b = a;
            calls.add(Executors.callable(() -> {
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
            }));
        }
        es.invokeAll(calls);
        assertFalse(findNonce.get());
    }
}
