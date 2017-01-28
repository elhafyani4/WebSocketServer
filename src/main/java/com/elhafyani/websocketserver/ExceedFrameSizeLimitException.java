package com.elhafyani.websocketserver;

/**
 * Created by yelha on 1/28/2017.
 */
public class ExceedFrameSizeLimitException extends Exception {

    public ExceedFrameSizeLimitException(String message) {
        super(message);
    }
}
