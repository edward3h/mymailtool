package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
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

    private final Iterable<Pattern> addressPatterns;

    protected AddressMatcher(boolean bLiteral, String patternSpec, String... morePatterns)
    {
        int nFlags = Pattern.CASE_INSENSITIVE;
        if(bLiteral)
        {
            nFlags = nFlags | Pattern.LITERAL;
        }
        List<Pattern> patterns = Lists.newArrayListWithCapacity(morePatterns.length + 1);
        patterns.add(Pattern.compile(patternSpec, nFlags));
        for(String pattern: morePatterns)
        {
            patterns.add(Pattern.compile(pattern, nFlags));
        }
        addressPatterns = ImmutableList.copyOf(patterns);
    }

    @Override
    public boolean apply(Message t)
    {
        try
        {
            Address[] addresses = getAddresses(t);
            for (Address a : addresses)
            {
                for(Pattern addressPattern: addressPatterns)
                {
                    Matcher m = addressPattern.matcher(a.toString());
                    if (m.matches())
                    {
                        return true;
                    }
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
