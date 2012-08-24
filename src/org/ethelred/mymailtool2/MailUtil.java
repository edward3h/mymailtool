package org.ethelred.mymailtool2;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * static utils for mail
 */
public class MailUtil
{
    /**
     * not instantiable
     */
    private MailUtil(){}

    public static String toString(Message m) throws MessagingException
    {
        return String.format(
                "%tY-%tm-%td:%s", m.getSentDate(), m.getSentDate(), m.getSentDate(), m.getSubject()
        );
    }
}
