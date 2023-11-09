package com.ariasaproject.cmls.stratum;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;

public class StratumSocket extends Socket {
    private BufferedWriter _tx;
    private BufferedReader _rx;
    private int _id;

    protected StratumSocket() {}

    public StratumSocket(URI i_url) throws UnknownHostException, IOException {
        super(i_url.getHost(), i_url.getPort());
        this._tx = new BufferedWriter(new OutputStreamWriter(this.getOutputStream()));
        this._rx = new BufferedReader(new InputStreamReader(this.getInputStream()));
        this._id = 1;
    }

    public long subscribe(String i_agent_name) throws IOException {
        long id;
        synchronized (this) {
            id = this._id;
            this._id++;
        }
        this._tx.write("{\"id\": " + id + ", \"method\": \"mining.subscribe\", \"params\": []}\n");
        this._tx.flush();
        return id;
    }

    public long authorize(String i_user, String i_password) throws IOException {
        long id;
        synchronized (this) {
            id = this._id;
            this._id++;
        }
        this._tx.write(
                "{\"id\": "
                        + id
                        + ", \"method\": \"mining.authorize\", \"params\": [\""
                        + i_user
                        + "\",\""
                        + i_password
                        + "\"]}\n");
        this._tx.flush();
        return id;
    }

    public long submit(int i_nonce, String i_user, String i_jobid, String i_nonce2, String i_ntime)
            throws IOException {
        long id;
        synchronized (this) {
            id = this._id;
            this._id++;
        }
        String sn =
                String.format(
                        "%08x",
                        (((i_nonce & 0xff000000) >> 24)
                                | ((i_nonce & 0x00ff0000) >> 8)
                                | ((i_nonce & 0x0000ff00) << 8)
                                | ((i_nonce & 0x000000ff) << 24)));
        // {"method": "mining.submit", "params": ["nyajira.xa", "e4c", "00000000", "52b7a1a9",
        // "79280100"], "id":4}
        String s =
                "{\"id\": "
                        + id
                        + ", \"method\": \"mining.submit\", \"params\": [\""
                        + i_user
                        + "\", \""
                        + i_jobid
                        + "\",\""
                        + i_nonce2
                        + "\",\""
                        + i_ntime
                        + "\",\""
                        + sn
                        + "\"]}\n";
        this._tx.write(s);
        this._tx.flush();
        return id;
    }

    public StratumJson recvStratumJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jn = mapper.readTree(this._rx.readLine());
        // parse method
        try {
            return new StratumJsonMethodGetVersion(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonMethodMiningNotify(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonMethodReconnect(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonMethodSetDifficulty(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonMethodShowMessage(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonResultSubscribe(jn);
        } catch (Exception e) {
        }
        try {
            return new StratumJsonResultStandard(jn);
        } catch (Exception e) {
        }
        return null;
    }
}
