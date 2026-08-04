package org.ethelred.mymailtool2;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FolderMatcher;
import org.ethelred.util.Predicates;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 *
 * @author edward
 */
public class ApplyMatchOperationsTask extends TaskBase
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Comparator<? super MatchOperation> SPECIFIC_OPS =
            Comparator.comparing(MatchOperation::getSpecificity).reversed();

    private final Comparator<? super Folder> folderPreference = Comparator.comparing(
            folder ->context.getDefaultFolder().equals(folder) || "INBOX".equalsIgnoreCase(folder.getName()) ? 0 : 1
    );

    Predicate<Message> defaultMinAgeDelegate;
    private  boolean deferredDefaultMinAge(Message message){return defaultMinAgeDelegate.test(message);}
    private final Random random = new Random();

    // Bridges the UID-range snapshot taken in readMessages() to the completion recording done
    // in onFolderScanFinished() for the same folder's scan. Empty means "no valid UID snapshot
    // for this folder's current scan". Safe as a plain mutable field ONLY because run() traverses
    // folders strictly sequentially, one at a time, in a single for loop -- never concurrently.
    // A future refactor introducing parallel folder traversal would need to make this
    // per-traversal state instead of a shared field.
    private OptionalLong scanEndUidExclusive = OptionalLong.empty();

    public boolean hasRules()
    {
        return !rules.isEmpty();
    }

    private static final class ApplyKey
    {
        private final String folderName;

        private ApplyKey(String folderName)
        {
            this.folderName = folderName.toLowerCase();
        }

        public ApplyKey(Folder folder) {
            this(folder.getFullName());
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            ApplyKey applyKey = (ApplyKey) o;

            return folderName.equals(applyKey.folderName);
        }

        @Override
        public int hashCode()
        {
            return folderName.hashCode();
        }

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper(this)
                    .add("folderName", folderName)
                    .toString();
        }
    }

    // order is important
    final LinkedHashMap<ApplyKey, List<MatchOperation>> rules = Maps.newLinkedHashMap();
    final Set<ApplyKey> includeSubFolders = Sets.newHashSet();

    public static ApplyMatchOperationsTask create()
    {
        return new ApplyMatchOperationsTask();
    }

    @Override
    public void init(MailToolContext ctx)
    {
        super.init(ctx);
        defaultMinAgeDelegate = ctx.defaultMinAge(this);
    }

    @Override
    public void run()
    {
        try
        {
            LOGGER.info("Starting task with {} folders", rules.size());
            boolean randomTraversal = context.randomTraversal();
            // for each folder
            List<Folder> sortedFolders = scanFolders(rules.keySet(), randomTraversal);
            sortedFolders.sort(folderPreference);
            if (randomTraversal)
            {
                LOGGER.debug("Folder order...\n{}", () -> Joiner.on("\n").join(sortedFolders));
            }
            for (Folder k : sortedFolders)
            {
                    List<MatchOperation> lmo = getRules(k);
                    lmo.sort(SPECIFIC_OPS);
                    LOGGER.info("Starting application: {} {}", k, Joiner.on(", ").join(lmo));
                    traverseFolder(k, !randomTraversal && includeSubFolders.contains(new ApplyKey(k)), true);

            }
        }
        catch (MessagingException | IOException ex)
        {
            LOGGER.error("Unknown", ex);
        }
    }

    private List<Folder> scanFolders(Set<ApplyKey> applyKeys, boolean randomTraversal) throws IOException, MessagingException {
        final List<Folder> result = Lists.newArrayList();
        TaskBase scanner = new TaskBase(){

            @Override
            public void run() {

            }

            @Override
            protected void runMessage(Folder f, Message m) {

            }

            @Override
            protected void status(Folder f) {
                result.add(f);
            }
        };
        scanner.init(context);
        for (ApplyKey k : applyKeys)
        {
            scanner.traverseFolder(k.folderName, randomTraversal && includeSubFolders.contains(k), false);
        }
        if (randomTraversal)
        {
            Collections.shuffle(result, random);
        }
        return result;
    }

    @Override
    protected int openMode()
    {
        return Folder.READ_WRITE;
    }

    private List<MatchOperation> getRules(@Nonnull Folder of) throws MessagingException {
        for (Folder f = of; f != null; f = f.getParent())
        {
            ApplyKey k = new ApplyKey(f);
            if (rules.containsKey(k))
            {
                return rules.get(k);
            }
        }

        throw new IllegalStateException("No matching rules for " + of.getFullName());
    }

    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException
    {
        LOGGER.debug("Checking {}", () -> MailUtil.supplyString(m).get());
        context.countMessage();
        // match/operation
        int ruleCount = 0;
        int shortcutCount = 0;
        for (MatchOperation mo : getRules(f))
        {
            ruleCount++;
            try
            {
                if (mo.testApply(m, context))
                {
                    break;
                }
            }
            catch (ShortcutFolderScanException e)
            {
                shortcutCount++;
            }
        }

        if (ruleCount == shortcutCount)
        {
            throw new ShortcutFolderScanException();
        }

    }

    @Override
    protected void status(Folder f)
    {
        LOGGER.info("Working on folder {}", f.getFullName());
    }

    @Override
    protected Iterable<? extends Message> readMessages(Folder f)
    {
        scanEndUidExclusive = OptionalLong.empty();
        try
        {
            OptionalLong resumeUid = context.getFolderScanCache().getResumeUid(f);
            if (resumeUid.isPresent())
            {
                UidRangeMessageIterable it = new UidRangeMessageIterable(f, resumeUid.getAsLong());
                scanEndUidExclusive = OptionalLong.of(it.getEndUidExclusive());
                LOGGER.info("Resuming folder {} from UID {} (of {})", f.getFullName(), resumeUid.getAsLong(), scanEndUidExclusive);
                return it;
            }
        }
        catch (MessagingException | RuntimeException e)
        {
            // RuntimeException here covers UidRangeMessageIterable's constructor, which wraps
            // any MessagingException from its own getUIDNext() snapshot call as an unchecked
            // exception. Without catching it too, a transient IMAP failure at that specific call
            // would propagate out of this method (and out of TaskBase.traverseFolder's
            // ShortcutFolderScanException-only catch, and out of run()'s MessagingException/
            // IOException-only catch), aborting the whole run instead of just this one folder.
            LOGGER.warn("Scan cache lookup failed for {}, falling back to full scan", f, e);
        }
        // Full scan path: still snapshot endUid if the folder supports it, so
        // onFolderScanFinished can persist a fresh baseline even though this wasn't a
        // resumed/incremental scan.
        if (f instanceof UIDFolder uf)
        {
            try
            {
                scanEndUidExclusive = OptionalLong.of(uf.getUIDNext());
            }
            catch (MessagingException ignored)
            {
                // leave scanEndUidExclusive empty; onFolderScanFinished will skip recording
            }
        }
        return super.readMessages(f);
    }

    @Override
    protected void onFolderScanFinished(Folder f, boolean completedFully, @CheckForNull Message lastConsidered)
    {
        if (scanEndUidExclusive.isEmpty())
        {
            // No valid UID snapshot was ever taken for this folder's scan (non-UIDFolder, or
            // getUIDNext() failed) -- nothing meaningful to persist.
            return;
        }
        try
        {
            context.getFolderScanCache().recordScanCompletion(f, scanEndUidExclusive.getAsLong(), lastConsidered, completedFully);
        }
        catch (MessagingException e)
        {
            LOGGER.warn("Failed to persist scan state for {}", f, e);
        }
    }

    public void addRule(String folder, Predicate<Message> matcher, List<Predicate<Message>> checkMatchers, MessageOperation operation, boolean includeSubFolders)
    {
        LOGGER.info("Adding rule against {} with operation {} matching {}", folder, operation, checkMatchers);
        ApplyKey key = new ApplyKey(folder);
        List<MatchOperation> lmo = rules.computeIfAbsent(key, k -> Lists.newArrayList());

        if (checkMatchers.stream().noneMatch(messagePredicate -> messagePredicate instanceof AgeMatcher))
        {
            matcher = Predicates.and(this::deferredDefaultMinAge, matcher);
        }

        if (!includeSubFolders)
        {
            matcher = Predicates.and(new FolderMatcher(folder), matcher);
        }
        else
        {
            this.includeSubFolders.add(key);
        }

        MatchOperation mo = new MatchOperation(matcher, operation, checkMatchers.size());
        lmo.add(mo);
        
    }

    @Override
    public boolean orderNewestFirst()
    {
        return false;
    }
}
