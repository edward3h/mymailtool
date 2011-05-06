package org.ethelred.mymailtool2;

import java.io.Console;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
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
    
    private void init(String[] args) {
        try {
            CommandLineConfiguration clc = new CommandLineConfiguration();

            CmdLineParser parser = new CmdLineParser(clc);
            parser.parseArgument(args);


            if (clc.isShowUsage()) {
                parser.printUsage(System.err);
                System.exit(1);
            }
            
            DefaultConfiguration dc = new DefaultConfiguration();
            
            CompositeConfiguration temp = new CompositeConfiguration(clc, dc);
            
            for(String fileLocation: config.getFileLocations())
            {
                MailToolConfiguration fileConfig = loadFileConfiguration(fileLocation);
                temp.insert(fileConfig);
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
            
        } finally {
            disconnect();
        }

    }
       
           private void connect() {
        try {
            session = Session.getDefaultInstance(mapAsProperties(config.getMailProperties()), new MyAuthenticator());
            store = session.getStore();
            store.connect();

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

    private MailToolConfiguration loadFileConfiguration(String fileLocation)
    {
        throw new UnsupportedOperationException("Not yet implemented");
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
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.init(args);
        app.run();

    }    
}
