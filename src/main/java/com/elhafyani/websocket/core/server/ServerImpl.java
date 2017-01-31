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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by yelha on 1/30/2017.
 */
public class ServerImpl implements Server {

    private static final int RECEIVE_BUFFER_SIZE = 16384;
    private static final Logger LOGGER = Logger.getLogger(ServerImpl.class.getName());
    private static AtomicInteger buffersQueueSize = new AtomicInteger(0);
    private int portId = 9999;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private LinkedBlockingQueue<ByteBuffer> buffers;
    private List<Socket> connection;

    public ServerImpl(int portId) {
        this.portId = portId;
    }

    public void run() {
        try {
            InetSocketAddress address = new InetSocketAddress(this.portId);
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.setReceiveBufferSize(RECEIVE_BUFFER_SIZE);
            selector = Selector.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(address);
            SelectionKey selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Error Starting Server ?", exception.getMessage());
        }

        try {
            selector.select();
            Set<SelectionKey> selectionKeySet = selector.selectedKeys();
            Iterator iterator = selectionKeySet.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                if (key.isAcceptable()) { //new client socket
                    key.attachment();
                    key.channel().re

                }

            }

        } catch (IOException ex) {
            LOGGER.log(Level.ALL, ex.getMessage());
        }
    }
}
