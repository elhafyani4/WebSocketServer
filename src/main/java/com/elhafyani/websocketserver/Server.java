package com.elhafyani.websocketserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

/**
 * Hello world!
 */


public class Server {
    private static final int port = 9999;
    public int numberThread = 0;
    private ServerSocket masterSocket;

    public Server() {

        try {
            masterSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Error Creating Socket");
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }

    public void run() {
        ClientProcessor clientProcessor = new ClientProcessor();
        Thread clientProcessorThread = new Thread(clientProcessor);
        clientProcessorThread.start();

        while (true) {
            try {
                Socket socket = masterSocket.accept();
                Client client = new Client(socket);
                clientProcessor.clients.put(UUID.randomUUID().toString(), client);
            } catch (IOException ex) {

            }
        }
    }
}
