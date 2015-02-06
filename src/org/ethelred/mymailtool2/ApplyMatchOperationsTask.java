package org.ethelred.mymailtool2;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
public class ApplyMatchOperationsTask extends TaskBase
{
    private static final Comparator<? super MatchOperation> SPECIFIC_OPS = Ordering.natural().reverse().onResultOf(new Function<MatchOperation, Comparable>()
    {
        @Override
        public Comparable apply(@Nullable MatchOperation matchOperation)
        {
            return matchOperation == null ? 0 : matchOperation.getSpecificity();
        }
    });
    private final Comparator<? super Folder> FOLDER_PREFERENCE = Ordering.natural().onResultOf(new Function<Folder, Integer>()
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

    private static class ApplyKey
    {
        private final String folderName;

        private ApplyKey(String folderName)
        {
            this.folderName = folderName.toLowerCase();
        }

        @Override
        public boolean equals(Object o)
        {
            if(this == o)
            {
                return true;
            }
            if(o == null || getClass() != o.getClass())
            {
                return false;
            }

            ApplyKey applyKey = (ApplyKey) o;

            if(!folderName.equals(applyKey.folderName))
            {
                return false;
            }

            return true;
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
            return Objects.toStringHelper(this)
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
            System.out.printf("Starting task with %s folders%n", rules.size());
            boolean randomTraversal = context.randomTraversal();
            // for each folder
            List<Folder> sortedFolders = _scanFolders(rules.keySet(), randomTraversal);
            Collections.sort(sortedFolders, FOLDER_PREFERENCE);
            if(randomTraversal)
            {
                context.debugF("Folder order...%n%s", Joiner.on("\n").join(sortedFolders));
            }
            for(Folder k: sortedFolders)
            {
                    List<MatchOperation> lmo = _getRules(k);
                    Collections.sort(lmo, SPECIFIC_OPS);
                    System.out.printf("Starting application: %s %s%n", k, Joiner.on(", ").join(lmo));
                    traverseFolder(k, !randomTraversal && includeSubFolders.contains(k), true);

            }
        }
        catch (MessagingException | IOException ex)
        {
            Logger.getLogger(ApplyMatchOperationsTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Folder> _scanFolders(Set<ApplyKey> applyKeys, boolean randomTraversal) throws IOException, MessagingException {
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
        for(ApplyKey k: applyKeys)
        {
            scanner.traverseFolder(k.folderName, includeSubFolders.contains(k) && randomTraversal, false);
        }
        if(randomTraversal)
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

    private List<MatchOperation> _getRules(Folder of) throws MessagingException {
        for(Folder f = of; f != null; f = f.getParent())
        {
            ApplyKey k = new ApplyKey(f.getFullName());
            if(rules.containsKey(k))
            {
                return rules.get(k);
            }
        }

        throw new IllegalStateException("No matching rules for " + of.getFullName());
    }

    @Override
    protected void runMessage(Folder f, Message m) throws MessagingException
    {
        context.debugF("Checking %s", m);
        context.countMessage();
        // match/operation
        int ruleCount = 0;
        int shortcutCount = 0;
        for(MatchOperation mo: _getRules(f))
        {
            ruleCount++;
            try
            {
                if(mo.testApply(m, context))
                {
                    break;
                }
            }
            catch (ShortcutFolderScanException e)
            {
                shortcutCount++;
            }
        }

        if(ruleCount == shortcutCount)
        {
            throw new ShortcutFolderScanException();
        }

    }

    @Override
    protected void status(Folder f)
    {
        context.debugF("Working on folder %s%n", f.getFullName());
    }

    public void addRule(String folder, Predicate<Message> matcher, List<Predicate<Message>> checkMatchers, MessageOperation operation, boolean includeSubFolders)
    {
        System.out.printf("Adding rule against %s with operation %s matching %s%n", folder, operation, checkMatchers);
        ApplyKey key = new ApplyKey(folder);
        List<MatchOperation> lmo = rules.get(key);
        if(lmo == null)
        {
            lmo = Lists.newArrayList();
            rules.put(key, lmo);
        }

        if(!Iterables.any(checkMatchers, new Predicate<Predicate<Message>>()
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

        if(!includeSubFolders)
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
        return context.randomTraversal() && random.nextBoolean();
    }
}
