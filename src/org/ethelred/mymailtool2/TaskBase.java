package org.ethelred.mymailtool2;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
abstract class TaskBase implements Task
{
    protected MailToolContext context;

    @Override
    public void init(MailToolContext ctx)
    {
        this.context = ctx;
    }


    protected void traverseFolder(String folderName, boolean includeSubFolders) throws MessagingException
    {
        Folder f = context.getFolder(folderName);
        if(f == null || !f.exists())
        {
            throw new IllegalStateException("Could not open folder " + folderName);
        }

        traverseFolder(f, includeSubFolders);
    }

    protected void traverseFolder(Folder f, boolean includeSubFolders) throws MessagingException
    {

        status(f);

        if((f.getType() & Folder.HOLDS_MESSAGES) > 0)
        {
            f.open(openMode());

            try
            {
                for(Message m: _readMessages(f))
                {
                    runMessage(f, m, includeSubFolders);
                }
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
                traverseFolder(child, includeSubFolders);
            }
        }
    }

    protected abstract void runMessage(Folder f, Message m, boolean includeSubFolders) throws MessagingException;

    protected int openMode()
    {
        return Folder.READ_ONLY;
    }

    protected abstract void status(Folder f);

    private Iterable<? extends Message> _readMessages(Folder f)
    {
        return new RecentMessageIterable(f, orderNewestFirst());
    }

    protected boolean orderNewestFirst()
    {
        return true;
    }

}
