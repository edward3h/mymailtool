/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

/**
 *
 * @author edward
 */
class ApplyMessageRulesTask extends MessageOpTask {

    private final static String RULE_PREFIX = "rule.";
    private List<RuleConfig> rules;

    protected ApplyMessageRulesTask(Properties p) {
        super(p);
        rules = new ArrayList<RuleConfig>();
    }

    @Override
    protected void init(Store store) {
        super.init(store);

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
            if (addressList != null && "*".equals(addressList.trim())) {
                newRule.any = true;
            } else {
                newRule.addresses = InternetAddress.parse(addressList);
            }
            if (type == RuleType.move) {
                newRule.moveTo = getFolder(store, moveTo);

                if (newRule.moveTo == null) {
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

    @Override
    protected boolean processMessage(Message m, Store store, Folder folder) {
        for (RuleConfig rule : rules) {
            if (rule.matches(m)) {
                rule.apply(m, store, folder);
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getPrefix() {
        return RULE_PREFIX;
    }

    static enum RuleType {

        move, delete
    }

    static class RuleConfig {

        @Override
        public String toString() {
            return String.format("Message Rule: %s %s to %s", type, any ? "*" : Arrays.toString(addresses), moveTo);
        }
        Address[] addresses;
        Folder moveTo;
        RuleType type;
        boolean any = false;

        private boolean matches(Message m) {
            if (any) {
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
                    if (type == RuleType.delete) {
                        System.out.printf("Deleting message %s %n", m);
                    }
                }
            } catch (MessagingException ex) {
                Logger.getLogger(ApplyMessageRulesTask.class.getName()).log(Level.SEVERE, null, ex);

            }
        }
    }
}
