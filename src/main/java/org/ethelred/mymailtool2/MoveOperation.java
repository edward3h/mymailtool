package org.ethelred.mymailtool2;

import com.google.common.base.MoreObjects;

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

/**
 *
 * @author edward
 */
public class MoveOperation implements MessageOperation
{
    private final String moveToFolderName;

    public MoveOperation(String moveToFolderName)
    {
        this.moveToFolderName = moveToFolderName;
    }

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            Folder startingFolder = m.getFolder();
            Folder moveTo = context.getFolder(moveToFolderName);
            startingFolder.copyMessages(new Message[]{m}, moveTo);
            m.setFlag(Flags.Flag.DELETED, true);
            MailUtil.log("Move message %s from %s to %s", MailUtil.toString(m), startingFolder.getFullName(), moveTo.getFullName());
            return true;
        }
        catch(MessagingException e)
        {
            Logger.getLogger(MoveOperation.class.getName()).log(Level.SEVERE, "Error in MoveOperation", e);
        }
        return false;
    }

    @Override
    public boolean finishApplying()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "Move(" + moveToFolderName + ")";
    }
}
