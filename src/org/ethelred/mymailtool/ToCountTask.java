/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.Map;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
class ToCountTask extends AddressCountTask
{
    public ToCountTask(Properties props, String folderName)
    {
        super(props, folderName);
    }

    @Override
    protected void countMessage(Message m) throws MessagingException
    {
        Address[] to = m.getAllRecipients();
        if(to != null)
        {
            for(Address a : to)
            {
                countAddress(a);
            }
        }
    }
}
