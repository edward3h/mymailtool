# Zero-Coverage Class Tests Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add unit tests for the 8 classes currently at 0% JaCoCo line coverage (`FlagOperation`, `HasAttachmentMatcher`, `HasFlagMatcher`, `DefaultConfiguration`, `ListFoldersTask`, `SplitTask`, `SearchTask`, `JavascriptFileConfiguration$FlagBuilder`), matching each to the test convention already used for similar classes in this codebase.

**Architecture:** No production code changes except one small addition to the hand-rolled test fixture (`MockMessage.addAttachment(...)`/`addFlag(...)`) needed to make `SearchTask`'s attachment-download logic testable. Everything else is new test files/methods using existing patterns: Mockito for message/operation-level tests, the `MockData`/`MockMessage`/`MockDefaultConfiguration`/`DefaultContext` fixture for task-level tests, and a JS-fixture addition for the DSL builder.

**Tech Stack:** JUnit 6 (Jupiter), Google Truth, Mockito — same as the rest of the suite (see PR #139).

**Spec:** `docs/superpowers/specs/2026-07-30-zero-coverage-tests-design.md`

---

## Chunk 1: Operation and matcher tests

### Task 1: `FlagOperation` tests

**Files:**
- Create: `src/test/java/org/ethelred/mymailtool2/FlagOperationTest.java`

- [ ] **Step 1: Write the test file**

```java
package org.ethelred.mymailtool2;

import jakarta.mail.Flags;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlagOperationTest
{
    @Mock Message m;
    @Mock MailToolContext mailContext;

    @Test
    public void testAddFlagWhenAbsent() throws MessagingException
    {
        when(m.getFlags()).thenReturn(new Flags());

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isTrue();
        ArgumentCaptor<Flags> captor = ArgumentCaptor.forClass(Flags.class);
        verify(m).setFlags(captor.capture(), eq(true));
        assertThat(captor.getValue().contains("myflag")).isTrue();
    }

    @Test
    public void testAddFlagWhenAlreadyPresent() throws MessagingException
    {
        Flags existing = new Flags();
        existing.add("myflag");
        when(m.getFlags()).thenReturn(existing);

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verify(m, never()).setFlags(any(Flags.class), anyBoolean());
    }

    @Test
    public void testRemoveFlagWhenPresent() throws MessagingException
    {
        Flags existing = new Flags();
        existing.add("myflag");
        when(m.getFlags()).thenReturn(existing);

        FlagOperation op = new FlagOperation(false, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isTrue();
        ArgumentCaptor<Flags> captor = ArgumentCaptor.forClass(Flags.class);
        verify(m).setFlags(captor.capture(), eq(false));
        assertThat(captor.getValue().contains("myflag")).isTrue();
    }

    @Test
    public void testRemoveFlagWhenAlreadyAbsent() throws MessagingException
    {
        when(m.getFlags()).thenReturn(new Flags());

        FlagOperation op = new FlagOperation(false, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verify(m, never()).setFlags(any(Flags.class), anyBoolean());
    }

    @Test
    public void testMessagingExceptionReturnsFalse() throws MessagingException
    {
        when(m.getFlags()).thenThrow(new MessagingException("boom"));

        FlagOperation op = new FlagOperation(true, "myflag");
        boolean result = op.apply(mailContext, m);

        assertThat(result).isFalse();
        verifyNoInteractions(mailContext);
    }

    @Test
    public void testFinishApplyingReturnsFalse()
    {
        FlagOperation op = new FlagOperation(true, "myflag");
        assertThat(op.finishApplying()).isFalse();
    }
}
```

Note: `mailContext` is unused by `FlagOperation.apply` (matches the same
pattern already fixed in `MatchOperationTest`/`MessageOperationsTest` during
the JUnit6 migration — jmock's implicit strictness has no Mockito
equivalent, so `verifyNoInteractions(mailContext)` on the exception-path test
is the only place it's meaningfully asserted; omit it from the other tests
since `mailContext` is never touched by this class at all, so it would just
be redundant everywhere).

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.FlagOperationTest"`
Expected: all 6 tests PASS. If `verify(m).setFlags(...)` fails because
`Flags.contains("myflag")` isn't matching as expected, double check the
captured `Flags` object directly (print `captor.getValue()`) rather than
changing the production code — `FlagOperation` is known-correct, already at
100% branch coverage in intent, just untested.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/FlagOperationTest.java
git commit -m "Add tests for FlagOperation"
```

### Task 2: `HasAttachmentMatcher` and `HasFlagMatcher` tests

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/matcher/MatchersTest.java`

- [ ] **Step 1: Add imports**

Add to the existing import block in `MatchersTest.java`:

```java
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Multipart;
import jakarta.mail.Part;

import java.io.IOException;
```

(`Message`, `MessagingException`, `Predicate`, `assertThat`, `fail`, `mock`,
`when` are already imported.)

- [ ] **Step 2: Add `HasAttachmentMatcher` test methods**

Add these methods to `MatchersTest.java` (reuses the existing `msg` field
from `setup()`):

```java
@Test
public void testHasAttachmentMatcherNullMessage()
{
    Predicate<Message> matcher = new HasAttachmentMatcher(".*");
    assertThat(matcher.test(null)).isFalse();
}

@Test
public void testHasAttachmentMatcherNonMultipart() throws MessagingException
{
    when(msg.isMimeType("multipart/mixed")).thenReturn(false);

    Predicate<Message> matcher = new HasAttachmentMatcher(".*");
    assertThat(matcher.test(msg)).isFalse();
}

@Test
public void testHasAttachmentMatcherMatchingFilename() throws MessagingException, IOException
{
    when(msg.isMimeType("multipart/mixed")).thenReturn(true);
    Multipart mp = mock(Multipart.class);
    BodyPart part = mock(BodyPart.class);
    when(msg.getContent()).thenReturn(mp);
    when(mp.getCount()).thenReturn(1);
    when(mp.getBodyPart(0)).thenReturn(part);
    when(part.getDisposition()).thenReturn(Part.ATTACHMENT);
    when(part.getFileName()).thenReturn("report.pdf");

    Predicate<Message> matcher = new HasAttachmentMatcher("report.*");
    assertThat(matcher.test(msg)).isTrue();
}

@Test
public void testHasAttachmentMatcherFilenameDoesNotMatchPattern() throws MessagingException, IOException
{
    when(msg.isMimeType("multipart/mixed")).thenReturn(true);
    Multipart mp = mock(Multipart.class);
    BodyPart part = mock(BodyPart.class);
    when(msg.getContent()).thenReturn(mp);
    when(mp.getCount()).thenReturn(1);
    when(mp.getBodyPart(0)).thenReturn(part);
    when(part.getDisposition()).thenReturn(Part.ATTACHMENT);
    when(part.getFileName()).thenReturn("other.txt");

    Predicate<Message> matcher = new HasAttachmentMatcher("report.*");
    assertThat(matcher.test(msg)).isFalse();
}

@Test
public void testHasAttachmentMatcherNonAttachmentPart() throws MessagingException, IOException
{
    when(msg.isMimeType("multipart/mixed")).thenReturn(true);
    Multipart mp = mock(Multipart.class);
    BodyPart part = mock(BodyPart.class);
    when(msg.getContent()).thenReturn(mp);
    when(mp.getCount()).thenReturn(1);
    when(mp.getBodyPart(0)).thenReturn(part);
    when(part.getDisposition()).thenReturn(Part.INLINE);

    Predicate<Message> matcher = new HasAttachmentMatcher(".*");
    assertThat(matcher.test(msg)).isFalse();
}

@Test
public void testHasAttachmentMatcherMessagingException() throws MessagingException
{
    when(msg.isMimeType("multipart/mixed")).thenThrow(new MessagingException("boom"));

    Predicate<Message> matcher = new HasAttachmentMatcher(".*");
    assertThat(matcher.test(msg)).isFalse();
}
```

- [ ] **Step 3: Add `HasFlagMatcher` test methods**

```java
@Test
public void testHasFlagMatcherNullMessage()
{
    Predicate<Message> matcher = new HasFlagMatcher("myflag");
    assertThat(matcher.test(null)).isFalse();
}

@Test
public void testHasFlagMatcherFlagPresent() throws MessagingException
{
    Flags flags = new Flags();
    flags.add("myflag");
    when(msg.getFlags()).thenReturn(flags);

    Predicate<Message> matcher = new HasFlagMatcher("myflag");
    assertThat(matcher.test(msg)).isTrue();
}

@Test
public void testHasFlagMatcherFlagAbsent() throws MessagingException
{
    when(msg.getFlags()).thenReturn(new Flags());

    Predicate<Message> matcher = new HasFlagMatcher("myflag");
    assertThat(matcher.test(msg)).isFalse();
}

@Test
public void testHasFlagMatcherMessagingException() throws MessagingException
{
    when(msg.getFlags()).thenThrow(new MessagingException("boom"));

    Predicate<Message> matcher = new HasFlagMatcher("myflag");
    assertThat(matcher.test(msg)).isFalse();
}
```

- [ ] **Step 4: Run the tests**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.matcher.MatchersTest"`
Expected: all tests (existing + 10 new) PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/matcher/MatchersTest.java
git commit -m "Add tests for HasAttachmentMatcher and HasFlagMatcher"
```

### Task 3: `DefaultConfiguration` tests

**Files:**
- Create: `src/test/java/org/ethelred/mymailtool2/DefaultConfigurationTest.java`

- [ ] **Step 1: Write the test file**

```java
package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.propertiesfile.PropertiesFileConfigurationHandler;
import org.junit.jupiter.api.Test;

import java.util.stream.StreamSupport;

import static com.google.common.truth.Truth.assertThat;

public class DefaultConfigurationTest
{
    @Test
    public void testGetTaskReturnsApplyMatchOperationsTask()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.getTask()).isInstanceOf(ApplyMatchOperationsTask.class);
    }

    @Test
    public void testGetFileLocationsIncludesHomeAndEtcPaths()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        Iterable<String> locations = config.getFileLocations();

        assertThat(locations).contains("/etc/mymailtoolrc.properties");
        boolean hasHomeLocation = StreamSupport.stream(locations.spliterator(), false)
                .anyMatch(l -> l.endsWith(".mymailtoolrc.properties") && !l.equals("/etc/mymailtoolrc.properties"));
        assertThat(hasHomeLocation).isTrue();
    }

    @Test
    public void testGetFileHandlersIncludesPropertiesFileHandler()
    {
        DefaultConfiguration config = new DefaultConfiguration();
        assertThat(config.getFileHandlers()).hasSize(1);
        assertThat(config.getFileHandlers().iterator().next())
                .isInstanceOf(PropertiesFileConfigurationHandler.class);
    }
}
```

(This deliberately skips the plain constant getters — `getPassword`,
`getUser`, `getOperationLimit`, `getMinAge`, `getTimeLimit`, `verbose`,
`getChunkSize`, `randomTraversal`, `toString` — per the spec's "skip trivial
stuff" scope.)

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.DefaultConfigurationTest"`
Expected: all 3 tests PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/DefaultConfigurationTest.java
git commit -m "Add tests for DefaultConfiguration"
```

---

## Chunk 2: Task-level tests

### Task 4: `ListFoldersTask` tests

**Files:**
- Create: `src/test/java/org/ethelred/mymailtool2/ListFoldersTaskTest.java`

- [ ] **Step 1: Write the test file**

```java
package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class ListFoldersTaskTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testRunDoesNotReadMessages()
    {
        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addMessage("Inbox", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));

        MailToolContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            ListFoldersTask task = (ListFoldersTask) ListFoldersTask.create();
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }

        assertThat(data.folderSize("Inbox")).isEqualTo(1);
    }

    @Test
    public void testReadMessagesReturnsEmpty()
    {
        ListFoldersTask task = (ListFoldersTask) ListFoldersTask.create();
        assertThat(task.readMessages(null)).isEmpty();
    }
}
```

Note: `readMessages` is `protected` on `TaskBase`; calling it directly from
`ListFoldersTaskTest` works because the test class is in the same package
(`org.ethelred.mymailtool2`), no subclassing needed.

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.ListFoldersTaskTest"`
Expected: both tests PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/ListFoldersTaskTest.java
git commit -m "Add tests for ListFoldersTask"
```

### Task 5: `SplitTask` tests

**Files:**
- Create: `src/test/java/org/ethelred/mymailtool2/SplitTaskTest.java`

- [ ] **Step 1: Write the test file**

```java
package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.google.common.truth.Truth.assertThat;

