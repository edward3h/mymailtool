/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
class FromCountTask extends AddressCountTask
{

    public FromCountTask(Properties props, String folderName)
    {
        super(props, folderName);
    }

    @Override
    protected void countMessage(Message m) throws MessagingException
    {
        Address[] from = m.getFrom();
        if(from != null)
        {
            for(Address a : from)
            {
                countAddress(a);
            }
        }
    }
}
