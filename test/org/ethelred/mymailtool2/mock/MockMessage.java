package org.ethelred.mymailtool2.mock;

import com.google.common.collect.Maps;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;

/**
 *
 */
public class MockMessage
{
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private String date;
    private String from;
    private String subject;
    private Map<String, String> mockheaders = Maps.newHashMap();

    private MockMessage()
    {
    }

    @Override
    public String toString()
    {
        return "MockMessage{" +
                "date='" + date + '\'' +
                ", from='" + from + '\'' +
                ", subject='" + subject + '\'' +
                '}';
    }

    public static MockMessage create(String date, String from, String subject)
    {
        MockMessage msg = new MockMessage();
        msg.date = date;
        msg.from = from;
        msg.subject = subject;
        return msg;
    }

    public MockMessage addHeader(String header, String value)
    {
        mockheaders.put(header, value);
        return this;
    }


    public Message getMimeMessage(MockFolder mockFolder, int i) throws MessagingException
    {
        return new MockMimeMessage(mockFolder, i);
    }

    public static MockMessage get(Message m) throws MessagingException
    {
        Address[] fromA = m.getFrom();
        String from = fromA.length > 0 ? fromA[0].toString() : "[unknown]";
        return MockMessage.create(dateFormat.format(m.getReceivedDate()), from, m.getSubject());
    }

    public static MockMessage getOuter(Message m)
    {
        if(m instanceof MockMimeMessage)
        {
            return ((MockMimeMessage) m).getMockMessage();
        }
        return null;
    }

    private class MockMimeMessage extends MimeMessage
    {
        public MockMimeMessage(MockFolder mockFolder, int i) throws MessagingException
        {
            super(mockFolder, i);
            headers = new InternetHeaders();
            setSubject(subject);
            setFrom(from);
            try
            {
                setSentDate(dateFormat.parse(date));
            }
            catch(ParseException e)
            {
                throw new MessagingException("Parse exception", e);
            }

            for(Map.Entry<String, String> e: mockheaders.entrySet())
            {
                headers.addHeader(e.getKey(), e.getValue());
            }

        }

        @Override
        public Date getReceivedDate() throws MessagingException
        {
            return getSentDate();
        }

        public MockMessage getMockMessage()
        {
            return MockMessage.this;
        }
    }
}
