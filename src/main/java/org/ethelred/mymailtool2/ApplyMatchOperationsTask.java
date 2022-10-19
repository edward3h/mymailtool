package org.ethelred.mymailtool2;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FolderMatcher;
import org.ethelred.util.Predicates;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Predicate<Message> deferredDefaultMinAge = (message) -> defaultMinAgeDelegate.test(message);
    private final Random random = new Random();

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
    LinkedHashMap<ApplyKey, List<MatchOperation>> rules = Maps.newLinkedHashMap();
    Set<ApplyKey> includeSubFolders = Sets.newHashSet();

    public static ApplyMatchOperationsTask create()
    {
        return new ApplyMatchOperationsTask();
    }

    @Override
    public void init(MailToolContext ctx)
    {
        super.init(ctx);
        defaultMinAgeDelegate = ctx.defaultMinAge(this)::test;
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
        LOGGER.debug("Checking {}", MailUtil.supplyString(m));
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

    public void addRule(String folder, Predicate<Message> matcher, List<Predicate<Message>> checkMatchers, MessageOperation operation, boolean includeSubFolders)
    {
        LOGGER.info("Adding rule against {} with operation {} matching {}", folder, operation, checkMatchers);
        ApplyKey key = new ApplyKey(folder);
        List<MatchOperation> lmo = rules.computeIfAbsent(key, k -> Lists.newArrayList());

        if (checkMatchers.stream().noneMatch(messagePredicate -> messagePredicate instanceof AgeMatcher))
        {
            matcher = Predicates.and(deferredDefaultMinAge, matcher);
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
