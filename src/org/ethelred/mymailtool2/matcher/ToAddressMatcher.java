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

    public ToAddressMatcher(String patternSpec)
    {
        super(patternSpec);
    }

    @Override
    protected Address[] getAddresses(Message t) throws MessagingException
    {
        return t.getAllRecipients();
    }
    
}
