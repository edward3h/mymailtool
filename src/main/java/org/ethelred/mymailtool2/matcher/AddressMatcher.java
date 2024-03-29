package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author edward
 */
abstract class AddressMatcher implements Predicate<Message>
{

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Address[] EMPTY_ADDRESSES = new Address[0];
    private final boolean bLiteral;
    private final Iterable<Pattern> addressPatterns;

    private final LoadingCache<Message, Address[]> addressCache =
            CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<>() {
                @Override @Nonnull
                public Address[] load(@Nonnull Message message) throws Exception {
                    Address[] r = getAddresses(message);
                    return r == null ? EMPTY_ADDRESSES : r;
                }
            });

    protected AddressMatcher(boolean bLiteral, String patternSpec, String... morePatterns)
    {
        this.bLiteral = bLiteral;
        int nFlags = Pattern.CASE_INSENSITIVE;
        if (bLiteral)
        {
            nFlags = nFlags | Pattern.LITERAL;
        }
        List<Pattern> patterns = Lists.newArrayListWithCapacity(morePatterns.length + 1);
        patterns.add(Pattern.compile(patternSpec, nFlags));
        for (String pattern : morePatterns)
        {
            patterns.add(Pattern.compile(pattern, nFlags));
        }
        addressPatterns = ImmutableList.copyOf(patterns);
    }

    @Override
    public boolean test(Message t)
    {
        try
        {
            Address[] addresses = addressCache.get(t);
            for (Address a : addresses)
            {
                for (Pattern addressPattern : addressPatterns)
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
        catch (ExecutionException e)
        {
            LOGGER.error("Exception in getAddresses", e);
            return false;
        }
    }

    protected abstract @CheckForNull Address[] getAddresses(Message t) throws MessagingException;

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("addressPatterns", Joiner.on("|").join(addressPatterns))
                .toString();
    }
}
