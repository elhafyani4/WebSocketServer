package com.elhafyani.websocketserver;

import java.net.Socket;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class Client {

    public Socket socket;

    public boolean isConnected;

    public Client(Socket socket) {
        this.socket = socket;
    }
}