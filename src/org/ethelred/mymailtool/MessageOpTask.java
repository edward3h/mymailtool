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
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.PeriodFormat;

/**
 *
 * @author edward
 */
abstract class MessageOpTask extends Task {

    protected static final ReadablePeriod DEFAULT_MIN_AGE = Days.days(30);
    protected DateTime ageCompare;
    protected Map<String, Folder> folderCache;
    protected int opLimit = 100;

    protected MessageOpTask(Properties p) {
        super(p);
        folderCache = new HashMap<String, Folder>();
    }

    protected void countOperation() {
        opLimit--;
    }

    protected Folder getFolder(Store store, String folderName) {
        try {
            Folder result;
            //try cache
            result = folderCache.get(folderName);
            if (result != null) {
                return result;
            }
            //now store
            result = store.getFolder(folderName);
            if (result != null) {
                if (!result.exists()) {
                    result.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
                }
                folderCache.put(folderName, result);
                return result;
            }
        } catch (MessagingException ex) {
            Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected boolean hitLimit() {
        return opLimit < 1;
    }

    protected boolean isOldEnough(Message m) throws MessagingException {
        DateTime received = new DateTime(m.getReceivedDate());
        return received.isBefore(ageCompare);
    }

    protected void init(Store store) {
        String sOpLimit = props.getProperty(getPrefix() + "limit");
        try {
            opLimit = Integer.parseInt(sOpLimit);
        } catch (NumberFormatException e) {
            //ignore
        }

        String sMinAge = props.getProperty(Main.MIN_AGE);
        try {
            Period p = PeriodFormat.getDefault().parsePeriod(sMinAge);
            ageCompare = new DateTime().minus(p);
        } catch (Exception ex) {
            Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
            ageCompare = new DateTime().minus(DEFAULT_MIN_AGE);
        }


    }

    @Override
    protected void storeRun(Store store) {
        init(store);
        String folderName = getFolder();
        try {
            Folder folder = store.getFolder(folderName);
            if (folder.exists()) {
                folder.open(Folder.READ_WRITE);
                for (Message m : folder.getMessages()) {
                    if (isOldEnough(m) && !hitLimit()) {
                        if (processMessage(m, store, folder)) {
                            countOperation();
                        }

                    } else if (hitLimit()) {
                        break;
                    }
                }
                folder.close(true);
            } else {
                System.out.printf("Folder %s was not found.%n", folderName);
            }
        } catch (MessagingException ex) {
            Logger.getLogger(FromCountTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected abstract String getPrefix();

    protected abstract boolean processMessage(Message m, Store store, Folder folder);
}
