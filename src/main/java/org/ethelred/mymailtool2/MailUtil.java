package org.ethelred.mymailtool2;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.util.Supplier;

/**
 * static utils for mail
 */
public final class MailUtil
{
    /**
     * not instantiable
     */
    private MailUtil() {}

    public static Supplier<String> supplyString(Message m) throws MessagingException
    {
        return () -> {
            try {
                return String.format(
                        "@|cyan %tY-%<tm-%<td %<tR|@: @|yellow %s|@", m.getSentDate(), m.getSubject()
                );
            }
            catch (MessagingException e)
            {
                return e.getMessage();
            }
        };
    }
}
