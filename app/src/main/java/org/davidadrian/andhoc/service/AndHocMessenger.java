package org.davidadrian.andhoc.service;

/**
 * Created by david on 8/5/14.
 */
public interface AndHocMessenger {

    public AndHocMessage createMessage(String messageText);

    public void setMessage(AndHocMessage message);

    public void broadcast();

    public void stopBroadcast();

}
