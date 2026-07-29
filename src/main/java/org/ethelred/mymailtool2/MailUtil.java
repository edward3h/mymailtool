package org.ethelred.mymailtool2;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.util.Supplier;

/**
 * static utils for mail
 */
public final class MailUtil
{
    /**
     * not instantiable
     */
    private MailUtil() {}

    /**
     * Call sites must pass the result as (or alongside) a lambda/method-reference literal,
     * e.g. {@code () -> MailUtil.supplyString(m).get()}. Passing this call's result directly
     * makes log4j2 pick the fixed-arity {@code info(String, Object)} overload instead of the
     * lazy {@code info(String, Supplier<?>...)} one, since a plain Supplier-typed expression
     * (unlike a lambda literal) is still Object-compatible — this silently logs the Supplier's
     * own toString() instead of invoking it.
     */
    public static Supplier<String> supplyString(Message m) {
        return () -> {
            try {
                return String.format(
                        "@|cyan %tY-%<tm-%<td %<tR|@: @|yellow %s|@", m.getSentDate(), m.getSubject()
                );
            }
            catch (MessagingException e)
            {
                return e.getMessage();
            }
        };
    }
}
