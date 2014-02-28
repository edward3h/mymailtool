package org.ethelred.mymailtool2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import java.io.Console;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.*;
import javax.mail.Authenticator;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

import com.google.common.collect.Sets;
import org.ethelred.util.ClockFactory;
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
public class Main
{   

    @VisibleForTesting
    MailToolConfiguration config;

    private MailToolContext context;
    private volatile MailToolConfiguration defaultConfiguration;

    @VisibleForTesting public void init(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
        CommandLineConfiguration clc = new CommandLineConfiguration();
        CmdLineParser parser = new CmdLineParser(clc);
        try {

            parser.parseArgument(args);


            if (clc.isShowUsage()) {
                parser.printUsage(System.err);
                System.exit(1);
            }
            
            SystemPropertiesConfiguration spc = new SystemPropertiesConfiguration();
            
            MailToolConfiguration dc = getDefaultConfiguration();
            
            CompositeConfiguration temp = new CompositeConfiguration(clc, spc, dc);
            
            for(String fileLocation: temp.getFileLocations())
            {
                FileConfigurationHelper.loadFileConfiguration(temp, fileLocation);
            }

            validateRequiredConfiguration(temp);
            
            config = temp;
            
        /*} catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
*/
        } catch (CmdLineException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            parser.printUsage(System.err);
            System.exit(1);
        }
    }

    private synchronized MailToolConfiguration getDefaultConfiguration()
    {
        if(defaultConfiguration == null)
        {
            defaultConfiguration = new DefaultConfiguration();
        }
        return defaultConfiguration;
    }

    @VisibleForTesting public synchronized void setDefaultConfiguration(MailToolConfiguration configuration)
    {
        defaultConfiguration = configuration;
    }

    private void validateRequiredConfiguration(MailToolConfiguration configuration) throws CmdLineException
    {
        Map<String, String> mailProperties = configuration.getMailProperties();
        if(mailProperties == null || mailProperties.isEmpty())
        {
            throw new CmdLineException("Missing mail properties - have you set up a config file?");
        }
        Set<String> missingProperties = Sets.newHashSet();
        for(String key: MailToolConfiguration.ALL_MAIL_PROPERTIES)
        {
            if(!mailProperties.containsKey(key))
            {
                missingProperties.add(key);
            }
        }
        if(!missingProperties.isEmpty())
        {
            throw new CmdLineException("Missing mail properties " + Joiner.on(',').join(missingProperties));
        }
    }

    @VisibleForTesting public void run() {
        try {

            context = new DefaultContext(config);
            context.connect();

            System.out.println("About to get task from config " + config);
            Task t = config.getTask();
            t.init(context);
            t.run();
        }
        catch(OperationLimitException e)
        {
            //Logger.getLogger(Main.class.getName()).log(Level.INFO, e.toString());
            System.out.println(e.toString());
        }
        catch(Exception e)
        {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, e);
            
        } finally {
            context.disconnect();
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        Main app = new Main();
        app.init(args);
        app.run();

    }

    private class ShutdownHook implements Runnable
    {
        @Override
        public void run()
        {
            if(context != null)
            {
                context.disconnect();
                context.shutdown();
            }
        }
    }
}
