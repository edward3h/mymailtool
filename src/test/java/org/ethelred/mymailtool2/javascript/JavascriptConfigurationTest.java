package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.net.URL;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * test loading a js file
 */
public class JavascriptConfigurationTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }


    @Test
    public void testLoadJavascript() throws Exception
    {
        FileConfigurationHandler h = new JavascriptFileConfigurationHandler();

        URL testFileLocation = this.getClass().getResource("testjavascript.js");
        assertThat(testFileLocation).isNotNull();
        File f = new File(testFileLocation.getFile());

        MailToolConfiguration conf = h.readConfiguration(f);

        assertThat(conf).isNotNull();
        assertThat(conf.getUser()).isEqualTo("edward");
        assertThat(conf.getMinAge()).isEqualTo("3 months");
        assertThat(conf.getOperationLimit()).isEqualTo(300);
        assertThat(conf.getMailProperties().get("mail.store.protocol")).isEqualTo("imap");

        Task t = conf.getTask();
        assertThat(t).isNotNull();
        assertThat(t).isInstanceOf(ApplyMatchOperationsTask.class);
    }

    @Test
    public void testJSMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new JavascriptFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testjavascript.js").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world world"));
        data.addMessage("spamtest", MockMessage.create("2012-01-01", "from1@example.com", "Hello world").addHeader("X-Spam-Score", "1.5"));
        data.addMessage("spamtest", MockMessage.create("2012-01-01", "from1@example.com", "Hello world 2").addHeader("X-Spam-Score", "2.6"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(-1);
        assertThat(data.folderSize("test")).isEqualTo(2);
        assertThat(data.folderSize("repeated")).isEqualTo(-1);
        assertThat(data.folderSize("spamtest")).isEqualTo(2);
        assertThat(data.folderSize("spamscore")).isEqualTo(-1);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(0);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(1);
        assertThat(data.folderSize("test")).isEqualTo(1);
        assertThat(data.folderSize("repeated")).isEqualTo(1);
        assertThat(data.folderSize("spamtest")).isEqualTo(1);
        assertThat(data.folderSize("spamscore")).isEqualTo(1);
    }



    @Test
    public void testOverloaded() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new JavascriptFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("overloaded.js").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "banana@fruit.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "cheddar@cheese.com", "Hello subject2 world"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(1);
    }
}
