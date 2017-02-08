package com.elhafyani.websocket.core.server;

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

import com.elhafyani.websocket.core.protocol.Protocol;
import com.elhafyani.websocket.core.protocol.websocket.WebSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Created by yelhafyani on 1/31/2017.
 */
public class WorkerThreadImpl implements WorkerThread, Runnable {

    /**
     * Receive Buffer Size
     */
    private static final int RECEIVE_BUFFER_SIZE = 4096;
    private static final int INITIAL_SIZE_OF_CLIENT_QUEUE = 512;
    private static final Logger LOGGER = Logger.getLogger(ServerImpl.class.getName());
    private int threadId;
    private BlockingQueue<Protocol> clientSocketQueue;
    private ByteBuffer byteBuffer;
    private Thread currentThread;

    public WorkerThreadImpl(int threadId) {
        this.threadId = threadId;
        clientSocketQueue = new LinkedBlockingQueue<>(INITIAL_SIZE_OF_CLIENT_QUEUE);
        byteBuffer = ByteBuffer.allocate(RECEIVE_BUFFER_SIZE);
    }

    public BlockingQueue<Protocol> getClientSocketQueue() {
        return clientSocketQueue;
    }

    public int getThreadId() {
        return threadId;
    }

    public boolean addClientSocketToWorkerQueue(Protocol clientSocket) {
        return clientSocketQueue.add(clientSocket);
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (clientSocketQueue) {
                    if (clientSocketQueue.isEmpty()) {
                        LOGGER.info(this.getThreadId() + " is going to wait for new Selection Key");
                        clientSocketQueue.wait();
                    }
                }

                LOGGER.info(this.getThreadId() + " Wakes up");
                Protocol clientSocket = clientSocketQueue.poll();

                if (clientSocket instanceof WebSocket) {
                    WebSocket cl = (WebSocket) clientSocket;
                    if (clientSocket != null && !cl.isConnected()) {
                        ((WebSocket) clientSocket).handleHandShake();


//                        byteBuffer.clear();
//                        try {
//                            int bufferLen = cl.getSocketChannel().read(byteBuffer);
//                            if (bufferLen > 0) {
//                               // cl.handleHandShake(byteBuffer);
//                            } else {
//                                cl.getSocketChannel().close();
//                            }
//
//                            cl.setConnected(true);
//                        } catch (IOException ex) {
//
//                        }
                        continue;
                    }
                    cl.setCurrentProcessingWorkerThread(this);
                    cl.handleSocketChannelInput(byteBuffer);
                } else {
                    String ss = "HTTP/1.0 200 OK\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Server: Apache/2.2.14 (Win32)\r\n" +
                            "Server: Bot\r\n\r\n" +
                            String.format("<H1>Welcome to the Ultra Mini-WebServer %s </H2>", (new Date()).toString());

                    System.out.println(ss);
                    clientSocket.getSocketChannel().write(ByteBuffer.wrap(ss.getBytes(Charset.forName("UTF-8"))));

                    clientSocket.getSocketChannel().close();

                }

            } catch (InterruptedException ex) {
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
