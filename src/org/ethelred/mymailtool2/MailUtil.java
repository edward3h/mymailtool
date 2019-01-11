package org.ethelred.mymailtool2;

import org.ethelred.util.ClockFactory;

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
                "%tY-%<tm-%<td %<tR: %s", m.getSentDate(), m.getSubject()
        );
    }

    public static void log(String format, Object... args)
    {
        System.out.printf("%tF %<tT ", ClockFactory.getClock().currentTimeMillis());
        System.out.printf(format, _replaceMailInterfaces(args));
        System.out.println();
    }


    private static Object[] _replaceMailInterfaces(Object[] objects)
    {
        for(int i = 0; i < objects.length; i++)
        {
            if(objects[i] instanceof Message)
            {
                try
                {
                    objects[i] = MailUtil.toString((Message) objects[i]);
                }
                catch (MessagingException e)
                {
                    // ignore - will use default representation
                }
            }
        }
        return objects;
    }
}
