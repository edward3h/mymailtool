package org.ethelred.mymailtool2.matcher;

import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * match against a message subject
 */
public class SubjectMatcher extends StringMatcher
{
    public SubjectMatcher(String patternSpec)
    {
        super(patternSpec);
    }

    @Override
    protected CharSequence getString(Message m) throws MessagingException
    {
        return m.getSubject();
    }
}
