package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonResultStandard extends StratumJsonResult {
    public final boolean result;

    public StratumJsonResultStandard(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        this.result = i_json_node.get("result").asBoolean();
    }
}
