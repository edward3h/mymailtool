package org.ethelred.mymailtool2;

import jakarta.mail.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;

/**
 *
 * @author edward
 */
public class MatchOperation
{
    private static final Logger LOGGER = LogManager.getLogger();
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
        if (match.test(m) && operation.apply(ctx, m))
        {
            LOGGER.debug("Matched {} and applied {} to message {}", match, operation, m);
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
