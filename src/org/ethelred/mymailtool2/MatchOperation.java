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
    private final boolean minAgeOnly;

    public boolean isSingleOp()
    {
        return singleOp;
    }

    public void setSingleOp(boolean singleOp)
    {
        this.singleOp = singleOp;
    }

    private boolean singleOp = false;

    public MatchOperation(Predicate<Message> match, MessageOperation operation, int specificity, boolean minAgeOnly)
    {
        this.match = match;
        this.operation = operation;
        this.specificity = specificity;
        this.minAgeOnly = minAgeOnly;
    }
    
    boolean testApply(Message m, MailToolContext ctx)
    {
        if(match.apply(m) && operation.apply(ctx, m))
        {
            System.out.printf("Matched %s and applied %s to message %s%n", match, operation, m);
            ctx.countOperation();
            return operation.finishApplying();
        }

        if(minAgeOnly && singleOp)
        {
            throw new ShortcutFolderScanException();
        }
        return false;
    }

    public int getSpecificity()
    {
        return specificity;
    }

}
