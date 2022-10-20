package org.ethelred.mymailtool2;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Collections;

/**
 * List folders available in the account
 */
public class ListFoldersTask extends TaskBase
{
    private static final Logger LOGGER = LogManager.getLogger(ListFoldersTask.class);

    @Override
    protected void runMessage(Folder f, Message m) {
        // don't do anything
    }

    /**
     * overridden to avoid reading messages
     * @param f
     * @return
     */
    @Override
    protected Iterable<? extends Message> readMessages(Folder f)
    {
        return Collections.emptyList();
    }

    @Override
    protected void status(Folder f)
    {
        LOGGER.info("{} {}", f.getFullName(), (f.equals(context.getDefaultFolder()) ? "[default]" : ""));
    }

    @Override
    public void run()
    {
        Folder root = context.getDefaultFolder();
        try
        {
            traverseFolder(root, true, false);
        }
        catch (MessagingException | IOException e)
        {
            LOGGER.error("Exception", e);
        }
    }

    public static Task create()
    {
        return new ListFoldersTask();
    }
}
