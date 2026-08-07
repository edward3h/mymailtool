package org.ethelred.mymailtool2;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;

/**
 * Iterate, oldest-first, over the messages in a UID-capable folder whose UID falls in
 * {@code [startUid, endUidExclusive)}, where {@code endUidExclusive} is a snapshot of
 * {@link UIDFolder#getUIDNext()} taken once, up front, in the constructor.
 *
 * <p>Taking that snapshot before any fetch happens is the whole point of this class: it turns
 * "how far this scan covers" into an unambiguous, race-free fact, independent of any mail that
 * arrives concurrently while the scan is running. Callers (e.g. a later incremental-scan stage)
 * can rely on {@link #getEndUidExclusive()} as the exact upper bound actually covered by this
 * iteration, safe to persist as the new resume point.
 *
 * <p>This is a single-purpose class for scanning a resume range and is not a general
 * replacement for {@link RecentMessageIterable}: it always iterates oldest-first/ascending and
 * does not support a {@code newestFirst} option.
 *
 * <p>Unlike {@link RecentMessageIterable}, this class does not chunk its fetch: the whole range
 * is retrieved with a single {@link UIDFolder#getMessagesByUID(long, long)} call. This is an
 * intentional v1 design decision, not an oversight — the entire point of the scan cache is that
 * this range is normally small (just the mail that arrived since the last scan), so a single
 * fetch is the common case. If the range is ever large (e.g. the cache was stale for a long
 * time), a single large fetch is an acceptable one-time cost.
 */
public class UidRangeMessageIterable implements Iterable<Message>
{
    private final Folder folder;
    private final long startUid;
    private final long endUidExclusive;

    /**
     * @param folder   a folder that MUST implement {@link UIDFolder}; the caller is expected to
     *                 have already confirmed this (e.g. via a resume-uid lookup's contract)
     *                 before constructing this class
     * @param startUid the inclusive lower bound of the UID range to iterate
     * @throws IllegalArgumentException if {@code folder} does not implement {@link UIDFolder}
     * @throws RuntimeException         wrapping any {@link MessagingException} thrown while
     *                                  snapshotting {@code UIDFolder.getUIDNext()}
     */
    public UidRangeMessageIterable(Folder folder, long startUid)
    {
        if (!(folder instanceof UIDFolder))
        {
            throw new IllegalArgumentException("Folder must implement UIDFolder: " + folder);
        }
        this.folder = folder;
        this.startUid = startUid;
        try
        {
            this.endUidExclusive = ((UIDFolder) folder).getUIDNext();
        }
        catch (MessagingException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the snapshot of {@code UIDFolder.getUIDNext()} taken in the constructor; this value
     *         never changes, regardless of subsequent folder mutations or calls to
     *         {@link #iterator()}
     */
    public long getEndUidExclusive()
    {
        return endUidExclusive;
    }

    @Override
    public Iterator<Message> iterator()
    {
        if (startUid >= endUidExclusive)
        {
            return Collections.emptyIterator();
        }
        try
        {
            return new UidRangeMessageIterator(folder, startUid, endUidExclusive);
        }
        catch (MessagingException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static class UidRangeMessageIterator implements Iterator<Message>
    {
        private final Message[] messages;
        private int arrayIndex;

        UidRangeMessageIterator(Folder folder, long startUid, long endUidExclusive) throws MessagingException
        {
            UIDFolder uidFolder = (UIDFolder) folder;
            Message[] fetched = uidFolder.getMessagesByUID(startUid, UIDFolder.LASTUID);

            // Race guard: a message may have arrived between the constructor's snapshot of
            // endUidExclusive and this fetch call. Filter out anything at or beyond the snapshot
            // so this iteration covers exactly [startUid, endUidExclusive).
            Message[] inRange = new Message[fetched.length];
            int count = 0;
            for (Message m : fetched)
            {
                if (uidFolder.getUID(m) < endUidExclusive)
                {
                    inRange[count++] = m;
                }
            }
            messages = Arrays.copyOf(inRange, count);

            // Should already be ascending per JavaMail contract, but don't rely on it.
            Arrays.sort(messages, Comparator.comparingLong(m -> {
                try
                {
                    return uidFolder.getUID(m);
                }
                catch (MessagingException e)
                {
                    throw new RuntimeException(e);
                }
            }));

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            folder.fetch(messages, fp);

            arrayIndex = 0;
        }

        @Override
        public boolean hasNext()
        {
            return arrayIndex < messages.length;
        }

        @Override
        public Message next()
        {
            return messages[arrayIndex++];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
