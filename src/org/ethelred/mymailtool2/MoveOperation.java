package org.ethelred.mymailtool2;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

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
            System.out.printf("Move message %s from %s to %s%n", MailUtil.toString(m), startingFolder.getFullName(), moveTo.getFullName());
            return true;
        }
        catch(MessagingException e)
        {
            Logger.getLogger(MoveOperation.class.getName()).log(Level.SEVERE, "Error in MoveOperation", e);
        }
        return false;
    }
    
}
