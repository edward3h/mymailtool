package org.ethelred.mymailtool2;

import com.google.common.collect.Maps;
import java.io.Console;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.PeriodFormat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 *
 * @author edward
 */
public class Main implements MailToolContext
{   
    
    private MailToolConfiguration config;
    
    private Session session;
    private Store store;
    
    private int opCount = 0;
    
    protected Map<String, Folder> folderCache;
    
    protected static final ReadablePeriod DEFAULT_MIN_AGE = Days.days(30);
    private DateTime ageCompare;
    
    private void init(String[] args) {
        try {
            CommandLineConfiguration clc = new CommandLineConfiguration();

            CmdLineParser parser = new CmdLineParser(clc);
            parser.parseArgument(args);


            if (clc.isShowUsage()) {
                parser.printUsage(System.err);
                System.exit(1);
            }
            
            SystemPropertiesConfiguration spc = new SystemPropertiesConfiguration();
            
            DefaultConfiguration dc = new DefaultConfiguration();
            
            CompositeConfiguration temp = new CompositeConfiguration(clc, spc, dc);
            
            for(String fileLocation: config.getFileLocations())
            {
                FileConfigurationHelper.loadFileConfiguration(temp, fileLocation);
            }
            
            config = temp;
            
        /*} catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
*/
        } catch (CmdLineException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
       private void run() {
        try {

            connect();
            
            Task t = config.getTask();
            t.init(this);
            t.run();
        }
        catch(Exception e)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            
        } finally {
            disconnect();
        }

    }
       
           private void connect() {
        try {
            session = Session.getDefaultInstance(mapAsProperties(config.getMailProperties()), new MyAuthenticator());
            store = session.getStore();
            store.connect();
            
            folderCache = Maps.newHashMap();

        /*} catch (NoSuchProviderException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);*/
        } catch (MessagingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

        }
    }
           
           
    private void disconnect() {
        if (store != null) {
            try {
                store.close();
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
            p.setProperty(e.getKey(), e.getValue());
        }
        return p;
    }

    public void countOperation()
    {
        if(++opCount > config.getOperationLimit())
        {
            throw new OperationLimitException();
        }
    }

    public boolean isOldEnough(Message m) throws MessagingException {
        DateTime received = new DateTime(m.getReceivedDate());
        return received.isBefore(getAgeCompare());
    }
    
    private DateTime getAgeCompare()
    {
        if(ageCompare != null)
        {
            return ageCompare;
        }
        
        try {
            Period p = PeriodFormat.getDefault().parsePeriod(config.getMinAge());
            ageCompare = new DateTime().minus(p);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            ageCompare = new DateTime().minus(DEFAULT_MIN_AGE);
        }
        
        return ageCompare;
    }

    private class MyAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            String password = config.getPassword();
            if (password == null) {
                password = readPassword();
            }
            return new PasswordAuthentication(config.getUser(), password);
        }

        private String readPassword() {
            Console cons = null;
            if ((cons = System.console()) != null) {
                String password = new String(cons.readPassword("Please enter your password for %s at %s%n", config.getUser(), this.getRequestingSite()));
                return password;
            }
            return null;
        }
    }
    
    public Folder getFolder(String folderName) {
        try {
            Folder result = null;
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
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.init(args);
        app.run();

    }    
}
