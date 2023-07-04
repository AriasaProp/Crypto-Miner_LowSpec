package com.ariasaroject.cmls;

import com.ariasaproject.cmls.connection.StratumMiningConnection;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.HexArray;
import com.ariasaproject.cmls.hasher.Hasher;

import org.junit.Test;

import static org.junit.Assert.*;

public class MiningUnitTest {
    private static final String DEFAULT_URL = "stratum+tcp://us2.litecoinpool.org:3333";
    private static final String DEFAULT_USER = "Ariasa.test";
    private static final String DEFAULT_PASS = "123";
    @Test
    public void miningProsesTest() throws Exception {
        System.out.println("Start Test");
        
        System.out.println("Start Connection Test");
        StratumMiningConnection smc=new StratumMiningConnection(DEFAULT_URL,DEFAULT_USER,DEFAULT_PASS);
        MiningWork mw = smc.connect();
        System.out.println("Connection Test Success");
        
        System.out.println("Hashing Test Start");
        HexArray refTarget = mw.target;
        HexArray refHeader = mw.header;
        System.out.println("Hashing Target to below: \n"+refTarget.getStr());
        System.out.println("Hashing Header to process: \n"+refHeader.getStr());
        Hasher h = new Hasher();
        byte[] res, header = refHeader.refHex(), target = refTarget.refHex();
        boolean findNonce = true;
        for (int nonce = 0; (nonce > -1) && findNonce; nonce++) {
            res = h.hash(header.refHex(), nonce);
            for (int i = res.length - 1; i >= 0; i--) {
                if ((hash[i] & 0xff) > (target[i] & 0xff)) {
                    break;
                }
                if ((hash[i] & 0xff) < (target[i] & 0xff)) {
                    System.out.println("Nonce Found: " + nonce);
                    findNonce = false;
                    break;
                }
            }
        }
        if(findNonce)
            throw RuntimeException("Hashing was wrong, didn't find any nonce!");
        else
            System.out.println("Hashing Test Success");
        System.out.println("Test Success");
        
    }
}
