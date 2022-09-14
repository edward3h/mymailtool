package org.ethelred.mymailtool2;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 *
 * @author edward
 */
abstract class TaskBase implements Task
{
    private static final Logger LOGGER = LogManager.getLogger();
    protected MailToolContext context;

    @Override
    public void init(MailToolContext ctx)
    {
        this.context = ctx;
    }


    protected void traverseFolder(String folderName, boolean includeSubFolders, boolean readMessages) throws MessagingException, IOException
    {
        Folder f = context.getFolder(folderName);
        if(f == null || !f.exists())
        {
            throw new IllegalStateException("Could not open folder " + folderName);
        }

        traverseFolder(f, includeSubFolders, readMessages);
    }

    protected void traverseFolder(Folder f, boolean includeSubFolders, boolean readMessages) throws MessagingException, IOException
    {
        status(f);

        if(readMessages && (f.getType() & Folder.HOLDS_MESSAGES) > 0)
        {
            f.open(openMode());

            try
            {
                for(Message m: readMessages(f))
                {
                    runMessage(f, m);
                }
            }
            catch (ShortcutFolderScanException sc)
            {
                LOGGER.info("Short cut on folder {}", f.getName());
            }
            finally {
                if((openMode() & Folder.READ_WRITE) > 0) {
                    f.expunge();
                }
                f.close(true);
            }
        }

        if(includeSubFolders && (f.getType() & Folder.HOLDS_FOLDERS) > 0)
        {
            for(Folder child: f.list())
            {
                traverseFolder(child, includeSubFolders, readMessages);
            }
        }
    }

    protected abstract void runMessage(Folder f, Message m) throws MessagingException, IOException;

    protected int openMode()
    {
        return Folder.READ_ONLY;
    }

    protected abstract void status(Folder f);

    protected Iterable<? extends Message> readMessages(Folder f)
    {
        return new RecentMessageIterable(f, orderNewestFirst(), context.getChunkSize());
    }

    @Override
    public boolean orderNewestFirst()
    {
        return true;
    }

}
