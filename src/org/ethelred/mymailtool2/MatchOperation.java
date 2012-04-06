package org.ethelred.mymailtool2;

import com.google.common.base.Predicate;
import javax.mail.Message;

/**
 *
 * @author edward
 */
public class MatchOperation
{
    private final Predicate<Message> match;
    private final MessageOperation operation;

    public MatchOperation(Predicate<Message> match, MessageOperation operation)
    {
        this.match = match;
        this.operation = operation;
    }
    
    void testApply(Message m, MailToolContext ctx)
    {
        if(match.apply(m) && operation.apply(m))
        {
            ctx.countOperation();
        }
         
    }
}
