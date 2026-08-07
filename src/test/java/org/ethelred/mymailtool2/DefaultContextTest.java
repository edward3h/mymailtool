package org.ethelred.mymailtool2;

import java.io.File;
import java.util.OptionalLong;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link DefaultContext}'s {@link FolderScanCache} wiring.
 */
public class DefaultContextTest
{
    @TempDir
    File tempDir;

    @BeforeEach
    public void setup()
    {
        MockData data = MockData.getInstance();
        data.addFolder("Folder");
        data.setUidCapable("Folder", true);
    }

    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void oneArgConstructor_folderScanCacheAlwaysEmpty() throws MessagingException
    {
        DefaultContext context = new DefaultContext(new MockDefaultConfiguration());
        context.connect();
        try
        {
            Folder folder = context.getFolder("Folder");
            assertThat(context.getFolderScanCache().getResumeUid(folder)).isEqualTo(OptionalLong.empty());
        }
        finally
        {
            context.disconnect();
        }
    }

    @Test
    public void twoArgConstructor_wiresWorkingFolderScanCache() throws MessagingException
    {
        File stateFile = new File(tempDir, "scan-state.json");
        MockDefaultConfiguration config = new MockDefaultConfiguration()
        {
            @Override
            public String getScanStateFile()
            {
                return stateFile.getAbsolutePath();
            }
        };

        DefaultContext context1 = new DefaultContext(config, "fingerprint-1");
        context1.connect();
        try
        {
            assertThat(context1.getFolderScanCache()).isNotNull();
            Folder folder = context1.getFolder("Folder");
            assertThat(context1.getFolderScanCache().getResumeUid(folder)).isEqualTo(OptionalLong.empty());
            context1.getFolderScanCache().recordScanCompletion(folder, 42L, null, true);
        }
        finally
        {
            context1.disconnect();
        }

        // A second context, constructed with the same config + fingerprint, should see the
        // recorded resume point, proving the account key / state file / fingerprint used by
        // the first context's cache are the ones actually wired through.
        DefaultContext context2 = new DefaultContext(config, "fingerprint-1");
        context2.connect();
        try
        {
            Folder folder = context2.getFolder("Folder");
            assertThat(context2.getFolderScanCache().getResumeUid(folder)).isEqualTo(OptionalLong.of(42L));
        }
        finally
        {
            context2.disconnect();
        }
    }

    @Test
    public void nullUser_doesNotBlowUpAndProducesStableKey() throws MessagingException
    {
        // MockDefaultConfiguration.getUser() returns null, exercising the "no user configured"
        // guard in the account-key computation: it should fall back to a stable placeholder
        // rather than embedding the literal string "null", and still work correctly end to end.
        File stateFile = new File(tempDir, "scan-state.json");
        MockDefaultConfiguration config = new MockDefaultConfiguration()
        {
            @Override
            public String getScanStateFile()
            {
                return stateFile.getAbsolutePath();
            }
        };
        assertThat(config.getUser()).isNull();

        DefaultContext context1 = new DefaultContext(config, "fingerprint-1");
        context1.connect();
        try
        {
            Folder folder = context1.getFolder("Folder");
            assertThat(context1.getFolderScanCache().getResumeUid(folder)).isEqualTo(OptionalLong.empty());
            context1.getFolderScanCache().recordScanCompletion(folder, 7L, null, true);
        }
        finally
        {
            context1.disconnect();
        }

        DefaultContext context2 = new DefaultContext(config, "fingerprint-1");
        context2.connect();
        try
        {
            Folder folder = context2.getFolder("Folder");
            assertThat(context2.getFolderScanCache().getResumeUid(folder)).isEqualTo(OptionalLong.of(7L));
        }
        finally
        {
            context2.disconnect();
        }
    }
}
