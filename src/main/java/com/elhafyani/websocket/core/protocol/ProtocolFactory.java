package com.elhafyani.websocket.core.protocol;

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

import com.elhafyani.websocket.core.protocol.http.HttpHeader;
import com.elhafyani.websocket.core.protocol.http.HttpHeaderParser;
import com.elhafyani.websocket.core.protocol.http.HttpSocketImpl;
import com.elhafyani.websocket.core.protocol.websocket.WebSocketImpl;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by yelhafyani on 2/3/2017.
 */
public class ProtocolFactory {

    public static Protocol GetProtocol(SocketChannel socketChannel, ByteBuffer byteBuffer) throws Exception {
        byteBuffer = ByteBuffer.allocate( 4096 );
        byteBuffer.clear();
        Protocol protocol;
        socketChannel.read(byteBuffer);
        byteBuffer.flip();
        byte[] connectHeader = byteBuffer.array();
        HttpHeader httpHeaders = HttpHeaderParser.parse(new String(connectHeader));
        if (httpHeaders.getHeaders().containsKey("Upgrade") && httpHeaders.getHeaders().get("Upgrade").equals("WebSocket")) {
            protocol = new WebSocketImpl(socketChannel);
        } else {
            protocol = new HttpSocketImpl(socketChannel);
        }
        protocol.setHeaders(httpHeaders);
        byteBuffer.clear();
        return protocol;
    }
}
