package com.ariasaproject.cmls.stratum;

import com.ariasaproject.cmls.HexArray;
import com.ariasaproject.cmls.MiningWork;
import com.ariasaproject.cmls.StratumMiningWork;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class StratumWorkBuilder {
    private StratumJsonResultSubscribe _subscribe=null;
    private StratumJsonMethodMiningNotify _notify=null;
    private HexArray _xnonce2;
    private HexArray _coinbase;
    private HexArray _merkle_loot;
    private double _difficulty=Double.NEGATIVE_INFINITY;
    private byte[] sha256d(byte[] i_s) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(md.digest(i_s));
    }
    public StratumWorkBuilder(StratumJsonResultSubscribe i_attached_subscribe)
    {
        this._subscribe=i_attached_subscribe;
    }
    public void setDiff(StratumJsonMethodSetDifficulty i_difficulty) {
        this._difficulty=i_difficulty.difficulty;
    }
    public void setNotify(StratumJsonMethodMiningNotify i_attached_notify) throws RuntimeException
    {
        this._notify=i_attached_notify;
        try{
            this._xnonce2=this._notify.getXnonce2(this._subscribe);
            this._coinbase=this._notify.getCoinbase(this._subscribe);
            byte[] merkle_loot=new byte[64];
            System.arraycopy(sha256d(this._coinbase.refHex()),0,merkle_loot,0,32);
            for(int i=0;i<this._notify.merkle_arr.length;i++){
                System.arraycopy(this._notify.merkle_arr[i].refHex(),0,merkle_loot,32,32);
                System.arraycopy(sha256d(merkle_loot),0,merkle_loot,0,32);
            }
            this._merkle_loot=new HexArray(merkle_loot);
            this._merkle_loot.swapEndian();
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }
    public HexArray refXnonce2()
    {
        return this._xnonce2;
    }
    public MiningWork buildMiningWork() {
        if(this._notify==null ||this._subscribe==null || this._difficulty<0){
            return null;
        }
        //Increment extranonce2
        HexArray xnonce2=this._xnonce2;
        String xnonce2_str=xnonce2.getStr();
        for (int i = 0; i < xnonce2.getLength() && (0==(++xnonce2.refHex()[i])); i++);

        //Assemble block header
        HexArray work_data=new HexArray(this._notify.version);
        work_data.append(this._notify.prev_hash);
        work_data.append(this._merkle_loot,0,32);
        work_data.append(this._notify.ntime);
        work_data.append(this._notify.nbit);
        work_data.append(new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,(byte)0x80});
        work_data.append(new byte[40]);
        work_data.append(new byte[]{(byte)0x80,0x02,0x00,0x00});
        return new StratumMiningWork(work_data,diff2target(this._difficulty/65536.0),this._notify.job_id,xnonce2_str);

    }
    private static HexArray diff2target(double diff)
    {
        long m;
        int k;
        byte[] target=new byte[8*4];
        for (k = 6; k > 0 && diff > 1.0; k--){
            diff /= 4294967296.0;
        }
        m = (long)(4294901760.0 / diff);
        if (m == 0 && k == 6){
            Arrays.fill(target,(byte)0xff);
        }else{
            Arrays.fill(target,(byte)0);
            for(int i=0;i<8;i++){
                target[k*4+i]=(byte)((m>>(i*8))&0xff);
            }
        }
        return new HexArray(target);
    }
}