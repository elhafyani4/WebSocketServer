package com.elhafyani.websocketserver;

import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.stereotype.Component;

/**
 * Created by yelhafyani on 1/27/2017.
 */
@Component
public class MessagePrinter {

    final private MessageService service;

    @Autowired
    public MessagePrinter(MessageService service) {
        this.service = service;
    }

    public void printMessage() {
        System.out.println(this.service.getMessage());
    }


}
