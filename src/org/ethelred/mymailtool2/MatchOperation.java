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
        ctx.debugF("Checking %s%n", m);
        if(match.apply(m) && operation.apply(ctx, m))
        {
            ctx.debugF("Matched %s and applied %s to message %s%n", match, operation, m);
            ctx.countOperation();
            return operation.finishApplying();
        }
        return false;
    }

    public int getSpecificity()
    {
        return specificity;
    }

    @Override
    public String toString()
    {
        return operation.toString() + specificity + '[' + match + ']';
    }

}
