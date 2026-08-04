package org.ethelred.mymailtool2;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.ethelred.mymailtool2.mock.MockAuthenticator;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link DefaultFolderScanCache}.
 */
public class DefaultFolderScanCacheTest
{
    private static final String ACCOUNT = "jaq@example.com";
    private static final String FOLDER_NAME = "INBOX";
    private static final String FINGERPRINT = "fingerprint-1";

    private Store getStore() throws MessagingException
    {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        Session ss = Session.getDefaultInstance(props, new MockAuthenticator());
        return ss.getStore();
    }

    @BeforeEach
    public void setUp()
    {
        MockData.clear();
    }

    @AfterEach
    public void tearDown()
    {
        MockData.clear();
    }

    private Folder uidFolder() throws MessagingException
    {
        MockData.getInstance().setUidCapable(FOLDER_NAME, true);
        return getStore().getFolder(FOLDER_NAME);
    }

    private Folder plainFolder() throws MessagingException
    {
        return getStore().getFolder(FOLDER_NAME);
    }

    // --- getResumeUid failure-mode table ---

    @Test
    public void disabledAlwaysEmptyEvenWithMatchingFingerprintAndPriorState(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, true);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void nullStateFileAlwaysEmpty() throws MessagingException
    {
        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, null, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void nullCurrentFingerprintAlwaysEmpty(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, null, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void nonUidFolderAlwaysEmpty(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = plainFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void noStateFileYetIsEmpty(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "does-not-exist.properties");
        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void mismatchedFingerprintIsEmptyEvenWithFolderEntry(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint("old-fingerprint");
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void matchingFingerprintButNoEntryForFolderIsEmpty(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, "SomeOtherFolder", new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void uidValidityMismatchIsEmpty(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(999L, 42L));
        seed.save(stateFile);

        MockData.getInstance().setUidCapable(FOLDER_NAME, true);
        MockData.getInstance().setUidValidity(FOLDER_NAME, 1L);
        Folder folder = getStore().getFolder(FOLDER_NAME);
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder).isPresent()).isFalse();
    }

    @Test
    public void matchingEverythingReturnsStoredNextUid(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        assertThat(cache.getResumeUid(folder)).isEqualTo(java.util.OptionalLong.of(42L));
    }

    // --- recordScanCompletion ---

    @Test
    public void completedFullyPersistsSnapshottedEndUidNotCurrentUidNext(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "scan-state.properties");
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        Folder folder = getStore().getFolder(FOLDER_NAME);

        long endUidExclusive = data.getUidNext(FOLDER_NAME); // 2

        // add another message AFTER snapshot but BEFORE recording completion
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-02", "b@example.com", "two"));
        assertThat(data.getUidNext(FOLDER_NAME)).isGreaterThan(endUidExclusive);

        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);
        cache.recordScanCompletion(folder, endUidExclusive, null, true);

        ScanState reloaded = ScanState.loadOrEmpty(stateFile);
        assertThat(reloaded.get(ACCOUNT, FOLDER_NAME))
                .hasValue(new ScanState.FolderScanState(1L, endUidExclusive));
    }

    @Test
    public void incompleteScanWithLastConsideredPersistsItsUid(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "scan-state.properties");
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        MockMessage first = MockMessage.create("2024-01-01", "a@example.com", "one");
        MockMessage second = MockMessage.create("2024-01-02", "b@example.com", "two");
        data.addMessage(FOLDER_NAME, first);
        data.addMessage(FOLDER_NAME, second);
        Folder folder = getStore().getFolder(FOLDER_NAME);
        jakarta.mail.UIDFolder uidFolder = (jakarta.mail.UIDFolder) folder;
        Message lastConsidered = uidFolder.getMessageByUID(1L);

        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);
        cache.recordScanCompletion(folder, 999L, lastConsidered, false);

        ScanState reloaded = ScanState.loadOrEmpty(stateFile);
        assertThat(reloaded.get(ACCOUNT, FOLDER_NAME))
                .hasValue(new ScanState.FolderScanState(1L, 1L));
    }

    @Test
    public void incompleteScanWithNoLastConsideredLeavesExistingStateUntouched(@TempDir File dir) throws MessagingException, IOException
    {
        File stateFile = new File(dir, "scan-state.properties");
        ScanState seed = new ScanState();
        seed.setConfigFingerprint(FINGERPRINT);
        seed.put(ACCOUNT, FOLDER_NAME, new ScanState.FolderScanState(1L, 42L));
        seed.save(stateFile);

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);
        cache.recordScanCompletion(folder, 999L, null, false);

        ScanState reloaded = ScanState.loadOrEmpty(stateFile);
        assertThat(reloaded.get(ACCOUNT, FOLDER_NAME))
                .hasValue(new ScanState.FolderScanState(1L, 42L));
        assertThat(reloaded.getConfigFingerprint()).isEqualTo(FINGERPRINT);
    }

    @Test
    public void disabledRecordScanCompletionIsTrueNoOp(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "scan-state.properties");
        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, true);

        cache.recordScanCompletion(folder, 100L, null, true);

        assertThat(stateFile.exists()).isFalse();
    }

    @Test
    public void nullStateFileRecordScanCompletionIsNoOp() throws MessagingException
    {
        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, null, FINGERPRINT, false);

        cache.recordScanCompletion(folder, 100L, null, true);
        // no exception thrown is the assertion
    }

    @Test
    public void nonUidFolderRecordScanCompletionIsNoOp(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "scan-state.properties");
        Folder folder = plainFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);

        cache.recordScanCompletion(folder, 100L, null, true);

        assertThat(stateFile.exists()).isFalse();
    }

    @Test
    public void ioFailureOnSaveIsSwallowedNotPropagated(@TempDir File dir) throws MessagingException
    {
        // Use a directory as the "state file" so writing to it fails with IOException.
        File stateFileAsDirectory = new File(dir, "state-is-a-directory");
        assertThat(stateFileAsDirectory.mkdirs()).isTrue();

        Folder folder = uidFolder();
        FolderScanCache cache = new DefaultFolderScanCache(ACCOUNT, stateFileAsDirectory, FINGERPRINT, false);

        // Should not throw, despite the underlying save() failing.
        cache.recordScanCompletion(folder, 100L, null, true);
    }

    @Test
    public void freshCacheInstanceSeesPersistedStateAfterRecordScanCompletion(@TempDir File dir) throws MessagingException
    {
        File stateFile = new File(dir, "scan-state.properties");
        MockData data = MockData.getInstance();
        data.setUidCapable(FOLDER_NAME, true);
        data.addMessage(FOLDER_NAME, MockMessage.create("2024-01-01", "a@example.com", "one"));
        Folder folder = getStore().getFolder(FOLDER_NAME);
        long endUidExclusive = data.getUidNext(FOLDER_NAME);

        FolderScanCache first = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);
        first.recordScanCompletion(folder, endUidExclusive, null, true);

        FolderScanCache second = new DefaultFolderScanCache(ACCOUNT, stateFile, FINGERPRINT, false);
        Folder folderAgain = getStore().getFolder(FOLDER_NAME);

        assertThat(second.getResumeUid(folderAgain)).isEqualTo(java.util.OptionalLong.of(endUidExclusive));
    }
}
