package com.ariasaproject.cmls.stratum;

import com.ariasaproject.cmls.HexArray;

public class StratumJson {
    protected StratumJson() {}

    protected StratumJson(StratumJson i_src) {}

    protected static HexArray toHexArray(String i_str, int i_str_len) throws RuntimeException {
        if (i_str.length() != i_str_len) {
            throw new RuntimeException("String length and input length isn't equal");
        }
        return new HexArray(i_str);
    }
}
