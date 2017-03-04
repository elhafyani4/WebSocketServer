package com.elhafyani.websocket.core.protocol.http;

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

import com.elhafyani.websocket.core.protocol.SocketClient;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

/**
 * Created by yelhafyani on 2/3/2017.
 */
public class HttpSocketImpl extends SocketClient implements HttpSocket {

    public static String userdir = System.getProperty( "user.dir" );

    public HttpSocketImpl(SocketChannel socketChannel) {
        super(socketChannel);
    }

    @Override
    public void getHeader(SocketChannel socketChannel) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.allocate( 4096 );
        byteBuffer.clear( );
        socketChannel.read( byteBuffer );
        byteBuffer.flip( );
        byte[] connectHeader = byteBuffer.array( );
        HttpHeader httpHeaders = HttpHeaderParser.parse( new String( connectHeader ) );

        this.setHeaders( httpHeaders );
        byteBuffer.clear( );
    }

    @Override
    public void handleRequest() {

        if (getHeaders( ) == null) {
            ByteBuffer bufferx = ByteBuffer.allocate( 4096 );
            try {
                getSocketChannel( ).read( bufferx );
                bufferx.flip( );
                String x = new String( bufferx.array( ) );
                System.out.println( "request : " + x );
                HttpHeader header = HttpHeaderParser.parse( x );
                setHeaders( header );
            } catch (IOException e) {
                e.printStackTrace( );
            } catch (Exception e) {
                e.printStackTrace( );
            }
        }


        try {
            System.out.println( getHeaders( ).getContext( ) );
            RandomAccessFile aFile = new RandomAccessFile( userdir + "\\static\\" + getHeaders( ).getContext( ), "r" );
            FileChannel inChannel = aFile.getChannel( );
            long fileSize = inChannel.size( );
            String ss = "HTTP/1.0 200 OK\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Server: Apache/2.2.14 (Win32)\r\n" +
                    "Server: Bot\r\n" +
                    "Keep-Alive: timeout=10, max=20\r\n";


            getSocketChannel( ).write( ByteBuffer.wrap( ss.getBytes( Charset.forName( "UTF-8" ) ) ) );


            ByteBuffer buffer = ByteBuffer.allocate( ( int ) fileSize );
            inChannel.read( buffer );
            //buffer.rewind();
            buffer.flip( );
            getSocketChannel( ).write( buffer );
            inChannel.close( );
            aFile.close( );
            setHeaders( null );
            getSocketChannel( ).close( );
        } catch (FileNotFoundException exc) {
            String response = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Length: 22\r\n" +
                    "Content-Type: text/html\r\n\r\n" +
                    "<h1>404 Not Found</h1>";


            try {
                getSocketChannel( ).write( ByteBuffer.wrap( response.getBytes( Charset.forName( "UTF-8" ) ) ) );
            } catch (IOException e) {
                e.printStackTrace( );
            }

            System.out.println( exc );
        } catch (IOException ex) {

        }
    }



}
