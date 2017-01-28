package com.elhafyani.websocketserver;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class SocketRequestHandler implements Runnable {

    private DataInputStream is;
    private DataOutputStream os;
    private Client client;
    private static final String HASH = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final byte[] PONG_BYTES = new byte[]{(byte) 0x8A, 0x00};
    private static final byte[] PING_BYTES = new byte[]{(byte) 0x89, 0x00};

    public SocketRequestHandler(Client client) {
        this.client = client;
        try {
            is = new DataInputStream(client.socket.getInputStream());
            os = new DataOutputStream(client.socket.getOutputStream());
        } catch (IOException exception) {

        }
    }

    @Override
    public void run() {
        try {
            if (!client.isConnected) {
                doHandShake();
                return;
            }

            processRequest();

        } catch (IOException ex) {

        } catch (NoSuchAlgorithmException ex) {

        }
    }

    public void processRequest() throws IOException {
        int available = is.available();
        byte[] data = new byte[available];
        is.read(data, 0, available);
        is.mark(available);
        int isFin = (data[0] >> 7) & 1;

        //read opcode
        switch (data[0] & 0x0f) {
            case 9:
                sendPong();
                break;
            case 0: //continuous
                break;
            case 1: //text

                break;
            case 2: //binary
                break;
            case 8:
                closeConnection();
                break;
        }


//
//        if((data[0]&0x0f) == 10){
//            System.out.println("its a pong");
//        }else {
//            byte[] key = Arrays.copyOfRange(v, 2, 6);
//            byte[] data = Arrays.copyOfRange(v, 6, available);
//            byte[] d = new byte[data.length];
//        }
    }

    private void sendResponse(byte[] message, MessageType messageType) {
        byte firstByte = 0;
        switch (messageType) {
            case CONTINOUS:
                firstByte = 0 + (byte) 128;
                break;
            case TEXT:
                firstByte = 1 + (byte) 128;
                break;
            case BINARY:
                firstByte = 2 + (byte) 128;
                break;
            case CLOSE:
                firstByte = 8 + (byte) 128;
                break;
            case PING:
                firstByte = 9 + (byte) 128;
                break;
            case PONG:
                firstByte = 10 + (byte) 128;
                break;
        }

        int messageLength = message.length;
        byte secondByte = 0;


        if(messageLength < 126){
            secondByte = (byte)messageLength;
        }else if(messageLength <= 65536){
            secondByte = 126;
        }
    }

    private void closeConnection() throws IOException {
        this.is.close();
        this.os.close();
        this.client.socket.close();
    }

    private void sendPing() throws IOException {
        os.write(PING_BYTES);
    }


    private void sendPong() throws IOException {
        os.write(PONG_BYTES);
    }

    private void doHandShake() throws IOException, NoSuchAlgorithmException {
        String webSocketKey = getWebSocketKey(is);
        String key = webSocketKey + HASH;
        String handShakeAcceptKey = Base64.getEncoder().encodeToString(sha1(key));
        os.writeBytes("HTTP/1.1 101 Switching Protocols\r\n");
        os.writeBytes("Connection: Upgrade\r\n");
        os.writeBytes("Upgrade: websocket\r\n");
        os.writeBytes("Sec-WebSocket-Accept: " + handShakeAcceptKey + "\r\n\r\n");
        client.isConnected = true;
    }

    private static byte[] sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        return result;
    }

    private static String getWebSocketKey(DataInputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while (true) {
            String line = br.readLine();
            if (!line.isEmpty()) {
                Pattern p = Pattern.compile("^Sec-WebSocket-Key: (.+?)$", Pattern.MULTILINE);
                Matcher m = p.matcher(line);
                if (m.find()) {
                    return m.group(1);
                }
            } else {
                break;
            }
        }
        return null;
    }
}