public class SplitTaskTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testRunSplitsMessageIntoMonthlySubfolder()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        data.addMessage("archive", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", "test subject"));

        DefaultContext context = new DefaultContext(new MockDefaultConfiguration());
        try
        {
            context.connect();
            SplitTask task = new SplitTask("archive");
            task.init(context);
            task.run();
        }
        finally
        {
            context.disconnect();
        }

        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.04-Apr-2012")).isEqualTo(1);
        assertThat(context.messageCheckedCount).isEqualTo(1);
    }
}
```

(The `archive.2012.04-Apr-2012` subfolder naming mirrors what
`JavascriptConfigurationTest.testJSMain` already demonstrates for
`archive.2012.01-Jan-2012` — same `SplitOperation` logic, different month.
`context.messageCheckedCount` is package-private and directly accessible,
same as in `ApplyMatchOperationsTaskTest`.)

- [ ] **Step 2: Run the test**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.SplitTaskTest"`
Expected: PASS. If the subfolder name doesn't match, check
`SplitOperation.getSubFolderName`'s actual date format output rather than
guessing further — the pattern is `folder + sep + year + sep + MM-MMM-yyyy`.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/SplitTaskTest.java
git commit -m "Add tests for SplitTask"
```

### Task 6: Extend `MockMessage` with attachment and flag support

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/mock/MockMessage.java`

