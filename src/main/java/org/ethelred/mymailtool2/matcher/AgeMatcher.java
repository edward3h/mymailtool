package org.ethelred.mymailtool2.matcher;

import com.google.common.base.MoreObjects;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.ethelred.mymailtool2.ShortcutFolderScanException;
import org.ethelred.mymailtool2.Task;
import org.ethelred.util.ClockFactory;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * matches against the age of the message
 */
public class AgeMatcher implements Predicate<Message>
{
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)\\s*(second|minute|hour|day|week)s?", Pattern.CASE_INSENSITIVE);

    private final boolean older;
    private final Instant comparisonTime;
    private final Task task;

    public AgeMatcher(String periodSpec, boolean older, @CheckForNull Task t)
    {
        this.older = older;
        this.comparisonTime = Instant.ofEpochMilli(ClockFactory.getClock().currentTimeMillis()).minus(parseDuration(periodSpec));
        this.task = t;
    }

    private static Duration parseDuration(String spec) {
        try {
            return Duration.parse(spec);
        } catch (DateTimeParseException ignored) {}
        Matcher m = DURATION_PART.matcher(spec);
        Duration total = Duration.ZERO;
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            total = total.plus(switch (m.group(2).toLowerCase()) {
                case "second" -> Duration.ofSeconds(n);
                case "minute" -> Duration.ofMinutes(n);
                case "hour" -> Duration.ofHours(n);
                case "day" -> Duration.ofDays(n);
                case "week" -> Duration.ofDays(n * 7);
                default -> Duration.ZERO;
            });
        }
        return total;
    }

    @Override
    public boolean test(@Nullable Message message)
    {
        if (message == null)
        {
            return false;
        }

        try
        {
            Instant received = message.getReceivedDate().toInstant();

            if (task == null)
            {
                return older ? received.isBefore(comparisonTime)
                    : received.isAfter(comparisonTime);
            }
            else if ((older && !task.orderNewestFirst() && received.isAfter(comparisonTime)) || (!older && task.orderNewestFirst() && received.isBefore(comparisonTime)))
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

}
