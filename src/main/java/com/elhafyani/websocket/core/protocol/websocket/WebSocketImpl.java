package com.elhafyani.websocket.core.protocol.websocket;

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
import com.elhafyani.websocket.core.frame.FrameImpl;
import com.elhafyani.websocket.core.handshake.HandshakeImpl;
import com.elhafyani.websocket.core.protocol.SocketClient;
import com.elhafyani.websocket.core.server.WorkerThread;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by yelhafyani on 1/31/2017.
 */
public class WebSocketImpl extends SocketClient implements WebSocket {


    private LinkedBlockingQueue<ByteBuffer> inBuffers;
    private LinkedBlockingQueue<ByteBuffer> outBuffers;
    private Stack<Frame> dataFrames;

    private WorkerThread currentProcessingWorkerThread;

    public WebSocketImpl(SocketChannel socket) {
        super(socket);
        dataFrames = new Stack<>();
    }

    public WorkerThread getCurrentProcessingWorkerThread() {
        return currentProcessingWorkerThread;
    }

    public void setCurrentProcessingWorkerThread(WorkerThread currentProcessingWorkerThread) {
        this.currentProcessingWorkerThread = currentProcessingWorkerThread;
    }

    public synchronized void addFrame(Frame frame) {
        dataFrames.add(frame);
    }

    @Override
    public boolean handleHandShake() {
        try {
            String responseHandshake = HandshakeImpl.getAcceptResponse(getHeaders().getHeaders().get("Sec-WebSocket-Key"));
            System.out.println(responseHandshake);
            getSocketChannel().write(ByteBuffer.wrap(responseHandshake.getBytes(Charset.forName("UTF-8"))));
            setConnected(true);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean handleSocketChannelInput(ByteBuffer byteBuffer) throws Exception {
        if (dataFrames.isEmpty() || dataFrames.peek().getCurrentRead() >= dataFrames.peek().getExtendedPayLoadLength()) {
            System.out.println("start handle socket channel input");
            ByteBuffer header = ByteBuffer.allocate(14);
            getSocketChannel().read(header);
            header.flip();
            byte[] headerBytes = header.array();
            Frame frame = new FrameImpl(headerBytes);
            ByteBuffer b = ByteBuffer.allocate(4096);
            b.clear();
            while (frame.getCurrentRead() < frame.getExtendedPayLoadLength()) {
                System.out.println(frame.getCurrentRead() + " " + frame.getExtendedPayLoadLength());
                int read = getSocketChannel().read(b);
                if (read > 0) {
                    frame.addPayload(b);
                    b.compact();
                } else {
                    currentProcessingWorkerThread.addClientSocketToWorkerQueue(this);
                    break;
                }
            }


            dataFrames.push(frame);
            System.out.println(frame.getPayloadText());
        } else {
            Frame frame = dataFrames.peek();
            ByteBuffer b = ByteBuffer.allocate(4096);
            b.clear();
            while (frame.getCurrentRead() < frame.getExtendedPayLoadLength()) {
                System.out.println(frame.getCurrentRead() + " " + frame.getExtendedPayLoadLength());
                int read = getSocketChannel().read(b);
                if (read > 0) {
                    frame.addPayload(b);
                    b.compact();
                } else {
                    break;
                }
            }
            System.out.println(frame.getPayloadText());
        }
        return true;
    }

}
