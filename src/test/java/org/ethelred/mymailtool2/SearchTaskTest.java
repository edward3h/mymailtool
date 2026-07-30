package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.matcher.HasAttachmentMatcher;
import org.ethelred.mymailtool2.matcher.HasFlagMatcher;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.ethelred.util.Predicates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.mail.Message;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Predicate;

import static com.google.common.truth.Truth.assertThat;

public class SearchTaskTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    private void runSearch(SearchTask task) throws IOException
    {
        MailToolContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }
    }

    @Test
    public void testNonMatchingMessageDoesNotDownloadAttachment(@TempDir File outputDir) throws IOException
    {
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addAttachment("report.pdf", "content".getBytes()));

        SearchTask task = new SearchTask("Folder");
        task.addMatcher((Predicate<Message>) message -> false);
        task.addMatcher(new HasAttachmentMatcher(".*"));
        task.setDownloadAttachmentDirectory(outputDir);

        runSearch(task);

        assertThat(outputDir.listFiles()).isEmpty();
    }

    @Test
    public void testMatchingMessageDownloadsAttachment(@TempDir File outputDir) throws IOException
    {
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addAttachment("report.pdf", "content".getBytes()));

        SearchTask task = new SearchTask("Folder");
        task.addMatcher(new HasAttachmentMatcher(".*"));
        task.setDownloadAttachmentDirectory(outputDir);

        runSearch(task);

        File downloaded = new File(outputDir, "report.pdf");
        assertThat(downloaded.exists()).isTrue();
        assertThat(Files.readString(downloaded.toPath())).isEqualTo("content");
    }

    @Test
    public void testExistingDownloadIsNotOverwritten(@TempDir File outputDir) throws IOException
    {
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addAttachment("report.pdf", "new content".getBytes()));

        File existing = new File(outputDir, "report.pdf");
        Files.writeString(existing.toPath(), "original content");

        SearchTask task = new SearchTask("Folder");
        task.addMatcher(new HasAttachmentMatcher(".*"));
        task.setDownloadAttachmentDirectory(outputDir);

        runSearch(task);

        assertThat(Files.readString(existing.toPath())).isEqualTo("original content");
    }

    @Test
    public void testAddMatcherComposesWithAnd(@TempDir File outputDir) throws IOException
    {
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addAttachment("report.pdf", "content".getBytes()));

        SearchTask task = new SearchTask("Folder");
        task.addMatcher(Predicates.alwaysTrue());
        task.addMatcher(new HasAttachmentMatcher(".*"));
        task.addMatcher((Predicate<Message>) message -> false);
        task.setDownloadAttachmentDirectory(outputDir);

        runSearch(task);

        assertThat(outputDir.listFiles()).isEmpty();
    }

    @Test
    public void testAddMatcherWithHasFlagMatcherDoesNotThrow() throws IOException
    {
        // This is a smoke test, not a verification test: printFlags()'s only
        // observable effects are a log line and a System.out.println(), and the
        // design spec deliberately scopes out asserting on either (see
        // docs/superpowers/specs/2026-07-30-zero-coverage-tests-design.md). So this
        // cannot prove printFlags() actually ran — it only proves that matching a
        // message via HasFlagMatcher and reaching that code path doesn't throw
        // (e.g. m.getFlags() succeeding against a real flagged mock message).
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addFlag("myflag"));

        SearchTask task = new SearchTask("Folder");
        task.addMatcher(new HasFlagMatcher("myflag"));

        runSearch(task);
    }
}
