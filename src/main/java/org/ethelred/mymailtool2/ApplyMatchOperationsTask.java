package org.ethelred.mymailtool2;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.matcher.FolderMatcher;

import java.io.IOException;
import java.util.*;
import javax.annotation.Nullable;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author edward
 */
public class ApplyMatchOperationsTask extends TaskBase
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Comparator<? super MatchOperation> SPECIFIC_OPS = Ordering.natural().reverse().onResultOf(new Function<MatchOperation, Comparable>()
    {
        @Override
        public Comparable apply(@Nullable MatchOperation matchOperation)
        {
            return matchOperation == null ? 0 : matchOperation.getSpecificity();
        }
    });
    private final Comparator<? super Folder> folderPreference = Ordering.natural().onResultOf(new Function<Folder, Integer>()
    {
        @Override
        public Integer apply(@Nullable Folder applyKey)
        {
            return context.getDefaultFolder().equals(applyKey) || "INBOX".equalsIgnoreCase(applyKey.getName()) ? 0 : 1;
        }
    });


    Predicate<Message> defaultMinAgeDelegate;
    private Predicate<? super Message> deferredDefaultMinAge = new Predicate<Message>()
    {
        @Override
        public boolean apply(@Nullable Message message)
        {
            return defaultMinAgeDelegate.apply(message);
        }
    };

    private Random random = new Random();

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

            return ! !folderName.equals(applyKey.folderName);
        }

        @Override
        public int hashCode()
        {
            int result = folderName.hashCode();
            return result;
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
            Collections.sort(sortedFolders, folderPreference);
            if (randomTraversal)
            {
                LOGGER.debug("Folder order...\n{}", () -> Joiner.on("\n").join(sortedFolders));
            }
            for (Folder k : sortedFolders)
            {
                    List<MatchOperation> lmo = getRules(k);
                    Collections.sort(lmo, SPECIFIC_OPS);
                    LOGGER.info("Starting application: {} {}", k, Joiner.on(", ").join(lmo));
                    traverseFolder(k, !randomTraversal && includeSubFolders.contains(k), true);

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
            protected void runMessage(Folder f, Message m) throws MessagingException, IOException {

            }

            @Override
            protected void status(Folder f) {
                result.add(f);
            }
        };
        scanner.init(context);
        for (ApplyKey k : applyKeys)
        {
            scanner.traverseFolder(k.folderName, includeSubFolders.contains(k) && randomTraversal, false);
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

    private List<MatchOperation> getRules(Folder of) throws MessagingException {
        for (Folder f = of; f != null; f = f.getParent())
        {
            ApplyKey k = new ApplyKey(f.getFullName());
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
        LOGGER.debug("Checking {}", m);
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
        List<MatchOperation> lmo = rules.get(key);
        if (lmo == null)
        {
            lmo = Lists.newArrayList();
            rules.put(key, lmo);
        }

        if (!Iterables.any(checkMatchers, new Predicate<Predicate<Message>>()
        {
            @Override
            public boolean apply(@Nullable Predicate<Message> messagePredicate)
            {
                return messagePredicate instanceof AgeMatcher;
            }
        }))
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
