package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodSetDifficulty extends StratumJsonMethod {
    public static final String TEST_PATT =
            "{\"params\": [533.210506917676], \"jsonrpc\": \"2.0\", \"method\":"
                + " \"mining.set_difficulty\", \"id\": 44016281}";

    // public parameter
    public double difficulty;

    public StratumJsonMethodSetDifficulty(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        String s = i_json_node.get("method").asText();
        if (s.compareTo("mining.set_difficulty") != 0) {
            throw new RuntimeException("json not valid");
        }
        this.difficulty = i_json_node.get("params").get(0).asDouble();
    }
}