This is required before `SearchTask`'s attachment-download and
flag-printing logic can be tested (Task 7) — `MockMessage` currently has no
way to give a message multipart/attachment content, and `getContent()`
throws unconditionally without it.

- [ ] **Step 1: Add imports**

Add to `MockMessage.java`:

```java
import com.google.common.collect.Lists;

import java.util.List;

import jakarta.activation.DataHandler;
import jakarta.mail.Flags;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
```

- [ ] **Step 2: Add fields and the `Attachment` record**

Add just below the existing `mockheaders` field:

```java
    private List<Attachment> attachments = Lists.newArrayList();
    private Flags flags = new Flags();

    private record Attachment(String filename, byte[] content) {}
```

- [ ] **Step 3: Add builder methods**

Add next to the existing `addHeader` method:

```java
    public MockMessage addAttachment(String filename, byte[] content)
    {
        attachments.add(new Attachment(filename, content));
        return this;
    }

    public MockMessage addFlag(String flag)
    {
        flags.add(flag);
        return this;
    }
```

- [ ] **Step 4: Wire attachments/flags into `MockMimeMessage`**

In the `MockMimeMessage` constructor, after the existing header loop
(`for (Map.Entry<String, String> e : mockheaders.entrySet()) { ... }`), add:

```java
            setFlags(flags, true);

            if (!attachments.isEmpty())
            {
                MimeMultipart multipart = new MimeMultipart();
                for (Attachment attachment : attachments)
                {
                    MimeBodyPart part = new MimeBodyPart();
                    part.setFileName(attachment.filename());
                    part.setDisposition(Part.ATTACHMENT);
                    part.setDataHandler(new DataHandler(
                            new ByteArrayDataSource(attachment.content(), "application/octet-stream")));
                    multipart.addBodyPart(part);
                }
                setContent(multipart);
                saveChanges();
            }
```

