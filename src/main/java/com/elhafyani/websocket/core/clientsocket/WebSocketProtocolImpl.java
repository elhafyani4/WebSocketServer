package com.elhafyani.websocket.core.clientsocket;

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

import com.elhafyani.websocket.core.frame.Frame;
import com.elhafyani.websocket.core.handshake.HandshakeImpl;
import com.elhafyani.websocket.core.http.HttpHeaderParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by yelhafyani on 1/31/2017.
 */
public class WebSocketProtocolImpl implements WebSocketProtocol {

    private boolean isConnected;
    private SocketChannel socketChannel;
    private LinkedBlockingQueue<ByteBuffer> inBuffers;
    private LinkedBlockingQueue<ByteBuffer> outBuffers;
    private List<Frame> dataFrames;


    public WebSocketProtocolImpl(SocketChannel socket) {
        this.socketChannel = socket;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public synchronized void addFrame(Frame frame) {
        dataFrames.add(frame);
    }

    @Override
    public boolean handleHandShake(ByteBuffer byteBuffer) {
        byteBuffer.flip();
        byte[] connectHeader = byteBuffer.array();
        Map<String, String> httpHeaders = HttpHeaderParser.parse(new String(connectHeader));
        try {
            String responseHandshake = HandshakeImpl.getAcceptResponse(httpHeaders.get("Sec-WebSocket-Key"));
            socketChannel.write(ByteBuffer.wrap(responseHandshake.getBytes(Charset.forName("UTF-8"))));
            setConnected(true);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean handleSocketChannelInput() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(2);
        socketChannel.read(header);
        header.flip();
        byte[] headerBytes = header.array();

    }

}
