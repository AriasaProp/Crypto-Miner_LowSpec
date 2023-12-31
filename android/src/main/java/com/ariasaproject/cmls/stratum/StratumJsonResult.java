package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonResult extends StratumJson {
    public final JsonNode error;
    public final Long id;

    public StratumJsonResult(JsonNode i_json_node) throws RuntimeException {
        if (i_json_node.has("id")) {
            this.id = i_json_node.get("id").isNull() ? null : i_json_node.get("id").asLong();
        } else {
            this.id = null;
        }
        if (!i_json_node.has("error")) {
            throw new RuntimeException("json not valid");
        }
        if (i_json_node.get("error").isNull()) {
            this.error = null;
        } else {
            this.error = i_json_node.get("error");
        }
    }
}
