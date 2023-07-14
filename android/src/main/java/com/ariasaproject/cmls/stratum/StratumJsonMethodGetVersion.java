package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;

public class StratumJsonMethodGetVersion extends StratumJsonMethod {
  // {"method":"client.reconnect",params:["host",1]}
  public static final String TEST_PATT =
      "{\"params\": [], \"jsonrpc\": \"2.0\", \"method\": \"client.get_version\", \"id\":"
          + " null}";

  // public parameterima
  public StratumJsonMethodGetVersion(JsonNode i_json_node) throws RuntimeException {
    super(i_json_node);
    String s = i_json_node.get("method").asText();
    if (s.compareTo("client.get_version") != 0) {
      throw new RuntimeException();
    }
    return;
  }
}
