package com.ariasaproject.cmls.stratum;

import com.ariasaproject.cmls.HexArray;
import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonResultSubscribe extends StratumJsonResult {
    public final String session_id;
    public final HexArray xnonce1;
    public final int xnonce2_size;

    public StratumJsonResultSubscribe(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        if (this.error != null) {
            throw new RuntimeException(this.error.asText());
        }
        JsonNode n = i_json_node.get("result");
        if (!n.isArray()) {
            throw new RuntimeException("json not valid");
        }
        // sessionID
        if ((n.get(0).get(0).get(0) != null)) {
            if (n.get(0).get(0).get(0).asText().compareTo("mining.notify") != 0) {
                throw new RuntimeException("json not valid");
            }
        } else {
            if (n.get(0).get(0).asText().compareTo("mining.notify") != 0) {
                throw new RuntimeException("json not valid");
            }
        }

        this.session_id = n.get(0).get(1).asText();
        // xnonce1
        this.xnonce1 = new HexArray(n.get(1).asText());
        // xnonce2_size
        this.xnonce2_size = n.get(2).asInt();
    }
}
