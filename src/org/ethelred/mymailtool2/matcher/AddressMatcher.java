package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
abstract class AddressMatcher implements Predicate<Message>
{

    private final Pattern addressPattern;

    protected AddressMatcher(String patternSpec)
    {
        addressPattern = Pattern.compile(patternSpec);
    }

    public boolean apply(Message t)
    {
        try
        {
            Address[] addresses = getAddresses(t);
            for (Address a : addresses)
            {
                Matcher m = addressPattern.matcher(a.toString());
                if (m.matches())
                {
                    return true;
                }
            }
            return false;
        }
        catch (MessagingException e)
        {
            return false;
        }
    }

    protected abstract Address[] getAddresses(Message t) throws MessagingException;
}
