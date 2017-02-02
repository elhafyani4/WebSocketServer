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

import com.elhafyani.websocket.core.ChatHub;
import com.elhafyani.websocket.core.clientsocket.Protocol;
import com.elhafyani.websocket.core.clientsocket.WebSocketProtocolImpl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yelha on 1/30/2017.
 */
public class ServerImpl implements Server {


    private static final Logger LOGGER = Logger.getLogger(ServerImpl.class.getName());
    private static final int NUMBER_OF_WORKING_THREADS = 2;
    public static volatile Map<String, Class> Handlers;
    private static AtomicInteger reusableBuffersQueueSize = new AtomicInteger(0);
    private int portId = 9999;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private LinkedBlockingQueue<ByteBuffer> reusableBuffers;
    private List<SocketChannel> connection;
    private List<WorkerThread> workerThreads;
    private int currentWorkThread = 0;

    public ServerImpl(int portId) {
        Handlers = new HashMap<>();

        Handlers.put("Chat", ChatHub.class);

        LOGGER.info("starting the server");
        this.portId = portId;

        workerThreads = new ArrayList<>(NUMBER_OF_WORKING_THREADS);
        for (int i = 0; i < NUMBER_OF_WORKING_THREADS; i++) {
            WorkerThread wt = new WorkerThreadImpl(i);
            LOGGER.info("starting Thread " + i);
            workerThreads.add(wt);
            Thread t = new Thread((WorkerThreadImpl) wt);
            t.start();
        }
    }

    public static void main(String[] args) {
        Server server = new ServerImpl(81);
        server.run();
    }

    private WorkerThread getWorkThread() {
        WorkerThread w = workerThreads.get(currentWorkThread++ % NUMBER_OF_WORKING_THREADS);
        LOGGER.info("Getting Thread " + w.getThreadId());
        return w;
    }


    public void run() {
        try {
            InetSocketAddress address = new InetSocketAddress(this.portId);
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.setReceiveBufferSize(4096);
            selector = Selector.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(address);
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Error Starting Server ?", exception.getMessage());
        }


        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator iterator = selectionKeySet.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();

                    iterator.remove();

                    if (key.isAcceptable()) { //new client socket
                        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        SocketChannel socketChannel = serverChannel.accept();
                        Protocol clientSocket = new WebSocketProtocolImpl(socketChannel);
                        socketChannel.configureBlocking(false);
                        key.attach(clientSocket);
                        WorkerThread wt = getWorkThread();
                        addClientSocketToThreadQueue(wt, clientSocket);

                        continue;
                    }

                    if (key.isReadable()) {
                        Protocol clientSocket = (Protocol) key.attachment();
                        WorkerThread wt = getWorkThread();
                        addClientSocketToThreadQueue(wt, clientSocket);
                    }
                }

            } catch (IOException ex) {
                LOGGER.log(Level.ALL, ex.getMessage());
            }
//            }catch(InterruptedException ex){
//
//            }
        }
    }

    public void addClientSocketToThreadQueue(WorkerThread wt, Protocol clientSocket) {
        synchronized (wt.getClientSocketQueue()) {
            wt.addClientSocketToWorkerQueue(clientSocket);
            wt.getClientSocketQueue().notify();
        }
    }

}
