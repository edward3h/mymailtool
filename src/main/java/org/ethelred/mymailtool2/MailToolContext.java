package org.ethelred.mymailtool2;

import java.util.function.Predicate;

import javax.annotation.CheckForNull;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

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

    int getChunkSize();

    boolean randomTraversal();
}
