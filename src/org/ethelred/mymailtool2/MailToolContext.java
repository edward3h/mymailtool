package org.ethelred.mymailtool2;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
interface MailToolContext
{

    public void countOperation();

    public Folder getFolder(String folderName);

    public Folder getDefaultFolder();

    public boolean isOldEnough(Message m) throws MessagingException;

    void connect();

    void disconnect();

    void shutdown();
}
