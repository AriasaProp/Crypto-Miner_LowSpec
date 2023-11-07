package com.ariasaroject.cmls;

import static org.junit.jupiter.api.Assertions.*;

import com.ariasaproject.cmls.Constants;
import com.ariasaproject.cmls.HexArray;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.connection.*;
import com.ariasaproject.cmls.stratum.StratumJson;
import com.ariasaproject.cmls.stratum.StratumJsonMethodGetVersion;
import com.ariasaproject.cmls.stratum.StratumJsonMethodMiningNotify;
import com.ariasaproject.cmls.stratum.StratumJsonMethodReconnect;
import com.ariasaproject.cmls.stratum.StratumJsonMethodSetDifficulty;
import com.ariasaproject.cmls.stratum.StratumJsonMethodShowMessage;
import com.ariasaproject.cmls.stratum.StratumJsonResult;
import com.ariasaproject.cmls.stratum.StratumJsonResultStandard;
import com.ariasaproject.cmls.stratum.StratumJsonResultSubscribe;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
public class StratumJsonParserUnitTest {
    //other stratum json request
    // {"method":"client.reconnect",params:["host",1]}
    // {"method":"client.reconnect",params:["host",1]}


    ObjectMapper mapper = new ObjectMapper();
    @Test
    public void GetVersion() {
        try {
            final String TEST_PATT = "{\"params\": [], \"jsonrpc\": \"2.0\", \"method\": \"client.get_version\", \"id\": null}";
            StratumJson s = new StratumJsonMethodGetVersion(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void MiningNotify() {
        try {
            final String TEST_PATT = "{\"params\":[\"113341302436276469056253036457324159787\","
                + " \"7bbb491d8da308c42bb84c28dd6c8f77aa0102a676b266bec530c737959ec4b4\","
                + " \"01000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0b03485215062f503253482fffffffff110024f400000000001976a9140833120f05dc473d8d3e4b7f0a7174207e62144588acfd718006000000001976a9147077906a5f88819a036bbb804c2435e239b384ef88aceea18106000000001976a91483eb72afc57909a4b44430e9783b2d18cb24315b88ac83708706000000001976a914a1d2c08641709fb25bbd5f35a3de1eae2fff161f88ac43ef9c06000000001976a9149d5584f55d593958a20e6b8f7353345551f8fd3d88ac05c1c406000000001976a914147a6c5a927d9422a819b58cb6a4803523b1c08888ac063dc706000000001976a9142fe80ea5aaf8dbfc819265dd2e6d8a56b5979b0f88ac401bb20b000000001976a9143bc4089dbb0a83c69365926434ccba8b8001626088acec522b0d000000001976a914ce1f15f063a332c71b9030a7ecffc4f22dfc635888ac86142e0d000000001976a914a68ce3ed72d8b8bd695129c97ff276ea673b179c88acf0ac300d000000001976a91420ac5a56c73a88e6492556d438c27d6844746e0388ac50f2330d000000001976a9148639380456dec002ed1238eabdabfb25cf8e88b988ac20f46b0d000000001976a914df5a67b33cb8c08693c0dcbbfd5a619632f9e66688ac757f451a000000001976a9149bd07713fd6c44bb2a0652f5c21f654420448e1b88acb7f4f32d000000001976a914b553762cbd73a73ed3def6f7fb48eec16a88191788ac0600000000000000434104ffd03de44a6e11b9917f3a29f9443283d9871c9d743ef30d5eddcd37094b64d1b3d8090496b53256786bf5c82932ec23c3b74d9f05a6f95a8b5529352656664bac00000000000000002a6a287cf05a8a20076248b28f7963992718a0dd3765ea76c77c8656dcf6ff985c4eb9000000000100\","
                + " \"00000000\", [], \"00000001\", \"1d00bf80\", \"52ac5e55\", true], \"jsonrpc\":"
                + " \"2.0\", \"method\": \"mining.notify\", \"id\": 764659215}";
            StratumJson s = new StratumJsonMethodMiningNotify(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void MethodReconnect() {
        try {
            final String TEST_PATT = "{\"params\": [\"host\",80], \"jsonrpc\": \"2.0\", \"method\": \"client.reconnect\", \"id\": null}";
            StratumJson s = new StratumJsonMethodReconnect(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void MethodSetDifficulty() {
        try {
            final String TEST_PATT = "{\"params\": [533.210506917676], \"jsonrpc\": \"2.0\", \"method\": \"mining.set_difficulty\", \"id\": 44016281}";
            StratumJson s = new StratumJsonMethodSetDifficulty(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void MethodShowMessage() {
        try {
            final String TEST_PATT = "{\"params\": [\"TEST\"], \"jsonrpc\": \"2.0\", \"method\": \"client.show_message\", \"id\": null}";
            StratumJson s = new StratumJsonMethodShowMessage(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void ResultStandard() {
        try {
            final String TEST_PATT = "{\"error\": null, \"jsonrpc\": \"2.0\", \"id\": 2, \"result\": true}";
            StratumJson s = new StratumJsonResultStandard(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void ResultSubscribe() {
        try {
            final String TEST_PATT = "{\"id\":1,\"result\":[[\"mining.notify\",\"b86c07fd6cc70b367b61669fb5e91bfa\"],\"f8000105\",4],\"error\":null}";
            StratumJson s = new StratumJsonResultSubscribe(mapper.readTree(TEST_PATT));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

