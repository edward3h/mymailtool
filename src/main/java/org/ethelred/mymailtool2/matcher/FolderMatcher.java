package org.ethelred.mymailtool2.matcher;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import jakarta.mail.Message;

/**
 * Created by edward on 3/3/14.
 */
public class FolderMatcher implements Predicate<Message>
{

    private final String folder;

    public FolderMatcher(String folder)
    {
        this.folder = folder;
    }

    @Override
    public boolean test(@Nullable Message message)
    {
        if (message == null)
        {
            return false;
        }

        return folder.equalsIgnoreCase(message.getFolder().getFullName());
    }

    @Override public String toString()
    {
        return "Folder(" + folder + ")";
    }
}
