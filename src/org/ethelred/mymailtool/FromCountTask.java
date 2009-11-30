/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.ethelred.mymailtool;

import java.util.HashMap;
import java.util.Map;
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
class FromCountTask extends Task {
    private String folderName;
    private Map<Address, Integer> counts;

    public FromCountTask(String folderName) {
        this.folderName = folderName;
        counts = new HashMap<Address, Integer>(100);
    }

    @Override
    protected void storeRun(Store store) {
        try {
            Folder folder = store.getFolder(folderName);
            if(folder.exists()) {
                folder.open(Folder.READ_ONLY);
                for(Message m: folder.getMessages()) {
                    Address[] from = m.getFrom();
                    if(from != null) {
                        for(Address a: from) {
                            countAddress(a);
                        }
                    }
                }
                folder.close(true);

                for(Map.Entry<Address, Integer> e: counts.entrySet()) {
                    System.out.printf("%d - %s%n", e.getValue(), e.getKey());
                }
            } else {
                System.out.printf("Folder %s was not found.%n", folderName);
            }
        } catch (MessagingException ex) {
            Logger.getLogger(FromCountTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void countAddress(Address a) {
        if(counts.containsKey(a)) {
            counts.put(a, counts.get(a) + 1);
        } else {
            counts.put(a, 1);
        }
    }


}
