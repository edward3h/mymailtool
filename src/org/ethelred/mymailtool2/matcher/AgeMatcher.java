package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import org.ethelred.util.ClockFactory;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.format.PeriodFormat;

import javax.annotation.Nullable;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * matches against the age of the message
 */
public class AgeMatcher implements Predicate<Message>
{
    private final boolean older;
    private final DateTime comparisonTime;

    public AgeMatcher(String periodSpec, boolean older)
    {
        this.older = older;
        this.comparisonTime = new DateTime(ClockFactory.getClock().currentTimeMillis()).minus(PeriodFormat.getDefault().parsePeriod(periodSpec));
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
            DateTime received = new DateTime(message.getReceivedDate());
            return older ? received.isBefore(comparisonTime)
                    : received.isAfter(comparisonTime);
        }
        catch (MessagingException e)
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("older", older)
                .add("comparisonTime", comparisonTime)
                .toString();
    }
}
