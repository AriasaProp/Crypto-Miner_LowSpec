package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodSetDifficulty extends StratumJsonMethod {
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
