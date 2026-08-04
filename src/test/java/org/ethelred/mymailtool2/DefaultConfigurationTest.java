package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.propertiesfile.PropertiesFileConfigurationHandler;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static com.google.common.truth.Truth.assertThat;

public class DefaultConfigurationTest
{
    @Test
    public void testGetTaskReturnsApplyMatchOperationsTask()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.getTask()).isInstanceOf(ApplyMatchOperationsTask.class);
    }

    @Test
    public void testGetFileLocationsIncludesHomeAndEtcPaths()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        Iterable<String> locations = config.getFileLocations();

        assertThat(locations).contains("/etc/mymailtoolrc.properties");
        boolean hasHomeLocation = StreamSupport.stream(locations.spliterator(), false)
                .anyMatch(l -> l.endsWith(".mymailtoolrc.properties") && !l.equals("/etc/mymailtoolrc.properties"));
        assertThat(hasHomeLocation).isTrue();
    }

    @Test
    public void testGetFileHandlersIncludesPropertiesFileHandler()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.getFileHandlers()).hasSize(1);
        assertThat(config.getFileHandlers().iterator().next())
                .isInstanceOf(PropertiesFileConfigurationHandler.class);
    }

    @Test
    public void testGetScanStateFileReturnsPathEndingInScanStateProperties()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        String scanStateFile = config.getScanStateFile();

        assertThat(scanStateFile).isNotNull();
        assertThat(scanStateFile).endsWith("scan-state.properties");
        assertThat(scanStateFile).contains("mymailtool");
    }

    @Test
    public void testDisableScanCacheDefaultsToFalse()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.disableScanCache()).isFalse();
    }

    @Test
    public void testGetScanStateFileNeverThrows()
    {
        // getScanStateFile() wraps ProjectDirectories.from(...) in a try/catch so that,
        // as the always-safe fallback layer, it returns null instead of propagating an
        // exception (e.g. on an unsupported OS) - hard to trigger for real in a unit test,
        // but this at least pins down that calling it under normal conditions never throws.
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.getScanStateFile()).isNotNull();
    }
}
