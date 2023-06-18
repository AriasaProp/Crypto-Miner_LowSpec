package com.ariasaproject.cmls.connection;

public interface IMiningConnection
{
    public void addListener(IConnectionEvent i_listener) throws RuntimeException;
    public MiningWork connect() throws RuntimeException;
    public void disconnect() throws RuntimeException;
    public MiningWork getWork() throws RuntimeException;
    public void submitWork(MiningWork i_work, int i_nonce) throws RuntimeException;
}