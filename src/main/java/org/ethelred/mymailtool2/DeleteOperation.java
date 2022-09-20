package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author edward
 */
public class DeleteOperation implements MessageOperation
{

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        try
        {
            m.setFlag(Flags.Flag.DELETED, true);
            LOGGER.info("Delete message {}", MailUtil.supplyString(m));
            return true;
        }
        catch (MessagingException e)
        {
            LOGGER.error("Error in DeleteOperation", e);
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
        return "Delete";
    }

}
