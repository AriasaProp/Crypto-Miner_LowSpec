package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodReconnect extends StratumJsonMethod {
    public int port;
    public String host;

    public StratumJsonMethodReconnect(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        String s = i_json_node.get("method").asText();
        if (s.compareTo("client.reconnect") != 0) {
            throw new RuntimeException("json not valid");
        }
        JsonNode p = i_json_node.get("params");
        this.host = p.get(0).asText();
        this.port = p.get(1).asInt();
        return;
    }
}
