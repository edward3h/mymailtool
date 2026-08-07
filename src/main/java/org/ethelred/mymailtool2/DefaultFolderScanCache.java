package org.ethelred.mymailtool2;

import java.io.File;
import java.io.IOException;
import java.util.OptionalLong;

import javax.annotation.CheckForNull;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Default {@link FolderScanCache} implementation, backed by a {@link ScanState}
 * loaded once at construction time.
 */
public class DefaultFolderScanCache implements FolderScanCache
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final String accountKey;
    @CheckForNull
    private final File stateFile;
    @CheckForNull
    private final String currentFingerprint;
    private final boolean disabled;
    private final ScanState state;

    public DefaultFolderScanCache(String accountKey, @CheckForNull File stateFile,
                                   @CheckForNull String currentFingerprint, boolean disabled)
    {
        this.accountKey = accountKey;
        this.stateFile = stateFile;
        this.currentFingerprint = currentFingerprint;
        this.disabled = disabled;
        // Always non-null, even when unused (disabled or no stateFile): callers guard on those
        // conditions independently before touching state, so there is no cross-field invariant
        // to reconstruct here, and this branch never has anything to load in that case anyway.
        this.state = (disabled || stateFile == null) ? new ScanState() : ScanState.loadOrEmpty(stateFile);
    }

    @Override
    public OptionalLong getResumeUid(Folder folder) throws MessagingException
    {
        if (disabled || stateFile == null || currentFingerprint == null || !(folder instanceof UIDFolder uidFolder))
        {
            return OptionalLong.empty();
        }

        String storedFingerprint = state.getConfigFingerprint();
        if (storedFingerprint == null || !storedFingerprint.equals(currentFingerprint))
        {
            return OptionalLong.empty();
        }

        var stored = state.get(accountKey, folder.getFullName());
        if (stored.isEmpty())
        {
            return OptionalLong.empty();
        }

        long currentUidValidity = uidFolder.getUIDValidity();
        if (stored.get().uidValidity() != currentUidValidity)
        {
            return OptionalLong.empty();
        }

        return OptionalLong.of(stored.get().nextUid());
    }

    @Override
    public void recordScanCompletion(Folder folder, long endUidExclusiveOfThisScan,
                                      @CheckForNull Message lastConsidered, boolean completedFully) throws MessagingException
    {
        // Mirrors getResumeUid's treatment of a null fingerprint as "cache not usable this run":
        // recording here would stamp a null fingerprint over any good one already on disk,
        // which would make every future getResumeUid call see a mismatch and force a full scan
        // even once a real fingerprint becomes available again. No-op instead and let a run
        // with a working fingerprint record state normally.
        if (disabled || stateFile == null || currentFingerprint == null || !(folder instanceof UIDFolder uidFolder))
        {
            return;
        }

        long nextUid;
        if (completedFully)
        {
            nextUid = endUidExclusiveOfThisScan;
        }
        else if (lastConsidered != null)
        {
            nextUid = uidFolder.getUID(lastConsidered);
        }
        else
        {
            // Nothing new learned about this folder; leave any existing stored state untouched.
            return;
        }

        state.put(accountKey, folder.getFullName(), new ScanState.FolderScanState(uidFolder.getUIDValidity(), nextUid));
        state.setConfigFingerprint(currentFingerprint);
        try
        {
            state.save(stateFile);
        }
        catch (IOException ex)
        {
            LOGGER.warn("Failed to save scan-state file {}, continuing without persisting this update", stateFile, ex);
        }
    }
}
