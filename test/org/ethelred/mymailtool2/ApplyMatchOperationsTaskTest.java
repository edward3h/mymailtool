package org.ethelred.mymailtool2;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.ethelred.util.ClockFactory;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.mail.Message;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

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
            public boolean apply(@Nullable Message message)
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

    @After
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
        for(int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertEquals(5, data.folderSize("F1"));
        assertEquals(-1, data.folderSize("F2"));
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age = new AgeMatcher("3 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age), Collections.singletonList(age), new MoveOperation("F2"), false);
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


        assertEquals("F1 size", 3, data.folderSize("F1"));
        assertEquals("F2 size", 2, data.folderSize("F2"));
        assertEquals("messages checked", 3, ((DefaultContext) context).messageCheckedCount);
    }

    @Test
    public void testMultipleShortcut()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for(int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertEquals(5, data.folderSize("F1"));
        assertEquals(-1, data.folderSize("F2"));
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age1 = new AgeMatcher("4 days", true, task);
        Predicate<Message> age2 = new AgeMatcher("2 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age1), Collections.singletonList(age1), new MoveOperation("F2"), false);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age2), Collections.singletonList(age2), new MoveOperation("F3"), false);
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


        assertEquals("F1 size", 2, data.folderSize("F1"));
        assertEquals("F2 size", 1, data.folderSize("F2"));
        assertEquals("F3 size", 2, data.folderSize("F3"));
        assertEquals("messages checked", 4, ((DefaultContext) context).messageCheckedCount);
    }

}
