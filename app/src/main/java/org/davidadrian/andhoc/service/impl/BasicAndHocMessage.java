package org.davidadrian.andhoc.service.impl;

import org.davidadrian.andhoc.service.AndHocMessage;

/**
 * Created by david on 8/5/14.
 */
public class BasicAndHocMessage implements AndHocMessage {

    private String name;
    private String message;

    public BasicAndHocMessage(String name, String message) {
        this.name = name;
        this.message = message;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
