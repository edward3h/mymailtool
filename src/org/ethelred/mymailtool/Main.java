/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ethelred.mymailtool;

import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 *
 * @author edward
 */
public class Main {

    private final static String PROTOCOL = "mail.store.protocol";
    private final static String USER = "mail.user";
    private final static String PASSWORD = "mymailtool.password";
    private final static String HOST = "mail.host";
    private final static String PORT = "mail.port";
    final static String FOLDER = "mymailtool.rules.folder";
    final static String MIN_AGE = "mymailtool.minage";
    private Properties props;
    private Queue<Task> taskQueue;
    private Session session;
    private Store store;
    @Option(name = "--help", usage = "Show help.", aliases = {"-h", "-?"})
    private boolean showUsage = false;

    @Option(name = "--list", usage = "List folders.", aliases = {"-l"})
    private void taskListFolders(boolean fake) {
        taskQueue.offer(new ListFoldersTask());
    }

    @Option(name = "--fromCount", usage = "Count occurrences of from addresses", aliases = {"-f"})
    private void taskFromCount(String folderName) {
        taskQueue.offer(new FromCountTask(folderName));
    }

    @Option(name = "--apply", usage = "Apply rules - define rules in config file", aliases = {"-a"})
    private void taskApplyRules(String folderName) {
        props.setProperty(FOLDER, folderName);
        taskQueue.offer(new ApplyMessageRulesTask(props));
    }

    @Option(name = "--month-split", usage = "Split folder into monthly subfolders", aliases = {"-m"})
    private void taskSplitByMonth(String folderName) {
        props.setProperty(FOLDER, folderName);
        taskQueue.offer(new DateSplitTask(props));
    }

    @Option(name = "--host", usage = "mail hostname", aliases = {"-H"})
    private void setHost(String hostname) {
        props.setProperty(HOST, hostname);
    }

    @Option(name = "--port", usage = "mail port. Default 143", aliases = {"-p"})
    private void setPort(int port) {
        props.setProperty(PORT, String.valueOf(port));
    }

    @Option(name = "--user", usage = "mail user", aliases = {"-u"})
    private void setUser(String username) {
        props.setProperty(USER, username);
    }

    @Option(name = "--min-age", usage = "Minimum age of mail to process", aliases = {"-t"})
    private void setMinAge(String minAge) {
        props.setProperty(MIN_AGE, minAge);
    }

    private Main() {
        props = new Properties();
        props.setProperty(PROTOCOL, "imap");//default to IMAP
        taskQueue = new LinkedList<Task>();
    }

    private void init(String[] args) {
        try {
            File conffile = new File(System.getProperty("user.home"), ".mymailtoolrc");
            if (conffile.canRead()) {
                props.load(new FileReader(conffile));

            }

            CmdLineParser parser = new CmdLineParser(this);
            parser.parseArgument(args);

            if (showUsage || taskQueue.isEmpty()) {
                parser.printUsage(System.err);
                System.exit(1);
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);

        } catch (CmdLineException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void run() {
        try {

            connect();
            for (Task t : taskQueue) {
                t.setStore(store);
                t.run();
            }
        } finally {
            disconnect();
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

    private void connect() {
        try {
            session = Session.getDefaultInstance(props, new MyAuthenticator());
            store = session.getStore();
            store.connect();

        } catch (NoSuchProviderException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
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

    private class MyAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            String password = props.getProperty(PASSWORD);
            if (password == null) {
                password = readPassword();
            }
            return new PasswordAuthentication(props.getProperty(USER), password);
        }

        private String readPassword() {
            Console cons = null;
            if ((cons = System.console()) != null) {
                String password = new String(cons.readPassword("Please enter your password for %s at %s%n", props.getProperty(USER), this.getRequestingSite()));
                return password;
            }
            return null;
        }
    }
}
