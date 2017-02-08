package com.elhafyani.websocket.core.frame;

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

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by yelha on 1/30/2017.
 */
public class FrameImpl implements Frame {

    public int payloadLength;
    public int currentRead;
    private int extendedPayLoadLength;
    private boolean FIN;
    private byte[] payload;
    private int opCode;
    private byte[] maskBytes;
    public FrameImpl() {

    }

    public FrameImpl(byte[] headerBytes) throws Exception {
        System.out.println(Integer.toBinaryString(headerBytes[0] & 0xFF));
        System.out.println(Integer.toBinaryString(headerBytes[1] & 0xFF));
        FIN = headerBytes[0] >> 7 == 1;
        opCode = headerBytes[0] & 0x0F;
        payloadLength = headerBytes[1] & 0x7F;
        maskBytes = new byte[4];
        System.out.println(payloadLength);
        if (payloadLength < 125) {
            extendedPayLoadLength = payloadLength;
            maskBytes = Arrays.copyOfRange(headerBytes, 2, 6);
            this.payload = new byte[extendedPayLoadLength];
            if (extendedPayLoadLength < 8) {
                System.arraycopy(headerBytes, 6, this.payload, 0, extendedPayLoadLength);
                currentRead = extendedPayLoadLength;
            } else {
                System.arraycopy(headerBytes, 6, this.payload, 0, 8);
                currentRead = 6;
            }
        } else if (payloadLength == 126) {
            extendedPayLoadLength = ((headerBytes[2] << 8) & 0x0000ff00) | (headerBytes[3] & 0x000000ff);
            maskBytes = Arrays.copyOfRange(headerBytes, 4, 8);
            this.payload = new byte[extendedPayLoadLength];
            System.arraycopy(headerBytes, 8, this.payload, 0, 6);
            currentRead = 6;
        } else if (payloadLength == 127) {
            long dataLength = ((headerBytes[3] << 56) & 0xff00000000000000L)
                    | ((headerBytes[4] << 48) & 0x00ff000000000000L)
                    | ((headerBytes[5] << 40) & 0x0000ff0000000000L)
                    | ((headerBytes[6] << 32) & 0x000000ff00000000L)
                    | ((headerBytes[7] << 24) & 0x00000000ff000000L)
                    | ((headerBytes[8] << 16) & 0x0000000000ff0000L)
                    | ((headerBytes[9] << 8) & 0x000000000000ff00L)
                    | (headerBytes[10] & 0x00000000000000ffL);
            if (dataLength > Integer.MAX_VALUE) {
                throw new Exception("Message is too big");
            }
            extendedPayLoadLength = (int) dataLength;
            this.payload = new byte[extendedPayLoadLength];
            maskBytes = Arrays.copyOfRange(maskBytes, 10, 14);
        }
    }

    public byte[] getPayload() {
        return payload;
    }

    public synchronized void addPayload(ByteBuffer payload) {

        payload.flip();
        while (payload.hasRemaining()) {
            this.payload[currentRead++] = payload.get();
        }
        payload.clear();
    }

    public void setPayloadLength() {
    }

    @Override
    public String getPayloadText() {
        byte[] unmaskedData = new byte[extendedPayLoadLength];
        for (int k = 0; k < this.payload.length; k++) {
            unmaskedData[k] = (byte) (this.payload[k] ^ maskBytes[k & 0x3]);
        }
        return new String(unmaskedData);
    }

    public int getCurrentRead() {
        return currentRead;
    }

    public void setCurrentRead(int currentRead) {
        this.currentRead = currentRead;
    }

    public byte[] getMaskBytes() {
        return maskBytes;
    }

    public void setMaskBytes(byte[] maskBytes) {
        this.maskBytes = maskBytes;
    }

    public int getExtendedPayLoadLength() {
        return extendedPayLoadLength;
    }

    public void setExtendedPayLoadLength(int extendedPayLoadLength) {
        this.extendedPayLoadLength = extendedPayLoadLength;
    }

    public int getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    public boolean isFIN() {
        return FIN;
    }

    public int getOpCode() {
        return opCode;
    }



    public enum MessageType {CONTINUOUS, TEXT, BINARY, CLOSE, PING, PONG}

    public enum Status {CONNECTING, OPEN, CLOSING}
}
