package org.ethelred.mymailtool2;

/**
 *
 * @author edward
 */
public interface Task
{

    void run();

    void init(MailToolContext ctx);

    boolean orderNewestFirst();
}
