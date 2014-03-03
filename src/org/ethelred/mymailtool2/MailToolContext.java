package org.ethelred.mymailtool2;

import javax.annotation.CheckForNull;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
public interface MailToolContext
{

    public void countOperation();

    public Folder getFolder(String folderName);

    public Folder getDefaultFolder();

    public boolean isOldEnough(Message m) throws MessagingException;

    void connect();

    void disconnect();

    void shutdown();

    void logCompletion(@CheckForNull OperationLimitException e);

    void countMessage();
}
