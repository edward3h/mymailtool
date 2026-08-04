package org.ethelred.mymailtool2;

import org.junit.jupiter.api.Test;
import org.kohsuke.args4j.CmdLineParser;

import static com.google.common.truth.Truth.assertThat;

public class CommandLineConfigurationTest
{
    @Test
    public void testNoScanCacheFlagSetsDisableScanCache() throws Exception
    {
        CommandLineConfiguration clc = new CommandLineConfiguration();
        CmdLineParser parser = new CmdLineParser(clc);
        parser.parseArgument("--no-scan-cache");

        assertThat(clc.disableScanCache()).isTrue();
    }

    @Test
    public void testDisableScanCacheDefaultsToFalse() throws Exception
    {
        CommandLineConfiguration clc = new CommandLineConfiguration();
        CmdLineParser parser = new CmdLineParser(clc);
        parser.parseArgument();

        assertThat(clc.disableScanCache()).isFalse();
    }

    @Test
    public void testGetScanStateFileIsNull()
    {
        CommandLineConfiguration clc = new CommandLineConfiguration();
        assertThat(clc.getScanStateFile()).isNull();
    }
}