`saveChanges()` is required, not optional: `setContent(Multipart)` alone
only calls `setDataHandler(...)` and never touches the `Content-Type`
header — without `saveChanges()`, `message.isMimeType("multipart/mixed")`
(what `HasAttachmentMatcher` checks) would stay `false` even though the
content is set correctly, silently defeating any attachment-based test.
Messages that never call `addAttachment` are unaffected (no content set,
same as before this change).

- [ ] **Step 5: Verify it compiles**

Run: `./gradlew compileTestJava`
Expected: BUILD SUCCESSFUL. (No behavioral test yet — Task 7 exercises this
directly; this step only confirms the new code compiles cleanly before
building `SearchTaskTest` on top of it.)

- [ ] **Step 6: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/mock/MockMessage.java
git commit -m "Add attachment and flag support to MockMessage test fixture"
```

### Task 7: `SearchTask` tests

**Files:**
- Create: `src/test/java/org/ethelred/mymailtool2/SearchTaskTest.java`

- [ ] **Step 1: Write the test file**

```java
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
    public void testAddMatcherWithHasFlagMatcherRunsWithoutError() throws IOException
    {
        MockData data = MockData.getInstance();
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject")
                .addFlag("myflag"));

        SearchTask task = new SearchTask("Folder");
        task.addMatcher(new HasFlagMatcher("myflag"));

        runSearch(task);

        // No exception means addMatcher correctly turned on printFlags() and
        // its m.getFlags() call succeeded end-to-end against a matched message.
    }
}
```

- [ ] **Step 2: Run the tests**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.SearchTaskTest"`
Expected: all 5 tests PASS.

