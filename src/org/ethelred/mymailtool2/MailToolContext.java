package org.ethelred.mymailtool2;

import com.google.common.base.Predicate;

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

    public Predicate<Message> defaultMinAge(Task t);

    void connect();

    void disconnect();

    void shutdown();

    void logCompletion(@CheckForNull OperationLimitException e);

    void countMessage();

    void debugF(String format, Object... messageArgs);


    int getChunkSize();
}
