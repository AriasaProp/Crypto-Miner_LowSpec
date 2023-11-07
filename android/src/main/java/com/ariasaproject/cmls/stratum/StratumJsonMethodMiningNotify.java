package com.ariasaproject.cmls.stratum;

import com.ariasaproject.cmls.HexArray;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;

public class StratumJsonMethodMiningNotify extends StratumJsonMethod {
    public final String job_id;
    public final HexArray version;
    public final HexArray[] merkle_arr;
    public final HexArray ntime;
    public final HexArray nbit;
    public final boolean clean;
    public final HexArray prev_hash;
    public final HexArray coinb1;
    public final HexArray coinb2;

    public StratumJsonMethodMiningNotify(JsonNode i_json_node) throws RuntimeException {
        super(i_json_node);
        {
            String s = i_json_node.get("method").asText();
            if (s.compareTo("mining.notify") != 0) {
                throw new RuntimeException("json type isn't valid");
            }
        }
        JsonNode params = i_json_node.get("params");
        // job_id
        {
            this.job_id = params.get(0).asText();
        }
        {
            this.prev_hash = toHexArray(params.get(1).asText(), 64);
        }
        {
            this.coinb1 = new HexArray(params.get(2).asText());
        }
        {
            this.coinb2 = new HexArray(params.get(3).asText());
        }
        // merkle_arr
        {
            JsonNode merkle_arr = params.get(4);
            if (!merkle_arr.isArray()) {
                throw new RuntimeException("json not valid");
            }
            ArrayList<HexArray> l = new ArrayList<HexArray>();
            for (Iterator<JsonNode> i = merkle_arr.iterator(); i.hasNext(); ) {
                l.add(toHexArray(i.next().asText(), 64));
            }
            this.merkle_arr = l.toArray(new HexArray[l.size()]);
        }
        // version
        {
            this.version = toHexArray(params.get(5).asText(), 8);
        }
        // nbit
        {
            this.nbit = toHexArray(params.get(6).asText(), 8);
        }
        // ntime
        {
            this.ntime = toHexArray(params.get(7).asText(), 8);
        }
        // clean
        {
            this.clean = params.get(8).asBoolean();
        }
    }

    public HexArray getXnonce2(StratumJsonResultSubscribe i_subscribe) {
        // xnonce2
        HexArray xnonce2 = new HexArray(new byte[i_subscribe.xnonce2_size]);
        return xnonce2;
    }

    public HexArray getCoinbase(StratumJsonResultSubscribe i_subscribe) {
        // coinbase
        HexArray coinbase = new HexArray(this.coinb1);
        coinbase.append(i_subscribe.xnonce1);
        coinbase.append(this.getXnonce2(i_subscribe));
        coinbase.append(this.coinb2);
        return coinbase;
        //		coinb1_size = strlen(coinb1) / 2;
        //		coinb2_size = strlen(coinb2) / 2;
        //		sctx->job.coinbase_size = coinb1_size + sctx->xnonce1_size +sctx->xnonce2_size +
        // coinb2_size;
        //		sctx->job.coinbase = realloc(sctx->job.coinbase, sctx->job.coinbase_size);
        //		hex2bin(sctx->job.coinbase, coinb1, coinb1_size);
        //		memcpy(sctx->job.coinbase + coinb1_size, sctx->xnonce1, sctx->xnonce1_size);

        //		sctx->job.xnonce2 = sctx->job.coinbase + coinb1_size + sctx->xnonce1_size;
        //		if (!sctx->job.job_id || strcmp(sctx->job.job_id, job_id))
        //		memset(sctx->job.xnonce2, 0, sctx->xnonce2_size);
        //		hex2bin(sctx->job.xnonce2 + sctx->xnonce2_size, coinb2, coinb2_size);
    }
}
