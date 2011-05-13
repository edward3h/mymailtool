package org.ethelred.mymailtool2;

import javax.mail.Message;

/**
 *
 * @author edward
 */
class MatchOperation
{
    private final MessageMatcher match;
    private final MessageOperation operation;

    public MatchOperation(MessageMatcher match, MessageOperation operation)
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
