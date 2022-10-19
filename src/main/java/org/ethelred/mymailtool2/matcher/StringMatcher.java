package org.ethelred.mymailtool2.matcher;

import com.google.common.base.MoreObjects;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * base matcher for simple strings
 */
public abstract class StringMatcher implements Predicate<Message>
{
    private static final Logger LOGGER = LogManager.getLogger();
    private final Pattern stringPattern;

    protected StringMatcher(String patternSpec)
    {
        stringPattern = Pattern.compile(patternSpec, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public boolean test(Message message)
    {
        try
        {
            CharSequence s = getString(message);
            return s != null && stringPattern.matcher(s).matches();
        }
        catch (MessagingException e)
        {
            LOGGER.warn("Error in matcher", e);
        }
        return false;
    }

    protected abstract CharSequence getString(Message m) throws MessagingException;

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("stringPattern", stringPattern)
                .toString();
    }
}
