package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import javax.mail.Message;

/**
 * Created by edward on 3/3/14.
 */
public class FolderMatcher implements Predicate<Message>
{

    private String folder;

    public FolderMatcher(String folder)
    {
        this.folder = folder;
    }

    @Override
    public boolean apply(@Nullable Message message)
    {
        if(message == null)
        {
            return false;
        }

//        if(!folder.equalsIgnoreCase(message.getFolder().getFullName()))
//        System.err.println("FolderMatcher " + folder + " check " + message.getFolder().getFullName());

        return folder.equalsIgnoreCase(message.getFolder().getFullName());
    }

    @Override public String toString()
    {
        return "Folder(" + folder + ")";
    }
}
