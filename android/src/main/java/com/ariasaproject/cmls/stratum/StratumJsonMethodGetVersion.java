package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodGetVersion extends StratumJsonMethod {
    public final String val;
    public StratumJsonMethodGetVersion(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        String s = i_json_node.get("method").asText();
        if (s.compareTo("client.get_version") != 0) {
            throw new RuntimeException();
        }
        val = i_json_node.get("jsonrpc").asText();
    }
}
