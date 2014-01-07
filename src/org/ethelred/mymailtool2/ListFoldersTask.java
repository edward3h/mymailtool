package org.ethelred.mymailtool2;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.Collections;

/**
 * List folders available in the account
 */
public class ListFoldersTask extends TaskBase
{
    @Override
    protected void runMessage(Folder f, Message m, boolean includeSubFolders, String originalName) throws MessagingException
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
    protected void status(Folder f, String originalName)
    {
        System.out.println(f.getFullName());
    }

    @Override
    public void run()
    {
        Folder root = context.getDefaultFolder();
        try
        {
            traverseFolder(root, true, "");
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
    }

    public static Task create()
    {
        return new ListFoldersTask();
    }
}
