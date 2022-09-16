package org.ethelred.mymailtool2;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.ethelred.util.function.CheckedSupplier;

import java.util.function.Supplier;

/**
 * static utils for mail
 */
public class MailUtil
{
    /**
     * not instantiable
     */
    private MailUtil(){}

    public static Supplier<String> supplyString(Message m) throws MessagingException
    {
        return CheckedSupplier.unchecked(() -> String.format(
                "%tY-%<tm-%<td %<tR: %s", m.getSentDate(), m.getSubject()
        ));
    }
}
