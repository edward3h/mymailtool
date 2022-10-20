package org.ethelred.mymailtool2;

import java.util.function.Predicate;

import javax.annotation.CheckForNull;
import jakarta.mail.Folder;
import jakarta.mail.Message;

/**
 *
 * @author edward
 */
public interface MailToolContext
{

    void countOperation();

    Folder getFolder(String folderName);

    Folder getDefaultFolder();

    Predicate<Message> defaultMinAge(Task t);

    void connect();

    void disconnect();

    void shutdown();

    void logCompletion(@CheckForNull OperationLimitException e);

    void countMessage();

    int getChunkSize();

    boolean randomTraversal();
}
