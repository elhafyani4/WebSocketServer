package com.mycompany.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class SocketRequestHandler implements Runnable {

    public DataInputStream is;
    public DataOutputStream os;
    private Socket socket;

    public SocketRequestHandler(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        try{
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());

            int available = is.available();
            byte[] data = new byte[available];
            is.read(data, 0, available);

            System.out.println(new String(data));


        }catch(IOException ex){

        }
    }


    public void handshake(){

    }
}
