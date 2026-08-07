package org.ethelred.mymailtool2;

import java.util.OptionalLong;

import javax.annotation.CheckForNull;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

/**
 * Decides, for a given IMAP {@link Folder}, whether a scan can resume from a
 * cached UID or must fall back to a full scan, and records the outcome of a
 * scan so future runs can benefit from it.
 * <p>
 * This is purely a decision-making layer on top of {@link ScanState}: it does
 * not iterate over messages itself (see {@code UidRangeMessageIterable}).
 * <p>
 * Known limitation: once a message has been fully considered by a scan and
 * matched nothing, its UID falls behind the recorded resume point and it will
 * be skipped by every subsequent scan, even if something about the message
 * itself later changes in a way that would now make it match (e.g. a
 * {@code hasFlag}-based rule, and the message is flagged afterwards via
 * another client). Only a full rescan (e.g. {@code --no-scan-cache}, or any
 * change that invalidates the config fingerprint) will reconsider it.
 */
public interface FolderScanCache
{
    /**
     * Returns the UID to resume scanning from for {@code folder}, or
     * {@link OptionalLong#empty()} if no usable cached resume point exists
     * and a full scan should be performed instead.
     */
    OptionalLong getResumeUid(Folder folder) throws MessagingException;

    /**
     * Records the outcome of a scan of {@code folder}.
     *
     * @param endUidExclusiveOfThisScan the UID boundary (exclusive) that was snapshotted
     *                                  before the scan began; used verbatim as the new resume
     *                                  point when {@code completedFully} is {@code true}
     * @param lastConsidered            the last message the scan looked at before stopping
     *                                  short, or {@code null} if none was considered (e.g. an
     *                                  exception before any message was read); may also be
     *                                  {@code null} when {@code completedFully} is {@code true}
     * @param completedFully            whether the scan covered the whole snapshotted range
     */
    void recordScanCompletion(Folder folder, long endUidExclusiveOfThisScan,
                               @CheckForNull Message lastConsidered, boolean completedFully) throws MessagingException;
}
