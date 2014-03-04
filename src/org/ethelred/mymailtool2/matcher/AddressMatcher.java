package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
abstract class AddressMatcher implements Predicate<Message>
{

    private static final Address[] EMPTY_ADDRESSES = new Address[0];
    private final boolean bLiteral;
    private final Iterable<Pattern> addressPatterns;

    private final Cache<Message, Address[]> addressCache =
            CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Message, Address[]>()
            {
                @Override
                public Address[] load(Message message) throws Exception
                {
                    Address[] r = getAddresses(message);
                    return r == null ? EMPTY_ADDRESSES : r;
                }
            });

    protected AddressMatcher(boolean bLiteral, String patternSpec, String... morePatterns)
    {
        this.bLiteral = bLiteral;
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
            Address[] addresses = addressCache.get(t);
            if(addresses == null)
            {
                return false;
            }
            for (Address a : addresses)
            {
                for(Pattern addressPattern: addressPatterns)
                {
                    Matcher m = addressPattern.matcher(a.toString());
                    if ((bLiteral && m.find()) || (!bLiteral && m.matches()))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
//        catch (MessagingException e)
//        {
//            return false;
//        }
        catch (ExecutionException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Exception in getAddresses", e);
            return false;
        }
    }

    protected abstract @CheckForNull Address[] getAddresses(Message t) throws MessagingException;

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("addressPatterns", Joiner.on("|").join(addressPatterns))
                .toString();
    }
}
