package org.ethelred.mymailtool2;

import org.ethelred.util.ClockFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 test for main app class
 */
@ExtendWith(MockitoExtension.class)
public class MainTest
{
    @Mock MailToolConfiguration conf;

    @Test
    public void testOperationLimit()
    {
        ClockFactory.setClock(LocalDate.of(2014, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(conf.getOperationLimit()).thenReturn(3);
        when(conf.getTimeLimit()).thenReturn("50 days");
        lenient().when(conf.verbose()).thenReturn(false);

        MailToolContext app = new DefaultContext(conf);
        app.countOperation();
        app.countOperation();
        app.countOperation();
        try
        {
            app.countOperation();
            fail("expected OperationLimitException");
        }
        catch (OperationLimitException e)
        {
            // expected - success
        }

        verify(conf, times(2)).getOperationLimit();
        verify(conf, times(1)).getTimeLimit();
    }

    /**
     * DefaultConfiguration's default file locations point at real files on the running
     * machine ($HOME/.mymailtoolrc.properties, /etc/mymailtoolrc.properties). To keep these
     * fingerprint tests hermetic and independent of whatever happens to exist on the test
     * host, we swap in a variant that reports no default file locations, so only the
     * explicit -c file (plus command line / system properties, which never contribute file
     * locations) feeds the fingerprint.
     */
    private static class NoDefaultFilesConfiguration extends DefaultConfiguration
    {
        @Override
        public Iterable<String> getFileLocations()
        {
            return Collections.emptyList();
        }
    }

    private static Main newMainForConfigFile(Path configFile)
    {
        Main main = new Main();
        main.setDefaultConfiguration(new NoDefaultFilesConfiguration());
        main.init(new String[] {"-c", configFile.toAbsolutePath().toString()});
        return main;
    }

    private static Path writeMinimalConfig(Path dir, String fileName, String host) throws IOException
    {
        Path file = dir.resolve(fileName);
        String content = "mail.store.protocol=imap\n"
                + "mail.user=test@example.com\n"
                + "mail.host=" + host + "\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    public void testInitComputesNonNullConfigFingerprint(@TempDir Path tempDir) throws IOException
    {
        Path configFile = writeMinimalConfig(tempDir, "config.properties", "imap.example.com");

        Main main = newMainForConfigFile(configFile);

        assertThat(main.configFingerprint).isNotNull();
    }

    @Test
    public void testInitProducesSameFingerprintForIdenticalConfigContent(@TempDir Path tempDir) throws IOException
    {
        // ConfigFingerprint hashes each file's (normalized) path as well as its content, by
        // design (see ConfigFingerprint's javadoc), so two distinct file paths never hash
        // equal even with byte-identical content. To isolate "identical content" as the only
        // variable, we point two separate Main instances/init() calls at the very same,
        // unchanged file.
        Path configFile = writeMinimalConfig(tempDir, "config.properties", "imap.example.com");

        Main mainA = newMainForConfigFile(configFile);
        Main mainB = newMainForConfigFile(configFile);

        assertThat(mainA.configFingerprint).isEqualTo(mainB.configFingerprint);
    }

    @Test
    public void testInitProducesDifferentFingerprintForDifferentConfigContent(@TempDir Path tempDir) throws IOException
    {
        Path configFileA = writeMinimalConfig(tempDir, "configA.properties", "imap.example.com");
        Path configFileB = writeMinimalConfig(tempDir, "configB.properties", "imap.other-example.com");

        Main mainA = newMainForConfigFile(configFileA);
        Main mainB = newMainForConfigFile(configFileB);

        assertThat(mainA.configFingerprint).isNotEqualTo(mainB.configFingerprint);
    }

    @Test
    public void testInitDetectsConfigFileContentChangeBetweenRuns(@TempDir Path tempDir) throws IOException
    {
        Path configFile = writeMinimalConfig(tempDir, "config.properties", "imap.example.com");

        Main first = newMainForConfigFile(configFile);
        String firstFingerprint = first.configFingerprint;

        writeMinimalConfig(tempDir, "config.properties", "imap.changed-example.com");

        Main second = newMainForConfigFile(configFile);
        String secondFingerprint = second.configFingerprint;

        assertThat(firstFingerprint).isNotEqualTo(secondFingerprint);
    }
}