If `testMatchingMessageDownloadsAttachment` fails with a
`MessagingException`/`IOException` around `getContent()` or
`isMimeType`, re-check Task 6's `saveChanges()` call landed correctly — that
was the exact failure mode two rounds of spec review caught.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/SearchTaskTest.java
git commit -m "Add tests for SearchTask"
```

---

## Chunk 3: JS DSL builder and final verification

### Task 8: `JavascriptFileConfiguration$FlagBuilder` coverage via JS fixture

**Files:**
- Modify: `src/test/resources/org/ethelred/mymailtool2/javascript/testjavascript.js`

`FlagBuilder` is only reachable through the JS DSL (`Callback.addFlag(...)`
is the sole constructor call site). `testjavascript.js` is already
exercised end-to-end by two existing tests in `JavascriptConfigurationTest`:
`testLoadJavascript` (loads the file and calls `conf.getTask()`, which is
enough by itself to hit `FlagBuilder`'s constructor, `inFolder`, and
`getOperation` — script evaluation triggers the constructor/`inFolder`
calls immediately, and `getTask()` iterates every deferred builder calling
`getOperation()` on each) and `testJSMain` (additionally runs the built task
against mock data).

Use a folder name (`flagtest`) that no existing assertion in either test
references, so the added rule can't interfere with `testJSMain`'s existing
folder-size assertions — `DefaultContext.getFolder` auto-creates any
folder that doesn't already exist in `MockData`, so an empty `flagtest`
folder is silently created and traversed with no messages, harmlessly.

- [ ] **Step 1: Add one line to the JS fixture**

Add this line to `testjavascript.js` (anywhere after the `config({...})`
block, e.g. at the end of the file):

```javascript
addFlag("myflag").inFolder("flagtest");
```

- [ ] **Step 2: Run the existing Javascript tests**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.javascript.JavascriptConfigurationTest"`
Expected: all existing tests (`testLoadJavascript`, `testJSMain`,
`testOverloaded`) still PASS — `testOverloaded` uses a different JS file
(`overloaded.js`) and is unaffected.

- [ ] **Step 3: Check coverage closed the gap**

Run: `./gradlew test jacocoTestReport`
Then check `build/reports/jacoco/test/html/org.ethelred.mymailtool2.javascript/JavascriptFileConfiguration.FlagBuilder.html`
(or the equivalent line in `build/reports/jacoco/test/jacocoTestReport.xml`)
shows `FlagBuilder` no longer at 0%.

- [ ] **Step 4: Commit**

```bash
git add src/test/resources/org/ethelred/mymailtool2/javascript/testjavascript.js
git commit -m "Exercise FlagBuilder via testjavascript.js fixture"
```

### Task 9: Final verification and coverage report

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew clean test jacocoTestReport`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 2: Recompute per-class coverage for the 8 target classes**

Run:
```bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
tree = ET.parse('build/reports/jacoco/test/jacocoTestReport.xml')
root = tree.getroot()
targets = {
    "SearchTask", "FlagOperation", "ListFoldersTask", "SplitTask",
    "DefaultConfiguration", "HasAttachmentMatcher", "HasFlagMatcher",
    "JavascriptFileConfiguration$FlagBuilder",
}
def line_ratio(el):
    for c in el.findall('counter'):
        if c.get('type') == 'LINE':
            m, c_ = int(c.get('missed')), int(c.get('covered'))
            return c_, m + c_
    return 0, 0
for pkg in root.findall('package'):
    for cls in pkg.findall('class'):
        name = cls.get('name').replace('/', '.').split('.')[-1]
        if name in targets:
            c, t = line_ratio(cls)
            print(f"{name}: {c}/{t} ({100*c/t:.0f}%)" if t else f"{name}: no lines")
c, t = line_ratio(root)
print(f"\nOVERALL: {c}/{t} ({100*c/t:.1f}%)")
EOF
```

Expected: none of the 8 target classes still at 0%; overall line coverage
higher than the 61.6% baseline recorded before this work started.

- [ ] **Step 3: Report results and hand off**

Report the before/after overall coverage percentage and per-class numbers
back to the user, then follow the superpowers:finishing-a-development-branch
skill to decide how to land the branch (this work was scoped as a single PR
per the earlier scoping decision).
