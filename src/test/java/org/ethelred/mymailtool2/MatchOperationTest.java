package org.ethelred.mymailtool2;

import jakarta.mail.Message;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import java.util.function.Predicate;

/**
 * unit test MatchOperation
 */
public class MatchOperationTest
{
    Mockery context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};

    @Test
    public void testSuccess()
    {
        @SuppressWarnings("unchecked") final Predicate<Message> matcher = context.mock(Predicate.class);
        final MessageOperation operation = context.mock(MessageOperation.class);
        final MailToolContext mailContext = context.mock(MailToolContext.class);
        final Message m = context.mock(Message.class);

        context.checking(new Expectations(){{
            oneOf(matcher).test(m); will(returnValue(true));
            oneOf(operation).apply(mailContext, m); will(returnValue(true));
            allowing(operation).finishApplying(); will(returnValue(true));
            oneOf(mailContext).countOperation();
        }});
        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);
        context.assertIsSatisfied();
    }


    @Test
    public void testOpFailure()
    {
        final Predicate<Message> matcher = context.mock(Predicate.class);
        final MessageOperation operation = context.mock(MessageOperation.class);
        final MailToolContext mailContext = context.mock(MailToolContext.class);
        final Message m = context.mock(Message.class);

        context.checking(new Expectations(){{
            oneOf(matcher).test(m); will(returnValue(true));
            oneOf(operation).apply(mailContext, m); will(returnValue(false));
            allowing(operation).finishApplying(); will(returnValue(true));
        }});
        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);
        context.assertIsSatisfied();
    }

    @Test
    public void testMatchFailure()
    {
        final Predicate<Message> matcher = context.mock(Predicate.class);
        final MessageOperation operation = context.mock(MessageOperation.class);
        final MailToolContext mailContext = context.mock(MailToolContext.class);
        final Message m = context.mock(Message.class);

        context.checking(new Expectations(){{
            oneOf(matcher).test(m); will(returnValue(false));
            allowing(operation).finishApplying(); will(returnValue(true));
        }});
        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);
        context.assertIsSatisfied();
    }
}
