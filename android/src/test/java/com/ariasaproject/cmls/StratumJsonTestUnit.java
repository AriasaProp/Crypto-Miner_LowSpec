package com.ariasaroject.cmls;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.ariasaproject.cmls.stratum.StratumJson;
import com.ariasaproject.cmls.stratum.StratumJsonMethodGetVersion;
import com.ariasaproject.cmls.stratum.StratumJsonMethodMiningNotify;
import com.ariasaproject.cmls.stratum.StratumJsonMethodReconnect;
import com.ariasaproject.cmls.stratum.StratumJsonMethodSetDifficulty;
import com.ariasaproject.cmls.stratum.StratumJsonMethodShowMessage;
import com.ariasaproject.cmls.stratum.StratumJsonResult;
import com.ariasaproject.cmls.stratum.StratumJsonResultStandard;
import com.ariasaproject.cmls.stratum.StratumJsonResultSubscribe;

public class StratumJsonTestUnit {
    @Test
    public void JsonParser() throws Exception {
        // JsonParseTest
        ObjectMapper mapper = new ObjectMapper();
        StratumJson s1 =
                new StratumJsonMethodGetVersion(
                        mapper.readTree(StratumJsonMethodGetVersion.TEST_PATT));
        StratumJson s2 =
                new StratumJsonMethodMiningNotify(
                        mapper.readTree(StratumJsonMethodMiningNotify.TEST_PATT));
        StratumJson s3 =
                new StratumJsonMethodReconnect(
                        mapper.readTree(StratumJsonMethodReconnect.TEST_PATT));
        StratumJson s4 =
                new StratumJsonMethodSetDifficulty(
                        mapper.readTree(StratumJsonMethodSetDifficulty.TEST_PATT));
        StratumJson s5 =
                new StratumJsonMethodShowMessage(
                        mapper.readTree(StratumJsonMethodShowMessage.TEST_PATT));
        StratumJson s6 =
                new StratumJsonResultStandard(
                        mapper.readTree(StratumJsonResultStandard.TEST_PATT));
        StratumJson s7 =
                new StratumJsonResultSubscribe(
                        mapper.readTree(StratumJsonResultSubscribe.TEST_PATT));
    }
}