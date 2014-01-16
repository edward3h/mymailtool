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

    private final int specificity;

    public MatchOperation(Predicate<Message> match, MessageOperation operation, int specificity)
    {
        this.match = match;
        this.operation = operation;
        this.specificity = specificity;
    }
    
    boolean testApply(Message m, MailToolContext ctx)
    {
        if(match.apply(m) && operation.apply(ctx, m))
        {
            ctx.countOperation();
            return operation.finishApplying();
        }
        return false;
    }

    public int getSpecificity()
    {
        return specificity;
    }

}
