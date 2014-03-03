package org.ethelred.mymailtool2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.ethelred.util.ClockFactory;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.PeriodFormat;

import javax.annotation.CheckForNull;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import java.io.Console;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DefaultContext implements MailToolContext
{

    private final MailToolConfiguration config;

    private Store store;

    private int opCount = 0;

    protected Map<String, Folder> folderCache;

    protected static final ReadablePeriod DEFAULT_MIN_AGE = Days.days(30);
    private DateTime ageCompare;
    private long startTime = ClockFactory.getClock().currentTimeMillis();
    private long timeLimit = -1;
    private int operationLimit = -1;
    private volatile boolean shutdown = false;

    private int messageCheckedCount = 0;

    public DefaultContext(MailToolConfiguration config)
    {
        this.config = config;
    }

    @Override
    public synchronized void connect() {
        try {
            Session session = Session.getDefaultInstance(mapAsProperties(config.getMailProperties()), new MyAuthenticator());
            store = session.getStore();
            store.connect();

            startTime = ClockFactory.getClock().currentTimeMillis();

            folderCache = Maps.newHashMap();
            System.out.printf("Connected to %s%n", config.getMailProperties().get(MailToolConfiguration.HOST));

        /*} catch (NoSuchProviderException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);*/
        } catch (MessagingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

        }
    }


    @Override
    public synchronized void disconnect() {
        if (store != null) {
            try {
                store.close();
                store = null;
                System.out.printf("Disconnected%n");
            } catch (MessagingException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private Properties mapAsProperties(Map<String, String> mailProperties)
    {
        Properties p = new Properties();
        for(Map.Entry<String, String> e: mailProperties.entrySet())
        {
            System.out.printf("Mail property: %s => %s%n", e.getKey(), e.getValue());
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    @Override
    public void countOperation()
    {
        if(++opCount > getOperationLimit())
        {
            throw new OperationLimitException(String.format("Hit operation limit %s (ops %s)", config.getOperationLimit(), opCount));
        }

        if(getTimeLimit() > 0 && startTime > 0 && (ClockFactory.getClock().currentTimeMillis() - startTime) > getTimeLimit())
        {
            throw new OperationLimitException(String.format("Hit time limit %s (ops %s)", config.getTimeLimit(), opCount));
        }

        if(shutdown)
        {
            throw new RuntimeException("Shutdown");
        }

    }

    private int getOperationLimit()
    {
        if(operationLimit > -1)
        {
            return operationLimit;
        }

        System.out.printf("getOperationLimit%n");
        int result = config.getOperationLimit();
        System.out.printf("Operation limit %s%n", result);
        operationLimit = result;
        return operationLimit;
    }

    private long getTimeLimit()
    {
        if(timeLimit > -1)
        {
            return timeLimit;
        }
        String timeLimitSpec = config.getTimeLimit();
        long newTimeLimit = 0;
        if(!(Strings.isNullOrEmpty(timeLimitSpec)))
        {
            Period p = PeriodFormat.getDefault().parsePeriod(timeLimitSpec);
            newTimeLimit = p.toStandardDuration().getMillis();
        }
        System.out.printf("Time limit %sms%n", newTimeLimit);
        timeLimit = newTimeLimit;
        return timeLimit;
    }

    @Override
    public boolean isOldEnough(Message m) throws MessagingException {
        DateTime received = new DateTime(m.getReceivedDate());
        return received.isBefore(getAgeCompare());
    }

    @VisibleForTesting
    DateTime getAgeCompare()
    {
        if(ageCompare != null)
        {
            return ageCompare;
        }

        try {
            Period p = PeriodFormat.getDefault().parsePeriod(config.getMinAge());
            ageCompare = new DateTime(startTime).minus(p);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            ageCompare = new DateTime(startTime).minus(DEFAULT_MIN_AGE);
        }

        return ageCompare;
    }

    private class MyAuthenticator extends Authenticator
    {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            String password = config.getPassword();
            if (password == null) {
                password = readPassword();
            }
            return new PasswordAuthentication(config.getUser(), password);
        }

        private String readPassword() {
            Console cons;
            if ((cons = System.console()) != null) {
                return new String(cons.readPassword("Please enter your password for %s at %s%n", config.getUser(), this.getRequestingSite()));
            }
            return null;
        }
    }

    @Override
    public Folder getFolder(String folderName) {
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
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public Folder getDefaultFolder()
    {
        try
        {
            return store.getDefaultFolder();
        }
        catch (MessagingException e)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
        }
        return null;
    }

    @Override
    public void shutdown()
    {
        shutdown = true;
    }

    @Override
    public void logCompletion(@CheckForNull OperationLimitException e)
    {
        System.out.printf("Checked %d messages, performed %d operations. %s. %n",
                          messageCheckedCount,
                          opCount,
                          e == null ? "Finished successfully." : e.toString());
    }

    @Override
    public void countMessage()
    {
        messageCheckedCount++;
    }
}
