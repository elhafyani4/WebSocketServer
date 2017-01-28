package com.mycompany.app;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class ClientProcessor implements Runnable {

    private ThreadPoolExecutor executor ;

    public ConcurrentMap<String, Socket> sockets;



    public ClientProcessor(){
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        sockets = new ConcurrentHashMap<>();
    }


    @Override
    public void run() {
        while(true) {

            synchronized (sockets) {
                for (Socket socket : sockets.values()) {
                    try {
                        if (socket.getInputStream().available() > 0) {
                            SocketRequestHandler handler = new SocketRequestHandler(socket);
                            executor.execute(handler);
                        }
                    } catch (IOException ex) {

                    }
                }
            }
        }
    }
}
