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
    @CheckForNull
    private final ScanState state;

    public DefaultFolderScanCache(String accountKey, @CheckForNull File stateFile,
                                   @CheckForNull String currentFingerprint, boolean disabled)
    {
        this.accountKey = accountKey;
        this.stateFile = stateFile;
        this.currentFingerprint = currentFingerprint;
        this.disabled = disabled;
        this.state = (disabled || stateFile == null) ? null : ScanState.loadOrEmpty(stateFile);
    }

    @Override
    public OptionalLong getResumeUid(Folder folder) throws MessagingException
    {
        if (disabled || stateFile == null || currentFingerprint == null || !(folder instanceof UIDFolder))
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

        long currentUidValidity = ((UIDFolder) folder).getUIDValidity();
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
        if (disabled || stateFile == null || !(folder instanceof UIDFolder))
        {
            return;
        }

        UIDFolder uidFolder = (UIDFolder) folder;
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
