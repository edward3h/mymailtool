package org.ethelred.mymailtool2;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

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
        for(ApplyKey k: rules.keySet())
        {
            try
            {
                traverseFolder(k.folderName, k.includeSubFolders);
            }
            catch (MessagingException ex)
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

    @Override
    protected void runMessage(Folder f, Message m, boolean includeSubFolders) throws MessagingException
    {
        // match/operation
        if (context.isOldEnough(m)) {
            for(MatchOperation mo: rules.get(new ApplyKey(f.getName(), includeSubFolders)))
            {
                if(mo.testApply(m, context))
                {
                    break;
                }
            }

        }
    }

    @Override
    protected void status(Folder f)
    {
        System.out.printf("Working on folder %s%n", f.getFullName());
    }

    public void addRule(String folder, MatchOperation mo, boolean includeSubFolders)
    {
        ApplyKey key = new ApplyKey(folder, includeSubFolders);
        List<MatchOperation> lmo = rules.get(key);
        if(lmo == null)
        {
            lmo = Lists.newArrayList();
            rules.put(key, lmo);
        }
        lmo.add(mo);
        Collections.sort(lmo, SPECIFIC_OPS);
        
    }

    @Override
    protected boolean orderNewestFirst()
    {
        return false;
    }
}
