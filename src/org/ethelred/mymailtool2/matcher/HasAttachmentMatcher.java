package org.ethelred.mymailtool2.matcher;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;

import javax.annotation.Nullable;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Does the message contain any file attachments
 */
public class HasAttachmentMatcher implements Predicate<Message>
{
    private Pattern fileNamePattern;

    public HasAttachmentMatcher(String patternSpec)
    {
        this.fileNamePattern = Pattern.compile(patternSpec);
    }

    @Override
    public boolean apply(@Nullable Message message)
    {
        if(message == null)
        {
            return false;
        }

        try
        {
            if(!message.isMimeType("multipart/mixed"))
            {
                return false;
            }

            Multipart mm = (Multipart) message.getContent();
            for(int i = 0; i < mm.getCount(); i++)
            {
                BodyPart part = mm.getBodyPart(i);
                if(Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && !Strings.isNullOrEmpty(part.getFileName()) && fileNamePattern.matcher(part.getFileName()).find())
                {
                    return true;
                }
            }
        }
        catch (MessagingException | IOException e)
        {
            return false;
        }

        return false;
    }

    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("fileNamePattern", fileNamePattern)
                .toString();
    }
}
