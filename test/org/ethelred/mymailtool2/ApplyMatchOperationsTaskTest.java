package org.ethelred.mymailtool2;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import javax.annotation.Nullable;
import javax.mail.Message;
import java.util.List;

/**
 *
 */
public class ApplyMatchOperationsTaskTest
{

    Mockery context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

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
}
