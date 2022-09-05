package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

/**
 * add/remove a User Flag on a message
 */
public class FlagOperation implements MessageOperation
{
    private boolean add;
    private String userFlag;

    public FlagOperation(boolean add, String userFlag)
    {
        this.add = add;
        this.userFlag = userFlag;
    }

    @Override
    public boolean apply(MailToolContext context, Message m)
    {
        Flags ff = new Flags(userFlag);
        try
        {
            Flags oldFlags = m.getFlags();
            if(add == oldFlags.contains(userFlag))
            {
                // already in correct state
                return false;
            }

            m.setFlags(ff, add);
            return true;
        }
        catch (MessagingException e)
        {
            System.out.printf("Error in %s: %s%n", getClass().getName(), e);
            return false;
        }
    }

    @Override
    public boolean finishApplying()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "Flag(" + (add ? "+" : "-") + userFlag + ")";
    }
}
