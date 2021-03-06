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
import com.elhafyani.websocket.core.protocol.ProtocolFactory;
import com.elhafyani.websocket.core.protocol.http.HttpSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;
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


    private static final Logger LOGGER = Logger.getLogger( ServerImpl.class.getName( ) );
    private static final int NUMBER_OF_WORKING_THREADS = 2;
    private int portId = 9999;
    private Selector selector;
    private LinkedBlockingQueue<ByteBuffer> reusableBuffers;
    private List<SocketChannel> connection;
    private List<WorkerThread> workerThreads;
    private AtomicInteger currentWorkThread = new AtomicInteger( 0 );
    private Set<SelectionKey> selectionKeySet;
    private ByteBuffer byteBuffer;

    public ServerImpl(int portId) {

        LOGGER.info( "starting the server" );
        this.portId = portId;
        byteBuffer = ByteBuffer.allocate( 4096 );
        workerThreads = new ArrayList<>( NUMBER_OF_WORKING_THREADS );
        for (int i = 0; i < NUMBER_OF_WORKING_THREADS; i++) {
            WorkerThread wt = new WorkerThreadImpl( i );
            LOGGER.info( "starting Thread " + i );
            workerThreads.add( wt );
            Thread t = new Thread( ( WorkerThreadImpl ) wt );
            t.start( );
        }
    }

    public static void main(String[] args) {
        try {
            s( );
        } catch (IOException e) {
            e.printStackTrace( );
        }
        Server server = new ServerImpl( 81 );
        try {
            server.start( );
        } catch (InterruptedException e) {
            e.printStackTrace( );
        }
    }

    public static void s() throws IOException {
        Enumeration<URL> resource = ClassLoader.getSystemResources( "com.elhafyani.websocket.core.server" );
        while (resource.hasMoreElements( )) {
            URL ss = resource.nextElement( );
            System.out.println( ss.getFile( ) );
        }
    }

    private WorkerThread getWorkThread() {
        WorkerThread w = workerThreads.get( currentWorkThread.getAndIncrement( ) % NUMBER_OF_WORKING_THREADS );
        LOGGER.info( "Getting Thread " + w.getThreadId( ) );
        return w;
    }

    public void start() {

        try {
            InetSocketAddress addr = new InetSocketAddress( this.portId );
            ServerSocketChannel ssChnl = ServerSocketChannel.open( );
            ServerSocket srvSokt = ssChnl.socket( );
            srvSokt.setReceiveBufferSize( 65536 );
            selector = Selector.open( );
            ssChnl.configureBlocking( false );
            ssChnl.bind( addr );
            SelectionKey selKey = ssChnl.register( selector, SelectionKey.OP_ACCEPT );
            selKey.attach( 5 );

        } catch (IOException exception) {
            LOGGER.log( Level.FINE, "Error Starting Server ?", exception.getMessage( ) );
        }

        //start looping through the selectionKeys and see if any have any valid options
        while (true) {
            try {
                selector.select( );
                selectionKeySet = selector.selectedKeys( );
                Iterator keys = selectionKeySet.iterator( );
                while (keys.hasNext( )) {

                    SelectionKey key = ( SelectionKey ) keys.next( );

                    if (!key.isValid( ))
                        continue;

                    if (key.isAcceptable( )) { //new client socket
                        LOGGER.info( "Remote Connection" );
                        ServerSocketChannel sc = ( ServerSocketChannel ) key.channel( );
                        SocketChannel socketChannel = sc.accept( );
                        if (socketChannel != null) {
                            //socketChannel.socket().setTcpNoDelay( true );
                            socketChannel.configureBlocking( false );
                            socketChannel.socket( ).setKeepAlive( true );

                            SelectionKey clientKey = socketChannel.register( selector, SelectionKey.OP_READ );
                            Protocol clientSocket = ProtocolFactory.GetProtocol( socketChannel, byteBuffer );
                            clientKey.attach( clientSocket );
                            (( HttpSocket ) clientSocket).handleRequest( );
//                        WorkerThread wt = getWorkThread( );
//                        addClientSocketToThreadQueue( wt, clientSocket );
                        }


                    }

                    if (key.isReadable( )) {
                        LOGGER.info( "Readable" );
                        Protocol clientSocket = ( Protocol ) key.attachment( );
                        SocketChannel socketChannel = ( SocketChannel ) key.channel( );
//                        socketChannel.
                        if (clientSocket != null) {
                            (( HttpSocket ) clientSocket).getHeader( clientSocket.getSocketChannel( ) );
                            (( HttpSocket ) clientSocket).handleRequest( );
//                        addClientSocketToThreadQueue( clientSocket.getCurrentProcessingWorkerThread(), clientSocket );
                        }

                    }
                    keys.remove( );
                }

            } catch (IOException ex) {
                LOGGER.log( Level.ALL, ex.getMessage( ) );
            } catch (Exception e) {
                e.printStackTrace( );
            }
            try {
                Thread.sleep( 10 );
            } catch (InterruptedException e) {
                e.printStackTrace( );
            }
        }
    }

    public void addClientSocketToThreadQueue(WorkerThread wt, Protocol clientSocket) {
        synchronized (wt.getClientSocketQueue( )) {
            wt.addClientSocketToWorkerQueue( clientSocket );
            wt.getClientSocketQueue( ).notify( );
        }
    }

}
