package org.ethelred.mymailtool2.matcher;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
public class ToAddressMatcher extends AddressMatcher
{

    public ToAddressMatcher(boolean bLiteral, String patternSpec, String... morePatterns)
    {
        super(bLiteral, patternSpec, morePatterns);
    }

    @Override
    protected Address[] getAddresses(Message t) throws MessagingException
    {
        return t.getAllRecipients();
    }
    
}
