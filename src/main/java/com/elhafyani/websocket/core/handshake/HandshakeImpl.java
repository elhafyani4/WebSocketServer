package com.elhafyani.websocket.core.handshake;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Created by yelha on 1/30/2017.
 */
public class HandshakeImpl {

    private static final String HASH = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static String getAcceptResponse(String webSocketKey) throws NoSuchAlgorithmException {
        String key = webSocketKey + HASH;
        String handShakeAcceptKey = Base64.getEncoder().encodeToString(sha1(key));
        StringBuilder builder = new StringBuilder(512);
        builder.append("HTTP/1.1 101 Switching Protocols\r\n");
        builder.append("Connection: Upgrade\r\n");
        builder.append("Upgrade: websocket\r\n");
        builder.append("Sec-WebSocket-Accept: " + handShakeAcceptKey + "\r\n\r\n");
        return builder.toString();
    }

    private static byte[] sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        return result;
    }
}
