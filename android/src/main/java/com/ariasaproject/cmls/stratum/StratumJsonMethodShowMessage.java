package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodShowMessage extends StratumJsonMethod {
    public final String val;

    public StratumJsonMethodShowMessage(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        String s = i_json_node.get("method").asText();
        if (s.compareTo("client.show_message") != 0) {
            throw new RuntimeException("json not valid");
        }
        this.val = i_json_node.get("params").asText();
    }
}
