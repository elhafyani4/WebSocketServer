package com.elhafyani.websocket.core;

/*
 *
 *   This software is released under a BSD-style license:
 *   Copyright (c) 2017 Youssef Elhafyani. All rights reserved.
 *   Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are
 *   met:
 *   1.  Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *   2.  The end-user documentation included with the redistribution, if any,
 *       must include the following acknowlegement:
 *         "This product includes software developed by Youssef Elhafyani
 *         (elhafyani4@gmail.com, http://www.ngglo.com/). That software is
 *         copyright (c) 2017 Youssef Elhafyani."
 *       Alternately, this acknowlegement may appear in the software itself,
 *       if wherever such third-party acknowlegements normally appear.
 *
 *   THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *   WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *   MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 *   NO EVENT SHALL YOUSSEF ELHAFYANI BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *   NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * \*---------------------------------------------------------------------------
 */

import com.elhafyani.websocket.core.exceptions.ExceedFrameSizeLimitException;
import com.elhafyani.websocket.core.frame.MessageType;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yelhafyani on 1/27/2017.
 */
public class SocketRequestHandler implements Runnable {

    private static final String HASH = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final byte[] PONG_BYTES = new byte[]{(byte) 0x8A, 0x00};
    private static final byte[] PING_BYTES = new byte[]{(byte) 0x89, 0x00};
    private static final long MAX_DATA_FRAME = 0x400000L; //2^22 bytes which equals ~4MB
    private DataInputStream is;
    private OutputStream os;
    private Client client;
    private byte[] receivedData = new byte[0];

    public SocketRequestHandler(Client client) {
        this.client = client;
        try {
            is = new DataInputStream(client.socket.getInputStream());
            os = client.socket.getOutputStream();
        } catch (IOException exception) {

        }
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

    public static byte[] combine(byte[] a, byte[] b) {
        int length = a.length + b.length;
        byte[] result = new byte[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    @Override
    public void run() {
        try {
            if (!client.isConnected) {
                doHandShake();
            } else {
                processRequest();
            }

            client.status = Status.READY;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (ExceedFrameSizeLimitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Process the Stream Request by reading the frame
     *
     * @throws IOException
     */
    public void processRequest() throws Exception {
        try {
            int available = is.available();
            System.out.println("AVAILABLE " + available);
            if (available > 0) {
                byte[] frame = new byte[available];
                is.read(frame, 0, available);
                is.mark(available);
                int isFin = 0;
                byte opCode = (byte) (frame[0] & 0x0f);
                System.out.println("OPCODE " + opCode);


                //read opcode
                switch (frame[0] & 0x0f) {
                    case 9:
                        sendPong();
                        break;
                    case 0: //continuous
                        System.out.println("Continous frame");
                        break;
                    case 1:
                    case 2: //text
                        isFin = (frame[0] >>> 7) & 1; //not used , only one message is supported
                        if (isFin == 0) {
                            client.status = Status.READING;
                        } else {
                            client.status = Status.PROCESSING;
                        }
                        System.out.println("FIN : " + isFin);
                        byte[] key = null, data = null, unmaskedData = null;
                        byte secondByte = (byte) (frame[1] & 0x7F);
                        System.out.println("LENGTH: " + secondByte);
                        if (secondByte < 125) {
                            key = Arrays.copyOfRange(frame, 2, 6);
                            data = Arrays.copyOfRange(frame, 6, available);
                            unmaskedData = new byte[data.length];
                        } else if (secondByte == 126) {
                            byte[] lenBytes = Arrays.copyOfRange(frame, 2, 4);
                            int dataLength = ((lenBytes[0] << 8) & 0x0000ff00) | (lenBytes[1] & 0x000000ff);
                            key = Arrays.copyOfRange(frame, 4, 8);
                            data = Arrays.copyOfRange(frame, 8, available);
                            unmaskedData = new byte[dataLength];
                        } else if (secondByte == 127) {
                            byte[] lenBytes = Arrays.copyOfRange(frame, 2, 10);
                            long dataLength = ((lenBytes[0] << 56) & 0xff00000000000000L)
                                    | ((lenBytes[1] << 48) & 0x00ff000000000000L)
                                    | ((lenBytes[2] << 40) & 0x0000ff0000000000L)
                                    | ((lenBytes[3] << 32) & 0x000000ff00000000L)
                                    | ((lenBytes[4] << 24) & 0x00000000ff000000L)
                                    | ((lenBytes[5] << 16) & 0x0000000000ff0000L)
                                    | ((lenBytes[6] << 8) & 0x000000000000ff00L)
                                    | (lenBytes[7] & 0x00000000000000ffL);
                            System.out.println("DataLength 127: " + dataLength);

                            if (dataLength > MAX_DATA_FRAME) {
                                throw new ExceedFrameSizeLimitException("Client Exceeded the limit of the frame");
                            }

                            int read = available;
                            key = Arrays.copyOfRange(frame, 10, 14);
                            data = Arrays.copyOfRange(frame, 14, available);
                            unmaskedData = new byte[(int) dataLength];
                            while (read <) {

                            }


                        } else {
                            throw new Exception("can't understand the request");
                        }

                        for (int k = 0; k < data.length; k++) {
                            unmaskedData[k] = (byte) (data[k] ^ key[k & 0x3]);
                        }

                        combine(this.receivedData, unmaskedData);

                        if (isFin == 1) {
                            String response = new String(this.receivedData);
                            System.out.println(response);


                            sendResponse(unmaskedData, MessageType.TEXT);
                        }

                        break;
                    case 8:
                        closeConnection();
                        break;
                }
            }
        } catch (Exception ex) {
            closeConnection();
            throw ex;
        }
    }

    private void sendResponse(byte[] message, MessageType messageType) throws IOException {
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

        byte[] frameHeader;
        if (messageLength < 126) {
            secondByte = (byte) messageLength;
            frameHeader = new byte[]{firstByte, secondByte};
            os.write(frameHeader);
        } else if (messageLength <= 65536) {
            secondByte = 126;
            frameHeader = new byte[]{firstByte, secondByte, (byte) (messageLength >>> 8), (byte) (messageLength >>> 0)};
            os.write(frameHeader);
        } else {
            secondByte = 127;
            long longLengthByte = (long) messageLength;
            frameHeader = new byte[]{
                    firstByte,
                    secondByte,
                    (byte) (longLengthByte >>> 56),
                    (byte) (longLengthByte >>> 48),
                    (byte) (longLengthByte >>> 40),
                    (byte) (longLengthByte >>> 32),
                    (byte) (longLengthByte >>> 24),
                    (byte) (longLengthByte >>> 16),
                    (byte) (longLengthByte >>> 8),
                    (byte) (longLengthByte >>> 0)
            };
            os.write(frameHeader);

        }
        os.write(message);
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
        os.write("HTTP/1.1 101 Switching Protocols\r\n".getBytes(Charset.forName("UTF-8")));
        os.write("Connection: Upgrade\r\n".getBytes(Charset.forName("UTF-8")));
        os.write("Upgrade: websocket\r\n".getBytes(Charset.forName("UTF-8")));
        os.write(("Sec-WebSocket-Accept: " + handShakeAcceptKey + "\r\n\r\n").getBytes(Charset.forName("UTF-8")));
        os.flush();
        client.isConnected = true;
    }
}
