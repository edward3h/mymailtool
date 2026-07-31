package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.google.common.truth.Truth.assertThat;

public class SplitTaskTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testRunSplitsMessageIntoMonthlySubfolder()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        data.addMessage("archive", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", "test subject"));

        DefaultContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            SplitTask task = new SplitTask("archive");
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }

        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.04-Apr-2012")).isEqualTo(1);
        assertThat(context.messageCheckedCount).isEqualTo(1);
    }
}
