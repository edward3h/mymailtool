package org.ethelred.mymailtool2;

import jakarta.mail.Message;

/**
 *
 * @author edward
 */
public interface MessageOperation
{
    /**
     * perform the operation on the message
     * @param context
     * @param m
     * @return
     */
    boolean apply(MailToolContext context, Message m);

    /**
     * true if no more rules should be applied to the message after this one
     * @return
     */
    boolean finishApplying();
}
