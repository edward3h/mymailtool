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
import org.ethelred.mymailtool2.matcher.AgeMatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final Comparator<? super ApplyKey> FOLDER_PREFERENCE = Ordering.natural().onResultOf(new Function<ApplyKey, Integer>()
    {
        @Override
        public Integer apply(@Nullable ApplyKey applyKey)
        {
            return context.getDefaultFolder().getName().equalsIgnoreCase(applyKey.folderName) ? 0 : 1;
        }
    }).compound(Ordering.natural().onResultOf(new Function<ApplyKey, Boolean>()
    {
        @Override
        public Boolean apply(@Nullable ApplyKey applyKey)
        {
            return applyKey.includeSubFolders;
        }
    }));

    private final Predicate<? super Message> defaultMinAge = new Predicate<Message>()
    {
        @Override
        public boolean apply(@Nullable Message message)
        {
            try
            {
                return message != null && context.isOldEnough(message);
            }
            catch (MessagingException e)
            {
                return false;
            }
        }
    };

    public boolean hasRules()
    {
        return !rules.isEmpty();
    }

    private static class ApplyKey
    {
        private final String folderName;
        private final boolean includeSubFolders;

        private ApplyKey(String folderName, boolean includeSubFolders)
        {
            this.folderName = folderName.toLowerCase();
            this.includeSubFolders = includeSubFolders;
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

            if(includeSubFolders != applyKey.includeSubFolders)
            {
                return false;
            }
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
            result = 31 * result + (includeSubFolders ? 1 : 0);
            return result;
        }

        @Override
        public String toString()
        {
            return Objects.toStringHelper(this)
                    .add("folderName", folderName)
                    .add("includeSubFolders", includeSubFolders)
                    .toString();
        }
    }

    // order is important
    LinkedHashMap<ApplyKey, List<MatchOperation>> rules = Maps.newLinkedHashMap();

    public static ApplyMatchOperationsTask create()
    {
        return new ApplyMatchOperationsTask();
    }

    @Override
    public void run()
    {
        System.out.printf("Starting task with %s folders%n", rules.size());
        // for each folder
        List<ApplyKey> sortedFolders = Lists.newArrayList(rules.keySet());
        Collections.sort(sortedFolders, FOLDER_PREFERENCE);
        for(ApplyKey k: sortedFolders)
        {
            try
            {
                List<MatchOperation> lmo = rules.get(k);
                Collections.sort(lmo, SPECIFIC_OPS);
                System.out.printf("Starting application: %s %s%n", k, Joiner.on(", ").join(lmo));
                traverseFolder(k.folderName, k.includeSubFolders);
            }
            catch (MessagingException | IOException ex)
            {
                Logger.getLogger(ApplyMatchOperationsTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected int openMode()
    {
        return Folder.READ_WRITE;
    }

    private List<MatchOperation> _getRules(String originalName, boolean includeSubFolders)
    {
        ApplyKey k = new ApplyKey(originalName, includeSubFolders);
        if(rules.containsKey(k))
        {
            return rules.get(k);
        }
        throw new IllegalStateException("No matching rules for " + originalName + " with includeSubFolders = " + includeSubFolders);
    }


    private int messageCounter;

    @Override
    protected void runMessage(Folder f, Message m, boolean includeSubFolders, String originalName) throws MessagingException
    {
        if(++messageCounter % 100 == 0)
        {
            System.err.print('.');
        }
        // match/operation
        for(MatchOperation mo: _getRules(originalName, includeSubFolders))
        {
            if(mo.testApply(m, context))
            {
                break;
            }
        }

    }

    @Override
    protected void status(Folder f, String originalName)
    {
        System.out.printf("Working on folder %s%n", f.getFullName());
        messageCounter = 0;
    }

    public void addRule(String folder, Predicate<Message> matcher, List<Predicate<Message>> checkMatchers, MessageOperation operation, boolean includeSubFolders)
    {
        System.out.printf("Adding rule against %s with operation %s matching %s%n", folder, operation.getShortName(), checkMatchers);
        ApplyKey key = new ApplyKey(folder, includeSubFolders);
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
            matcher = Predicates.and(defaultMinAge, matcher);
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
