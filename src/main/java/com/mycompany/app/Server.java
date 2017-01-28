package com.mycompany.app;

import com.sun.media.jfxmedia.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Hello world!
 *
 */

@PropertySource("classpath:/config.properties}")
@Configuration


public class Server
{
    @Value("${server.port:9999}")
    private int port;

    private ServerSocket masterSocket;

    public int numberThread = 0;

    public Server() {

        try {
            masterSocket = new ServerSocket(9999);
        }catch(IOException ex){
            Logger.logMsg(Logger.ERROR, ex.getStackTrace().toString());
        }
    }

    public void run(){
        ClientProcessor clientProcessor = new ClientProcessor();
        Thread thread = new Thread(clientProcessor);
        thread.start();

        while(true){

            try {
                Socket client = masterSocket.accept();

                clientProcessor.sockets.put(UUID.randomUUID().toString(), client);

                System.out.println(numberThread++);
            }catch(IOException ex){
                Logger.logMsg(Logger.ERROR, ex.getStackTrace().toString());
            }

        }
    }

    public static  void main(String[] args){
        Server server = new Server();
        server.run();
    }
}
