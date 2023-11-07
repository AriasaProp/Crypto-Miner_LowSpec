package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodGetVersion extends StratumJsonMethod {

    public StratumJsonMethodGetVersion(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        String s = i_json_node.get("method").asText();
        if (s.compareTo("client.get_version") != 0) {
            throw new RuntimeException("json method not equal for client.get_version");
        }
        return;
    }
}
