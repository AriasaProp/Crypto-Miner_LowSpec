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
          new HexArray(hash).getStr()
        );
    }
    public static void main(String[] args)
    {
        try {
            String SR="{\"error\": null, \"id\": 1, \"result\": [[\"mining.notify\", \"ae6812eb4cd7735a302a8a9dd95cf71f\"], \"f801d02f\", 4]}";
            String NT="{\"params\": [\"8bf\", \"8e50f956acdabb3f8e981a4797466043021388791bfa70b1c1a1ba54a8fbdf50\", \"01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff26022d53062f503253482f042e4cb55208\", \"0d2f7374726174756d506f6f6c2f0000000001310cb3fca45500001976a91446a9148895dfa88b9e1596c14afda26b9071861488ac00000000\", [\"e5af4fcc527ce0aecc36848b550032e10f1359a16a3d6b07d76d0ccd361a66e3\", \"9bb81dcf2f43dafcdb6006353ce628a81e27bafd9ea13142bbce9cdf2fa0319b\", \"d7c70d31366c6068a5481f1a9fd786ee1aaa5032c3abd87ac9e024279cd21432\", \"af8cf8b535246110f17fc823c380d1c129d72d20db91cc7be4fb99d5327234e7\", \"5ed74269c86186c14bed42d020fceb631c2db2ad46de19d593f85888503a4c3d\", \"9243decb3a9a34360650f9132cdfca33bbc26677a3c079e978d7c455303861ee\", \"6c7c245323e51b1cda9e7f9de1917c36f94a09d06726352b952c7bf18e5f0dd1\", \"cad9bdc3937c263d78879ccb935cde980f43469260dd0a23d0f0ad9d16fe1b89\", \"b13743cd86b181d1cf8de23404951e4b0c39a273e2061f1a418c36d51dfd3be9\"], \"00000001\", \"1c00adb7\", \"52b54c29\", true], \"id\": null, \"method\": \"mining.notify\"}";
            String ST="{\"params\": [128], \"id\": null, \"method\": \"mining.set_difficulty\"}";
            String WORK_DATA="000000018e50f956acdabb3f8e981a4797466043021388791bfa70b1c1a1ba54a8fbdf5093b73998a3b9d1ad9ee12578b6ffb49088bb9321fcb159e15f10b397cb514e4952b54c291c00adb700000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000080020000";
            String WORK_TARGET="000000000000000000000000000000000000000000000000000000feff010000";
            ObjectMapper mapper = new ObjectMapper();
            StratumJsonResultSubscribe s=new StratumJsonResultSubscribe(mapper.readTree(SR));
            StratumJsonMethodMiningNotify n=new StratumJsonMethodMiningNotify(mapper.readTree(NT));
            StratumJsonMethodSetDifficulty d=new StratumJsonMethodSetDifficulty(mapper.readTree(ST));
            StratumWorkBuilder j=new StratumWorkBuilder(s);
            j.setNotify(n);
            j.setDiff(d);
            MiningWork w=j.buildMiningWork();
            System.out.println(WORK_DATA);
            System.out.println(WORK_TARGET);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /*
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
                        meetsTarget(nonce, h, header, target);
                    }
                } catch (GeneralSecurityException e) {}
            }));
        }
        es.invokeAll(calls);
        assertFalse(findNonce.get());
    }
      
  	boolean meetsTarget(int nonce, Hasher hasher, byte[] header, byte[] target) throws GeneralSecurityException {
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
    */
}