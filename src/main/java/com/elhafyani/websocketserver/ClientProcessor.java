package com.elhafyani.websocketserver;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class ClientProcessor implements Runnable {

    public ConcurrentMap<String, Client> clients;
    private ThreadPoolExecutor executor;

    public ClientProcessor(){
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        clients = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        while(true) {
            for (Client client : clients.values()) {
                try {
                    if (client.socket.getInputStream().available() > 0 && client.status == Status.READY) {
                        client.status = Status.PROCESSING;
                        SocketRequestHandler handler = new SocketRequestHandler(client);
                        executor.execute(handler);
                    }
                } catch (IOException ex) {

                }
            }
        }
    }
}
