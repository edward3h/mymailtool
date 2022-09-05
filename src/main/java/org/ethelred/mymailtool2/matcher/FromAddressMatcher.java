package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

/**
 *
 * @author edward
 */
public class FromAddressMatcher extends AddressMatcher
{

    public FromAddressMatcher(boolean bLiteral, String patternSpec, String... morePatterns)
    {
        super(bLiteral, patternSpec, morePatterns);
    }

    @Override
    protected Address[] getAddresses(Message t) throws MessagingException
    {
        return t.getFrom();
    }
    
}
