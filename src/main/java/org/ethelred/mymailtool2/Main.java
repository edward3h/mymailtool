package org.ethelred.mymailtool2;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author edward
 */
public class Main
{

    private static final Logger LOGGER = LogManager.getLogger();

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
            
            for (String fileLocation : temp.getFileLocations())
            {
                FileConfigurationHelper.loadFileConfiguration(temp, fileLocation);
            }

            validateRequiredConfiguration(temp);
            
            config = temp;
        } catch (CmdLineException ex) {
            LOGGER.error("Argument error", ex);
            parser.printUsage(System.err);
            System.exit(1);
        }
    }

    private synchronized MailToolConfiguration getDefaultConfiguration()
    {
        if (defaultConfiguration == null)
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
        if (mailProperties == null || mailProperties.isEmpty())
        {
            throw new CmdLineException("Missing mail properties - have you set up a config file?");
        }
        Set<String> missingProperties = Sets.newHashSet();
        for (String key : MailToolConfiguration.ALL_MAIL_PROPERTIES)
        {
            if (!mailProperties.containsKey(key))
            {
                missingProperties.add(key);
            }
        }
        if (!missingProperties.isEmpty())
        {
            throw new CmdLineException("Missing mail properties " + Joiner.on(',').join(missingProperties));
        }
    }

    @VisibleForTesting public void run() {
        try {
            if (config.verbose()) {
                setDebugLogging();
            }

            context = new DefaultContext(config);
            context.connect();

            LOGGER.debug("About to get task from config {}", config);
            Task t = config.getTask();
            t.init(context);
            t.run();
            context.logCompletion(null);
        }
        catch (OperationLimitException e)
        {
            context.logCompletion(e);
        }
        catch (Exception e)
        {
            LOGGER.error("Unknown", e);
            
        } finally {
            context.disconnect();
        }

    }

    private void setDebugLogging() {
        var loggerContext = (LoggerContext) LogManager.getContext(false);
        var loggerConfiguration = loggerContext.getConfiguration();
        var root = loggerConfiguration.getRootLogger();
        root.setLevel(Level.DEBUG);
        loggerContext.updateLoggers();
        LOGGER.debug("Verbose logging enabled");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main app = new Main();
        app.init(args);
        app.run();
    }

    private class ShutdownHook implements Runnable
    {
        @Override
        public void run()
        {
            if (context != null)
            {
                context.disconnect();
                context.shutdown();
            }
        }
    }
}
