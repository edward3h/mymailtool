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
                "%tY-%<tm-%<td:%s", m.getSentDate(), m.getSubject()
        );
    }

    public static void log(String format, Object... args)
    {
        System.out.printf("%tF %<tT ", System.currentTimeMillis());
        System.out.printf(format, args);
        System.out.println();
    }
}
