package org.ethelred.mymailtool2.matcher;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;

import com.google.common.base.Predicate;

/**
 * base matcher for simple strings
 */
public abstract class StringMatcher implements Predicate<Message>
{
    private final Pattern stringPattern;

    protected StringMatcher(String patternSpec)
    {
        stringPattern = Pattern.compile(patternSpec, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean apply(Message message)
    {
        try
        {
            CharSequence s = getString(message);
            return s != null && stringPattern.matcher(s).matches();
        }
        catch(MessagingException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error in matcher", e);
        }
        return false;
    }

    protected abstract CharSequence getString(Message m) throws MessagingException;
}
