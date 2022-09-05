package org.ethelred.mymailtool2;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.io.IOException;
import java.util.Collections;

/**
 * List folders available in the account
 */
public class ListFoldersTask extends TaskBase
{
    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException
    {
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
        System.out.println(f.getFullName() + " " + (f.equals(context.getDefaultFolder()) ? "[default]" : ""));
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
            e.printStackTrace();
        }
    }

    public static Task create()
    {
        return new ListFoldersTask();
    }
}
