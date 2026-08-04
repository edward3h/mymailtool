package org.ethelred.mymailtool2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

/**
 * tests for ScanState
 */
public class ScanStateTest
{
    @Test
    public void roundTripSavesAndLoadsFingerprintAndFolderEntries(@TempDir File dir) throws IOException
    {
        File file = new File(dir, "scan-state.properties");
        ScanState state = new ScanState();
        state.setConfigFingerprint("abc123");
        state.put("jaq@example.com", "INBOX.Archive", new ScanState.FolderScanState(1690000000L, 48213L));
        state.put("jaq@example.com", "INBOX", new ScanState.FolderScanState(1690000001L, 9981L));
        state.put("other@example.com", "Some/Folder", new ScanState.FolderScanState(42L, 7L));

        state.save(file);

        ScanState loaded = ScanState.loadOrEmpty(file);

        assertThat(loaded.getConfigFingerprint()).isEqualTo("abc123");
        assertThat(loaded.get("jaq@example.com", "INBOX.Archive"))
                .hasValue(new ScanState.FolderScanState(1690000000L, 48213L));
        assertThat(loaded.get("jaq@example.com", "INBOX"))
                .hasValue(new ScanState.FolderScanState(1690000001L, 9981L));
        assertThat(loaded.get("other@example.com", "Some/Folder"))
                .hasValue(new ScanState.FolderScanState(42L, 7L));
    }

    @Test
    public void loadOrEmptyReturnsEmptyStateWhenFileMissing(@TempDir File dir)
    {
        File file = new File(dir, "does-not-exist.properties");

        ScanState state = ScanState.loadOrEmpty(file);

        assertThat(state.getConfigFingerprint()).isNull();
        assertThat(state.get("someone@example.com", "INBOX")).isEqualTo(Optional.empty());
    }

    @Test
    public void loadOrEmptyReturnsEmptyStateWhenFileIsCorrupt(@TempDir File dir) throws IOException
    {
        File file = new File(dir, "corrupt.properties");
        // bytes that will fail Properties.load due to invalid unicode escape
        Files.write(file.toPath(), new byte[] {(byte) 0xFF, (byte) 0xFE, '\\', 'u', 'z', 'z', 'z', 'z'});

        ScanState state = ScanState.loadOrEmpty(file);

        assertThat(state.getConfigFingerprint()).isNull();
        assertThat(state.get("someone@example.com", "INBOX")).isEqualTo(Optional.empty());
    }

    @Test
    public void loadOrEmptySkipsMalformedFolderEntryButKeepsOthers(@TempDir File dir) throws IOException
    {
        File file = new File(dir, "partial.properties");
        String content = """
                configFingerprint=fingerprint-value
                folder.0.account=jaq@example.com
                folder.0.name=INBOX
                folder.0.uidValidity=100
                folder.0.nextUid=200
                folder.1.account=jaq@example.com
                folder.1.name=Broken
                folder.1.uidValidity=100
                folder.1.nextUid=not-a-number
                """;
        Files.writeString(file.toPath(), content);

        ScanState state = ScanState.loadOrEmpty(file);

        assertThat(state.getConfigFingerprint()).isEqualTo("fingerprint-value");
        assertThat(state.get("jaq@example.com", "INBOX"))
                .hasValue(new ScanState.FolderScanState(100L, 200L));
        assertThat(state.get("jaq@example.com", "Broken")).isEqualTo(Optional.empty());
    }

    @Test
    public void saveCreatesMissingParentDirectories(@TempDir File dir) throws IOException
    {
        File nested = new File(new File(dir, "nested/sub"), "scan-state.properties");
        ScanState state = new ScanState();
        state.setConfigFingerprint("fp");

        state.save(nested);

        assertThat(nested.exists()).isTrue();
        ScanState loaded = ScanState.loadOrEmpty(nested);
        assertThat(loaded.getConfigFingerprint()).isEqualTo("fp");
    }

    @Test
    public void saveIsAtomicAndLeavesNoTempFile(@TempDir File dir) throws IOException
    {
        File file = new File(dir, "scan-state.properties");
        File tmpFile = new File(dir, "scan-state.properties.tmp");
        ScanState state = new ScanState();
        state.setConfigFingerprint("fp-atomic");
        state.put("jaq@example.com", "INBOX", new ScanState.FolderScanState(1L, 2L));

        state.save(file);

        assertThat(tmpFile.exists()).isFalse();
        assertThat(file.exists()).isTrue();

        ScanState loaded = ScanState.loadOrEmpty(file);
        assertThat(loaded.getConfigFingerprint()).isEqualTo("fp-atomic");
        assertThat(loaded.get("jaq@example.com", "INBOX"))
                .hasValue(new ScanState.FolderScanState(1L, 2L));
    }

    @Test
    public void getReturnsEmptyForUnknownFolder()
    {
        ScanState state = new ScanState();
        state.put("jaq@example.com", "INBOX", new ScanState.FolderScanState(1L, 2L));

        assertThat(state.get("jaq@example.com", "Other")).isEqualTo(Optional.empty());
        assertThat(state.get("someone-else@example.com", "INBOX")).isEqualTo(Optional.empty());
    }
}
