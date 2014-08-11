package org.davidadrian.andhoc.service;

/**
 * Created by david on 8/5/14.
 */
public interface AndHocMessenger {

    public AndHocMessage createMessage(String messageText);

    public void send(AndHocMessage message);

}
