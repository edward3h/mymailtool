package org.ethelred.mymailtool2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 *
 * @author edward
 */
public class ApplyMatchOperationsTask extends TaskBase
{
    // order is important
    // key is folder name
    LinkedHashMap<String, List<MatchOperation>> rules = Maps.newLinkedHashMap();

    public static ApplyMatchOperationsTask create()
    {
        return new ApplyMatchOperationsTask();
    }

    public void run()
    {
        // for each folder
        for(String folderName: rules.keySet())
        {
            try
            {
                Folder folder = context.getFolder(folderName);
                // does not traverse sub-folders - consider as future option
                
                if (folder != null && folder.exists()) {
                    folder.open(Folder.READ_WRITE);
                    
                // for each message
                    for (Message m : folder.getMessages()) {
                        
                    // match/operation
                        if (context.isOldEnough(m)) {
                            for(MatchOperation mo: rules.get(folderName))
                            {
                                mo.testApply(m, context);
                            }

                        }
                    }
                    folder.close(true);
                } else {
                    System.out.printf("Folder %s was not found.%n", folderName);
                }
            }
            catch (MessagingException ex)
            {
                Logger.getLogger(ApplyMatchOperationsTask.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void addRule(String folder, MatchOperation mo)
    {
        List<MatchOperation> lmo = rules.get(folder);
        if(lmo == null)
        {
            lmo = Lists.newArrayList();
            rules.put(folder, lmo);
        }
        lmo.add(mo);
        
    }
}
