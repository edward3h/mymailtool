package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Does the message have a matching User flag?
 */
public class HasFlagMatcher implements Predicate<Message>
{
    private String matchFlag;

    public HasFlagMatcher(String matchFlag)
    {
        this.matchFlag = matchFlag;
    }

    @Override
    public boolean apply(@Nullable Message message)
    {
        if(message == null)
        {
            return false;
        }
        try
        {
            return message.getFlags().contains(matchFlag);
        }
        catch (MessagingException e)
        {
            return false;
        }
    }
}
