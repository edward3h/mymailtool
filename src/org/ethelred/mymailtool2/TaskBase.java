package org.ethelred.mymailtool2;

/**
 *
 * @author edward
 */
abstract class TaskBase implements Task
{
    protected MailToolContext context;

    @Override
    public void init(MailToolContext ctx)
    {
        this.context = ctx;
    }
    
}
