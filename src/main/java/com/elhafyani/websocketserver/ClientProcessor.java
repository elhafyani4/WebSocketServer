package com.elhafyani.websocketserver;

import com.sun.media.jfxmedia.logging.Logger;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class ClientProcessor implements Runnable {

    private ThreadPoolExecutor executor ;

    public ConcurrentMap<String, Client> clients;

    public ClientProcessor(){
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        clients = new ConcurrentHashMap<>();
    }

    @Override
    public void run() {
        while(true) {
            for (Client client : clients.values()) {
                try {
                    if (client.socket.getInputStream().available() > 0) {
                        SocketRequestHandler handler = new SocketRequestHandler(client);
                        executor.execute(handler);
                    }
                } catch (IOException ex) {
                    Logger.logMsg(Logger.ERROR, ex.getStackTrace().toString());
                }
            }
        }
    }



}
