package org.ethelred.mymailtool2;

import javax.mail.Message;

/**
 *
 * @author edward
 */
public interface MessageOperation
{
    boolean apply(MailToolContext context, Message m);
}
