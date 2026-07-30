package org.ethelred.mymailtool2.propertiesfile;

import java.io.File;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 *
 */
public class PropertiesFileConfigurationTest
{
    File confFile;

    @BeforeEach
    public void setup()
    {
        confFile = new File(getClass().getResource("testproperties.properties").getFile());
    }

    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testProperties() throws Exception
    {
        MailToolConfiguration conf = new PropertiesFileConfiguration(confFile);
        assertThat(conf.getOperationLimit()).isEqualTo(300);

        Task t = conf.getTask();
        assertThat(t).isNotNull();
        assertThat(t).isInstanceOf(ApplyMatchOperationsTask.class);

    }

    @Test
    public void testPropertiesMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new PropertiesFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testproperties.properties").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(-1);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(0);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(3);
    }
}
