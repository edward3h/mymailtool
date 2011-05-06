package org.ethelred.mymailtool2;

import com.google.common.base.Predicate;
import javax.mail.Message;

/**
 *
 * @author edward
 */
public interface MessageMatcher extends Predicate<Message>
{
}
