package org.ethelred.mymailtool2.matcher;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import org.ethelred.mymailtool2.ShortcutFolderScanException;
import org.ethelred.mymailtool2.Task;
import org.ethelred.util.ClockFactory;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.format.PeriodFormat;

import javax.annotation.CheckForNull;
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
    private final Task task;

    public AgeMatcher(String periodSpec, boolean older, @CheckForNull Task t)
    {
        this.older = older;
        this.comparisonTime = new DateTime(ClockFactory.getClock().currentTimeMillis()).minus(PeriodFormat.getDefault().parsePeriod(periodSpec));
        this.task = t;
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

            if(task == null)
            {
                return older ? received.isBefore(comparisonTime)
                    : received.isAfter(comparisonTime);
            }
            else if((older && !task.orderNewestFirst() && received.isAfter(comparisonTime)) || (!older && task.orderNewestFirst() && received.isBefore(comparisonTime)))
            {
                throw new ShortcutFolderScanException();
            }

        }
        catch (MessagingException e)
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper(this)
                .add("older", older)
                .add("comparisonTime", comparisonTime)
                .toString();
    }

    public boolean isOlder()
    {
        return older;
    }
}
