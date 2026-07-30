package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class ListFoldersTaskTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testRunDoesNotReadMessages()
    {
        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addMessage("Inbox", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));

        MailToolContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            ListFoldersTask task = (ListFoldersTask) ListFoldersTask.create();
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }

        assertThat(data.folderSize("Inbox")).isEqualTo(1);
    }

    @Test
    public void testReadMessagesReturnsEmpty()
    {
        // run() always passes readMessages=false to traverseFolder, so this override
        // is never reached via the public API; tested directly instead. The null
        // argument is safe because the override ignores its Folder parameter entirely.
        ListFoldersTask task = (ListFoldersTask) ListFoldersTask.create();
        assertThat(task.readMessages(null)).isEmpty();
    }
}
