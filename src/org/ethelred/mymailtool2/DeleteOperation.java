package org.ethelred.mymailtool2;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
public class DeleteOperation implements MessageOperation
{

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            m.setFlag(Flags.Flag.DELETED, true);
            System.out.printf("Delete message %s%n", m);
            return true;
        }
        catch(MessagingException e)
        {
            Logger.getLogger(DeleteOperation.class.getName()).log(Level.SEVERE, "Error in DeleteOperation", e);
        }
        return false;
    }
    
}
