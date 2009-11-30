/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Address;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.PeriodFormat;

/**
 *
 * @author edward
 */
class ApplyMessageRulesTask extends Task {

    private final static String RULE_PREFIX = "rule.";
    private final static ReadablePeriod DEFAULT_MIN_AGE = Days.days(30);
    private Properties props;
    private List<RuleConfig> rules;
    private int opLimit = 100;
    private Map<String, Folder> folderCache;
    private DateTime ageCompare;

    ApplyMessageRulesTask(Properties p) {
        this.props = p;
        rules = new ArrayList<RuleConfig>();
        folderCache = new HashMap<String, Folder>();
    }

    @Override
    protected void storeRun(Store store) {
        initRules(store);

        String folderName = props.getProperty(Main.FOLDER);
        try {
            Folder folder = store.getFolder(folderName);
            if (folder.exists()) {
                folder.open(Folder.READ_WRITE);
                for (Message m : folder.getMessages()) {
                    if (isOldEnough(m) && !hitLimit()) {
                        for (RuleConfig rule : rules) {
                            if (rule.matches(m)) {
                                rule.apply(m, store, folder);
                                countOperation(rule.type);
                                break;
                            }
                        }
                    } else if(hitLimit()) {
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

    private Folder getFolder(Store store, String folderName)
    {
        try
        {
            Folder result = null;
            //try cache
            result = folderCache.get(folderName);
            if(result != null)
            {
                return result;
            }
            
            //now store
            result = store.getFolder(folderName);
            if(result != null)
            {
                if(!result.exists()) {
                    result.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
                }
                folderCache.put(folderName, result);
                return result;
            }

        }
        catch(MessagingException ex)
        {
            Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void initRules(Store store) {
        String sOpLimit = props.getProperty(RULE_PREFIX + "limit");
        try {
            opLimit = Integer.parseInt(sOpLimit);
        } catch (NumberFormatException e) {
            //ignore
        }

        String sMinAge = props.getProperty(Main.MIN_AGE);
        try {
            Period p = PeriodFormat.getDefault().parsePeriod(sMinAge);
            ageCompare = new DateTime().minus(p);
        } catch(Exception ex) {
            Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
            ageCompare = new DateTime().minus(DEFAULT_MIN_AGE);
        }
        
        for (int i = 1; props.containsKey(RULE_PREFIX + i + ".type"); i++) {
            initRule(i, store);
        }
    }

    private void initRule(int i, Store store) {
        try {
            RuleType type = RuleType.valueOf(props.getProperty(RULE_PREFIX + i + ".type"));
            String addressList = props.getProperty(RULE_PREFIX + i + ".match");
            String moveTo = props.getProperty(RULE_PREFIX + i + ".folder");
            RuleConfig newRule = new RuleConfig();
            newRule.type = type;
            if(addressList != null && "*".equals(addressList.trim())) {
                newRule.any = true;
            } else {
                newRule.addresses = InternetAddress.parse(addressList);
            }
            if (type == RuleType.move) {
                newRule.moveTo = getFolder(store, moveTo);

                if(newRule.moveTo == null) {
                    return; // don't add
                }
            }
            rules.add(newRule);
            System.out.println("Added rule " + newRule);
        } catch (AddressException ex) {
            Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
        } catch (MessagingException ex) {
            Logger.getLogger(FromCountTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean isOldEnough(Message m) throws MessagingException {
        DateTime received = new DateTime(m.getReceivedDate());
        return received.isBefore(ageCompare);
    }

    private boolean hitLimit() {
        return opLimit < 1;
    }

    private void countOperation(RuleType ruleType) {
        opLimit--;
    }

    static enum RuleType {

        move, delete
    }

    static class RuleConfig {

        @Override
        public String toString()
        {
            return String.format("Message Rule: %s %s to %s", type, any ? "*" : Arrays.toString(addresses), moveTo);
        }

        Address[] addresses;
        Folder moveTo;
        RuleType type;
        boolean any = false;

        private boolean matches(Message m) {
            if(any) {
                return true;
            }
            try {
                Address[] from = m.getFrom();
                if (from.length != 1) {
                    return false;
                }

                for (Address a : addresses) {
                    if (from[0].equals(a)) {
                        return true;
                    }
                }
            } catch (MessagingException ex) {
                Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);
            }
            return false;
        }

        private void apply(Message m, Store store, Folder current) {
            try {
                if (type == RuleType.move) {

                    current.copyMessages(new Message[]{m}, moveTo);
System.out.printf("Moving message %s to %s%n", m, moveTo);
                }
                if (type == RuleType.move || type == RuleType.delete) {
                    m.setFlag(Flag.DELETED, true);
if(type == RuleType.delete) {
    System.out.printf("Deleting message %s %n", m);
}
                }
            } catch (MessagingException ex) {
                Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);

            }
        }
    }
}
