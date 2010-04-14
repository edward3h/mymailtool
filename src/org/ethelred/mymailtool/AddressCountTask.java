/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 *
 * @author edward
 */
abstract class AddressCountTask extends Task
{

    private Map<Address, Integer> counts;

    public AddressCountTask(Properties props, String folderName)
    {
        super(props);
        setFolder(folderName);
        counts = new HashMap<Address, Integer>(100);
    }

    @Override
    protected void storeRun(Store store)
    {
        try
        {
            Folder folder = store.getFolder(getFolder());
            if(folder.exists())
            {
                folder.open(Folder.READ_ONLY);
                for(Message m : folder.getMessages())
                {
                    countMessage(m);
                }
                folder.close(true);

                for(Map.Entry<Address, Integer> e : counts.entrySet())
                {
                    System.out.printf("%d - %s%n", e.getValue(), e.getKey());
                }
            }
            else
            {
                System.out.printf("Folder %s was not found.%n", getFolder());
            }
        }
        catch(MessagingException ex)
        {
            Logger.getLogger(AddressCountTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void countAddress(Address a)
    {
        if(counts.containsKey(a))
        {
            counts.put(a, counts.get(a) + 1);
        }
        else
        {
            counts.put(a, 1);
        }
    }

    protected abstract void countMessage(Message m) throws MessagingException;
}
