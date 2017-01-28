package com.RequestHandler;

/**
 * Created by yelha on 1/28/2017.
 */
public interface IHandleRequest<T> {

    void Handle(T message);

}
