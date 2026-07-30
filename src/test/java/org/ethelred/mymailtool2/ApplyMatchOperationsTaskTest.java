package org.ethelred.mymailtool2;

import java.util.function.Predicate;
import org.ethelred.util.Predicates;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.ethelred.util.ClockFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import jakarta.mail.Message;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 *
 */
public class ApplyMatchOperationsTaskTest
{

    @Test
    public void testGlobalMinAge()
    {
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        TrackMatchMessageOperation msgop = new TrackMatchMessageOperation();
        MatchOperation mo = new MatchOperation(new Predicate<Message>()
        {
            @Override
            public boolean test(@Nullable Message message)
            {
                return true;
            }
        }, msgop, 0);

        //task.addRule("INBOX", mo, false);
    }

    private class TrackMatchMessageOperation implements MessageOperation
    {
        private List<Message> matches = Lists.newArrayList();

        @Override
        public boolean apply(MailToolContext context, Message m)
        {
            matches.add(m);
            return true;
        }

        @Override
        public boolean finishApplying()
        {
            return true;
        }
    }

    @AfterEach
    public void reset()
    {
        MockData.clear();
    }

    @Test
    public void testSimpleShortcut()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertThat(data.folderSize("F1")).isEqualTo(5);
        assertThat(data.folderSize("F2")).isEqualTo(-1);
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age = new AgeMatcher("3 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age), List.of(age), new MoveOperation("F2"), false);
        MailToolContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }


        assertThat(data.folderSize("F1")).isEqualTo(3);
        assertThat(data.folderSize("F2")).isEqualTo(2);
        assertThat(((DefaultContext) context).messageCheckedCount).isEqualTo(3);
    }

    @Test
    public void testMultipleShortcut()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertThat(data.folderSize("F1")).isEqualTo(5);
        assertThat(data.folderSize("F2")).isEqualTo(-1);
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age1 = new AgeMatcher("4 days", true, task);
        Predicate<Message> age2 = new AgeMatcher("2 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age1), List.of(age1), new MoveOperation("F2"), false);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age2), List.of(age2), new MoveOperation("F3"), false);
        MailToolContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }


        assertThat(data.folderSize("F1")).isEqualTo(2);
        assertThat(data.folderSize("F2")).isEqualTo(1);
        assertThat(data.folderSize("F3")).isEqualTo(2);
        assertThat(((DefaultContext) context).messageCheckedCount).isEqualTo(4);
    }

}
