# JUnit 6 / Truth / Mockito Test Migration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all 14 test files in `src/test/java` from JUnit 4 + jmock to JUnit 6 (Jupiter) + Google Truth (assertions) + Mockito (mocking), with no behavior change.

**Architecture:** File-by-file mechanical port. To keep every step independently verifiable, `junit-vintage-engine` is added alongside `junit-jupiter` for the duration of the migration so unconverted (JUnit 4) and converted (JUnit 6) test classes can both run under `useJUnitPlatform()` at the same time. The final task removes JUnit 4 and jmock entirely once every file is converted. Spec: `docs/superpowers/specs/2026-07-29-junit6-truth-mockito-design.md`.

**Tech Stack:** Gradle, Java 17, JUnit 6 (org.junit.jupiter), Google Truth 1.4.4, Mockito 5.14.2 (+ mockito-junit-jupiter), JaCoCo (already configured, untouched).

---

## Chunk 1: Gradle setup + simple/independent test files

### Task 1: Add JUnit 6 / Truth / Mockito dependencies (keep JUnit 4 / jmock temporarily)

**Files:**
- Modify: `build.gradle`

- [ ] **Step 1: Edit `build.gradle` dependencies block**

Replace the existing `dependencies { ... }` block's test-related lines. Current test lines (to keep, for now):

```groovy
    testImplementation group: 'junit', name: 'junit', version: '4.13.2'
    testImplementation group: 'org.jmock', name: 'jmock', version: '2.13.1'
    testImplementation group: 'org.jmock', name: 'jmock-imposters', version: '2.13.1'
```

Add directly below them:

```groovy
    testImplementation platform('org.junit:junit-bom:6.0.0')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
    testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
    testImplementation 'com.google.truth:truth:1.4.4'
    testImplementation 'org.mockito:mockito-core:5.14.2'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.14.2'
```

- [ ] **Step 2: Add `useJUnitPlatform()` to the `test` task**

Find the existing `test { finalizedBy jacocoTestReport }` block (added when JaCoCo was set up) and change it to:

```groovy
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}
```

- [ ] **Step 3: Verify the full (still-JUnit-4) suite still passes under the JUnit Platform via the vintage engine**

Run: `./gradlew clean test`
Expected: BUILD SUCCESSFUL, same test count as before (all tests still JUnit 4 at this point, now executed via junit-vintage-engine).

- [ ] **Step 4: Commit**

```bash
git add build.gradle
git commit -m "Add JUnit 6, Truth, and Mockito dependencies alongside JUnit 4/jmock"
```

---

### Task 2: Migrate `TestUtil.java` and `MapWithDefaultTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/util/TestUtil.java`
- Modify: `src/test/java/org/ethelred/util/MapWithDefaultTest.java`

`TestUtil` is a helper (no `@Test` methods) used only by `CompositeConfigurationTest` (migrated in Task 10). Convert it now since it has no jmock/JUnit-runner dependency itself, just `org.junit.Assert`.

- [ ] **Step 1: Rewrite `TestUtil.java`**

```java
package org.ethelred.util;

import static com.google.common.truth.Truth.assertThat;

public final class TestUtil
{
    public static void assertEmpty(String value)
    {
        assertThat(value).isEmpty();
    }

    public static void assertEmpty(Iterable<?> value)
    {
        assertThat(value).isEmpty();
    }

    public static <T> void assertEquals(Iterable<T> expected, Iterable<T> actual)
    {
        assertThat(actual).containsExactlyElementsIn(expected).inOrder();
    }

    private TestUtil() {
    }
}
```

- [ ] **Step 2: Rewrite `MapWithDefaultTest.java`**

```java
package org.ethelred.util;

import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

public class MapWithDefaultTest
{
    @Test
    public void basicTest()
    {
        Map<String, String> ms = Maps.newHashMap();
        assertThat(ms.get("key")).isNull();
        Supplier<String> def = () -> "def";
        ms = MapWithDefault.wrap(ms, def);
        assertThat(ms.containsKey("key")).isFalse();
        assertThat(ms.get("key")).isEqualTo("def");
        assertThat(ms.containsKey("key")).isTrue();
    }
}
```

- [ ] **Step 3: Run this test class**

Run: `./gradlew test --tests "org.ethelred.util.MapWithDefaultTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 4: Run the full suite to confirm nothing else broke**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/org/ethelred/util/TestUtil.java src/test/java/org/ethelred/util/MapWithDefaultTest.java
git commit -m "Migrate TestUtil and MapWithDefaultTest to JUnit 6 / Truth"
```

---

### Task 3: Migrate `mock/MockTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/mock/MockTest.java`

No jmock usage — straightforward JUnit4 → JUnit6 + Truth port.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2.mock;

import java.util.Properties;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Just tests loading the Mock providers
 */
public class MockTest
{
    @Test
    public void testMockStore() throws MessagingException
    {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imap");
        props.setProperty("mail.debug", "true");
        Session ss = Session.getDefaultInstance(props, new MockAuthenticator());
        Store store = ss.getStore();

        assertThat(store.getClass().getSimpleName()).isEqualTo("MockStore");

        Folder f = store.getDefaultFolder();
        assertThat(f.getName()).isEqualTo("Inbox");
    }
}
```

(Note: `jakarta.mail.NoSuchProviderException` import was unused in the original — dropped.)

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.mock.MockTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/mock/MockTest.java
git commit -m "Migrate MockTest to JUnit 6 / Truth"
```

---

### Task 4: Migrate `MatchOperationTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/MatchOperationTest.java`

First jmock → Mockito conversion. Establishes the `@ExtendWith(MockitoExtension.class)` + `@Mock` pattern used by all subsequent files.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import jakarta.mail.Message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Predicate;

import static org.mockito.Mockito.*;

/**
 * unit test MatchOperation
 */
@ExtendWith(MockitoExtension.class)
public class MatchOperationTest
{
    @Mock Predicate<Message> matcher;
    @Mock MessageOperation operation;
    @Mock MailToolContext mailContext;
    @Mock Message m;

    @Test
    public void testSuccess()
    {
        when(matcher.test(m)).thenReturn(true);
        when(operation.apply(mailContext, m)).thenReturn(true);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
        verify(operation).apply(mailContext, m);
        verify(mailContext).countOperation();
    }


    @Test
    public void testOpFailure()
    {
        when(matcher.test(m)).thenReturn(true);
        when(operation.apply(mailContext, m)).thenReturn(false);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
        verify(operation).apply(mailContext, m);
    }

    @Test
    public void testMatchFailure()
    {
        when(matcher.test(m)).thenReturn(false);
        lenient().when(operation.finishApplying()).thenReturn(true);

        MatchOperation test = new MatchOperation(matcher, operation, 1);
        test.testApply(m, mailContext);

        verify(matcher).test(m);
    }
}
```

Notes:
- `@Mock Predicate<Message> matcher` replaces `context.mock(Predicate.class)` — Mockito's `@Mock` is generically typed, so the old `@SuppressWarnings("unchecked")` cast is no longer needed and is dropped.
- `finishApplying()` was `allowing(...)` (not asserted as called) in all three tests, so it becomes `lenient().when(...)` with no corresponding `verify`.
- `mailContext.countOperation()` was `oneOf(...)` only in `testSuccess`, so only that test gets a `verify(mailContext).countOperation()`.

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.MatchOperationTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/MatchOperationTest.java
git commit -m "Migrate MatchOperationTest to JUnit 6 / Mockito"
```

---

### Task 5: Migrate `MailUtilTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/MailUtilTest.java`

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.lenient;

/**
 * tests for MailUtil
 */
@ExtendWith(MockitoExtension.class)
public class MailUtilTest
{
    @Mock Message m;

    @Test
    public void messageToString() throws MessagingException
    {
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17, 11, 55);
        final Date sentDate = c.getTime();

        lenient().when(m.getSentDate()).thenReturn(sentDate);
        lenient().when(m.getSubject()).thenReturn("test subject");

        assertThat(MailUtil.supplyString(m).get()).isEqualTo("@|cyan 2012-04-17 11:55|@: @|yellow test subject|@");
    }
}
```

Both stubs were `allowing(...)` in the original (never asserted as called), so both use `lenient()`.

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.MailUtilTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/MailUtilTest.java
git commit -m "Migrate MailUtilTest to JUnit 6 / Mockito / Truth"
```

---

### Task 6: Migrate `matcher/MatchersTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/matcher/MatchersTest.java`

Uses named mocks (`context.mock(Message.class, "Message2")`) — Mockito equivalent is `mock(Message.class, "Message2")` (used directly, not via `@Mock`, since we need multiple same-typed fields with custom names) plus a `@BeforeEach` setup. `Address` mocks are hand-built anonymous classes, unrelated to jmock/Mockito — left untouched.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2.matcher;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * test message matchers
 */
public class MatchersTest
{
    Message msg;
    Message msg2;
    Message msg3;

    @BeforeEach
    public void setup()
    {
        msg = mock(Message.class);
        msg2 = mock(Message.class, "Message2");
        msg3 = mock(Message.class, "Message3");
    }

    @Test
    public void testToMatcher()
    {
        final Address[] add1 = mockAddresses("edward@foobar.com");
        final Address[] add2 = mockAddresses();
        final Address[] add3 = null;

        try
        {
            when(msg.getRecipients(Message.RecipientType.TO)).thenReturn(add1);
            when(msg2.getRecipients(Message.RecipientType.TO)).thenReturn(add2);
            when(msg3.getRecipients(Message.RecipientType.TO)).thenReturn(add3);

            Predicate<Message> matcher = new ToAddressMatcher(true, "edward@foobar.com");
            assertThat(matcher.test(msg)).isTrue();

            assertThat(matcher.test(msg2)).isFalse();
            assertThat(matcher.test(msg3)).isFalse();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testSubjectMatcher()
    {
        try
        {
            when(msg.getSubject()).thenReturn("test Subject");
            when(msg2.getSubject()).thenReturn(null);

            Predicate<Message> matcher = new SubjectMatcher(".*subject.*");
            assertThat(matcher.test(msg)).isTrue();
            assertThat(matcher.test(msg2)).isFalse();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    private Address[] mockAddresses(String... addresses)
    {
        Address[] result = new Address[addresses.length];
        for (int i = 0; i < addresses.length; i++)
        {
            result[i] = mockAddress(addresses[i]);
        }
        return result;
    }

    private Address mockAddress(final String address)
    {
        return new Address()
        {
            @Override
            public String getType()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString()
            {
                return address;
            }

            @Override
            public boolean equals(Object o)
            {
                throw new UnsupportedOperationException();
            }
        };
    }
}
```

Note: `testToMatcher`'s three `oneOf(...)` expectations were each called exactly once by the code under test already (one `matcher.test()` call per mock), so plain `when(...)` (no explicit `verify`) preserves the same effective coverage — the return-value assertions on `matcher.test(...)` already prove each stub was exercised. This class is not annotated with `@ExtendWith(MockitoExtension.class)` since mocks are created directly via `mock(...)` in `@BeforeEach` rather than via `@Mock` fields.

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.matcher.MatchersTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/matcher/MatchersTest.java
git commit -m "Migrate MatchersTest to JUnit 6 / Mockito / Truth"
```

---

### Task 7: Migrate `MessageOperationsTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/MessageOperationsTest.java`

Exercises the counted-expectation (`exactly(n).of(...)`) and Hamcrest-argument-matcher (`with(hasItemInArray(...))`) conversion patterns from the spec.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import java.util.Calendar;
import java.util.Date;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * test message operations
 */
@ExtendWith(MockitoExtension.class)
public class MessageOperationsTest
{
    @Mock Message msg;
    @Mock Folder startingFolder;
    @Mock MailToolContext mailContext;
    private Date sentDate;

    @BeforeEach
    public void setup() throws MessagingException
    {
        Calendar c = Calendar.getInstance();
        c.set(2012, Calendar.APRIL, 17);
        sentDate = c.getTime();

        lenient().when(msg.getSentDate()).thenReturn(sentDate);
        lenient().when(msg.getSubject()).thenReturn("test subject");
    }

    @Test
    public void testDelete()
    {
        try
        {
            MessageOperation del = new DeleteOperation();
            assertThat(del.apply(mailContext, msg)).isTrue();
            verify(msg).setFlag(Flags.Flag.DELETED, true);
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }

    @Test
    public void testMove()
    {
        final Folder moveTo = mock(Folder.class, "moveTo");
        final String moveToName = "MoveTo";
        try
        {
            when(msg.getFolder()).thenReturn(startingFolder);
            when(mailContext.getFolder(moveToName)).thenReturn(moveTo);
            when(startingFolder.getFullName()).thenReturn("folder");
            when(moveTo.getFullName()).thenReturn(moveToName);

            MessageOperation move = new MoveOperation(moveToName);
            assertThat(move.apply(mailContext, msg)).isTrue();

            verify(startingFolder).copyMessages(argThat(a -> java.util.Arrays.asList(a).contains(msg)), eq(moveTo));
            verify(msg).setFlag(Flags.Flag.DELETED, true);
            verify(startingFolder).getFullName();
            verify(moveTo).getFullName();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }


    @Test
    public void testSplit()
    {
        final Folder moveTo = mock(Folder.class, "moveTo");
        try
        {
            when(msg.getFolder()).thenReturn(startingFolder);
            when(startingFolder.getSeparator()).thenReturn('.');
            when(startingFolder.getFullName()).thenReturn("folder");
            when(msg.getReceivedDate()).thenReturn(Date.from(LocalDate.of(2012, 4, 8).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            when(mailContext.getFolder("folder.2012.04-Apr-2012")).thenReturn(moveTo);
            when(moveTo.getFullName()).thenReturn("folder.2012.04-Apr-2012");

            MessageOperation split = new SplitOperation();
            assertThat(split.apply(mailContext, msg)).isTrue();

            verify(startingFolder).copyMessages(argThat(a -> java.util.Arrays.asList(a).contains(msg)), eq(moveTo));
            verify(msg).setFlag(Flags.Flag.DELETED, true);
            verify(startingFolder, times(2)).getFullName();
            verify(moveTo).getFullName();
        }
        catch (MessagingException e)
        {
            fail("unexpected exception");
        }
    }
}
```

Notes:
- `msg.getSentDate()`/`msg.getSubject()` in `setup()` were `allowing(...)` and not exercised by every test (`testDelete` doesn't call either) → `lenient()` in `@BeforeEach`, otherwise Mockito's strict stubbing would fail `testDelete` with `UnnecessaryStubbingException`.
- `startingFolder.getFullName()`/`moveTo.getFullName()` were genuine `oneOf(...)` expectations in the original, and they are actually exercised: `MoveOperation.apply` (`MoveOperation.java:33`) and `SplitOperation.apply` (`SplitOperation.java:35`) both pass `startingFolder::getFullName` and `moveTo::getFullName` as lazy log4j2 `Supplier` arguments to `LOGGER.info(...)`, which is invoked since the root logger level is INFO — so both are called exactly once in `testMove`. Converted to plain `when(...)` + `verify(...)`, matching the `oneOf → when()+verify()` rule.
- `testSplit`'s `startingFolder.getFullName()` is called twice: once inside `SplitOperation.getSubFolderName` (`SplitOperation.java:62`) and once via the same lazy log supplier (`SplitOperation.java:35`) — matching the original `exactly(2).of(startingFolder).getFullName()` → `verify(startingFolder, times(2)).getFullName()`. `moveTo.getFullName()` is called once (log supplier only) → plain `when(...)` + `verify(moveTo).getFullName()`.
- The Hamcrest `with(hasItemInArray(msg))` + `with(equal(moveTo))` becomes `argThat(a -> Arrays.asList(a).contains(msg))` + `eq(moveTo)` — both arguments must be matchers together (Mockito's all-or-nothing rule).

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.MessageOperationsTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/MessageOperationsTest.java
git commit -m "Migrate MessageOperationsTest to JUnit 6 / Mockito / Truth"
```

---

## Chunk 2: Remaining files + cutover

### Task 8: Migrate `MainTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/MainTest.java`

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import org.ethelred.util.ClockFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 test for main app class
 */
@ExtendWith(MockitoExtension.class)
public class MainTest
{
    @Mock MailToolConfiguration conf;

    @Test
    public void testOperationLimit()
    {
        ClockFactory.setClock(LocalDate.of(2014, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(conf.getOperationLimit()).thenReturn(3);
        when(conf.getTimeLimit()).thenReturn("50 days");
        lenient().when(conf.verbose()).thenReturn(false);

        MailToolContext app = new DefaultContext(conf);
        app.countOperation();
        app.countOperation();
        app.countOperation();
        try
        {
            app.countOperation();
            fail("expected OperationLimitException");
        }
        catch (OperationLimitException e)
        {
            // expected - success
        }

        verify(conf, times(2)).getOperationLimit();
        verify(conf, times(1)).getTimeLimit();
    }
}
```

Note: unused `jakarta.mail.Message`/`MessagingException` imports and `assertFalse`/`assertTrue` static imports from the original were dropped (never referenced in the test body).

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.MainTest"`
Expected: BUILD SUCCESSFUL, 1 test passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/MainTest.java
git commit -m "Migrate MainTest to JUnit 6 / Mockito"
```

---

### Task 9: Migrate `TaskBaseTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/TaskBaseTest.java`

No jmock usage — only JUnit4 lifecycle annotations and `assertEquals`.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * tests for base functionality
 */
public class TaskBaseTest
{
    private static final Logger LOGGER = LogManager.getLogger();
    MailToolContext mockContext;

    @BeforeEach
    public void setup()
    {

        MockData data = MockData.getInstance();
        data.addFolder("Folder");
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));
        data.addMessage("Folder", MockMessage.create("2012-12-12", "foo@example.com", "test subject"));


        mockContext = new DefaultContext(new MockDefaultConfiguration());
        mockContext.connect();
    }

    @AfterEach
    public void cleanup()
    {
        mockContext.disconnect();
        MockData.clear();
    }

    @Test
    public void testShortcutSimple() throws IOException, MessagingException
    {
        MockTaskBase tb = new MockTaskBase();
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);
        assertThat(tb.messageCounter).isEqualTo(3);

    }


    @Test
    public void testNoShortcut() throws IOException, MessagingException
    {
        MockTaskBase tb = new MockTaskBase();
        tb.giveUpAfter = Integer.MAX_VALUE;
        tb.init(mockContext);
        tb.traverseFolder("Folder", false, true);
        assertThat(tb.messageCounter).isEqualTo(MockData.getInstance().folderSize("Folder"));

    }

    private class MockTaskBase extends TaskBase
    {
        int giveUpAfter = 1;
        int messageCounter;
        @Override
        protected void runMessage(Folder f, Message m) throws MessagingException, IOException
        {
            LOGGER.info("Check message {}", messageCounter);
            if (messageCounter++ > giveUpAfter)
            {
                throw new ShortcutFolderScanException();
            }
        }

        @Override
        protected void status(Folder f)
        {

            LOGGER.info("Status folder {}", f);
        }

        @Override
        public void run()
        {

        }
    }
}
```

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.TaskBaseTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/TaskBaseTest.java
git commit -m "Migrate TaskBaseTest to JUnit 6 / Truth"
```

---

### Task 10: Migrate `CompositeConfigurationTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/CompositeConfigurationTest.java`

Depends on `TestUtil` (already migrated in Task 2). Uses named jmock mocks with a plain `Mockery` (no imposteriser needed since `MailToolConfiguration` is an interface).

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.ethelred.util.TestUtil;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.ethelred.util.TestUtil.assertEmpty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class CompositeConfigurationTest
{
    @Test
    public void testEmptyConfiguration()
    {
        MailToolConfiguration empty = new CompositeConfiguration();
        assertThat(empty.getOperationLimit()).isEqualTo(-1);
        assertThat(empty.getUser()).isNull();
        assertThat(empty.getMinAge()).isNull();
        assertThat(empty.getPassword()).isNull();
        assertEmpty(empty.getFileLocations());
        assertEmpty(empty.getFileHandlers());
    }

    @Test
    public void testSingleConfiguration()
    {

        MailToolConfiguration mock = new MailToolConfiguration()
        {
            @Override
            public String getPassword()
            {
                return "password";
            }

            @Override
            public Map<String, String> getMailProperties()
            {
                return Map.of("test", "mail");
            }

            @Override
            public String getUser()
            {
                return "user";
            }

            @Override
            public Iterable<String> getFileLocations()
            {
                return Lists.newArrayList("file1");
            }

            @Override
            public Task getTask() throws Exception
            {
                return null;
            }

            @Override
            public int getOperationLimit()
            {
                return 1000;
            }

            @Override
            public String getMinAge()
            {
                return "3 months";
            }

            @Override
            public Iterable<FileConfigurationHandler> getFileHandlers()
            {
                return Collections.emptyList();
            }

            @Override
            public String getTimeLimit()
            {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean verbose()
            {
                return false;
            }

            @Override
            public int getChunkSize()
            {
                return PRIMITIVE_DEFAULT;
            }

            @Override
            public boolean randomTraversal() {
                return false;
            }
        };

        MailToolConfiguration comp = new CompositeConfiguration(mock);
        assertThat(comp.getOperationLimit()).isEqualTo(mock.getOperationLimit());
        assertThat(comp.getUser()).isEqualTo(mock.getUser());
        assertThat(comp.getMinAge()).isEqualTo(mock.getMinAge());
        assertThat(comp.getPassword()).isEqualTo(mock.getPassword());
        TestUtil.assertEquals(mock.getFileLocations(), comp.getFileLocations());
        TestUtil.assertEquals(mock.getFileHandlers(), comp.getFileHandlers());
    }

    @Test
    public void testInsertion()
    {
        final Iterable<String> testFileLocs = ImmutableList.of("loc1", "loc2");
        final Iterable<String> testFileLocs2 = ImmutableList.of("ins1");
        final MailToolConfiguration mockDefault = mock(MailToolConfiguration.class, "MTCdefault");
        final MailToolConfiguration mockFile1 = mock(MailToolConfiguration.class, "MTCfile1");
        final MailToolConfiguration mockFile2 = mock(MailToolConfiguration.class, "MTCfile2");
        Map<String, MailToolConfiguration> mockFiles = ImmutableMap.of(
                "loc1", mockFile1,
                "ins1", mockFile2
        );
        CompositeConfiguration cmp = new CompositeConfiguration(mockDefault);

        when(mockDefault.getFileLocations()).thenReturn(testFileLocs);
        when(mockFile1.getFileLocations()).thenReturn(testFileLocs2);
        when(mockFile2.getFileLocations()).thenReturn(Collections.emptyList());

        int counter = 0;
        for (String filename : cmp.getFileLocations())
        {
            MailToolConfiguration fileConf = mockFiles.get(filename);
            if (fileConf != null)
            {
                cmp.insert(fileConf);
            }
            counter++;
            assertThat(counter).isLessThan(5);
        }

        verify(mockDefault).getFileLocations();
        verify(mockFile1).getFileLocations();
        verify(mockFile2).getFileLocations();
    }
}
```

Note: `mockFile1`/`mockFile2`'s `getFileLocations()` stubs are only exercised if `cmp.getFileLocations()` actually yields `"loc1"`/`"ins1"` and triggers `cmp.insert(...)` — same conditional-execution shape as the original jmock `oneOf(...)` expectations, so no `lenient()` needed as long as the loop always visits both.

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.CompositeConfigurationTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/CompositeConfigurationTest.java
git commit -m "Migrate CompositeConfigurationTest to JUnit 6 / Mockito / Truth"
```

---

### Task 11: Migrate `RecentMessageIterableTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/RecentMessageIterableTest.java`

No jmock usage.

- [ ] **Step 1: Rewrite the file**

Keep the file identical except: `import org.junit.Before/After/Test` → `org.junit.jupiter.api.BeforeEach/AfterEach/Test`, `@Before`→`@BeforeEach`, `@After`→`@AfterEach`, and `import static org.junit.Assert.assertEquals` + all `assertEquals(...)` calls → `import static com.google.common.truth.Truth.assertThat` + `assertThat(actual).isEqualTo(expected)`.

```java
package org.ethelred.mymailtool2;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by edward on 3/3/14.
 */
public class RecentMessageIterableTest
{
    MailToolContext context;

    @BeforeEach
    public void setup()
    {
        MockData data = MockData.getInstance();

        data.addMessage("Folder", MockMessage.create("2013-01-01", "foo@example.com", "1"));
        data.addMessage("Folder", MockMessage.create("2013-01-02", "foo@example.com", "2"));
        data.addMessage("Folder", MockMessage.create("2013-01-03", "foo@example.com", "3"));


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 202; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("Large", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        context = new DefaultContext(new MockDefaultConfiguration());
        context.connect();
    }

    @AfterEach
    public void reset()
    {
        context.disconnect();
        MockData.clear();
    }

    @Test
    public void testNewestFirstSmall() throws MessagingException
    {
        Iterable<Message> it = new RecentMessageIterable(context.getFolder("Folder"), true);
        List<String> subjects = Lists.newArrayList();
        for (Message m : it)
        {
            subjects.add(m.getSubject());
        }

        assertThat(Joiner.on(",").join(subjects)).isEqualTo("3,2,1");
    }

    @Test
    public void testNewestFirstLarge() throws MessagingException
    {
        Iterable<Message> it = new RecentMessageIterable(context.getFolder("Large"), true);
        List<String> subjects = Lists.newArrayList();
        for (Message m : it)
        {
            subjects.add(m.getSubject());
        }

        assertThat(Joiner.on(",").join(subjects)).isEqualTo(
                "202,201,200,199,198,197,196,195,194,193,192,191,190,189,188,187,186,185,184,183,182,181,180,179,"
                        + "178,177,176,175,174,173,172,171,170,169,168,167,166,165,164,163,162,161,160,159,158,157,"
                        + "156,155,154,153,152,151,150,149,148,147,146,145,144,143,142,141,140,139,138,137,136,135,"
                        + "134,133,132,131,130,129,128,127,126,125,124,123,122,121,120,119,118,117,116,115,114,113,"
                        + "112,111,110,109,108,107,106,105,104,103,102,101,100,99,98,97,96,95,94,93,92,91,90,89,88,"
                        + "87,86,85,84,83,82,81,80,79,78,77,76,75,74,73,72,71,70,69,68,67,66,65,64,63,62,61,60,59,58,"
                        + "57,56,55,54,53,52,51,50,49,48,47,46,45,44,43,42,41,40,39,38,37,36,35,34,33,32,31,30,29,28,"
                        + "27,26,25,24,23,22,21,20,19,18,17,16,15,14,13,12,11,10,9,8,7,6,5,4,3,2,1");
    }

    @Test
    public void testOldestFirstSmall() throws MessagingException
    {
        Iterable<Message> it = new RecentMessageIterable(context.getFolder("Folder"), false);
        List<String> subjects = Lists.newArrayList();
        for (Message m : it)
        {
            subjects.add(m.getSubject());
        }

        assertThat(Joiner.on(",").join(subjects)).isEqualTo("1,2,3");
    }

    @Test
    public void testOldestFirstLarge() throws MessagingException
    {
        Iterable<Message> it = new RecentMessageIterable(context.getFolder("Large"), false);
        List<String> subjects = Lists.newArrayList();
        for (Message m : it)
        {
            subjects.add(m.getSubject());
        }

        assertThat(Joiner.on(",").join(subjects)).isEqualTo(
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,"
                        + "36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,"
                        + "66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,"
                        + "96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,"
                        + "119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,"
                        + "141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,"
                        + "163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,"
                        + "185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202");
    }
}
```

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.RecentMessageIterableTest"`
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/RecentMessageIterableTest.java
git commit -m "Migrate RecentMessageIterableTest to JUnit 6 / Truth"
```

---

### Task 12: Migrate `ApplyMatchOperationsTaskTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/ApplyMatchOperationsTaskTest.java`

Only uses jmock imports (`Mockery`, `ByteBuddyClassImposteriser`) but never actually creates a mock via them — those imports can simply be dropped.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2;

import java.util.function.Predicate;
import org.ethelred.util.Predicates;
import com.google.common.collect.Lists;
import org.ethelred.mymailtool2.matcher.AgeMatcher;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.ethelred.util.ClockFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import jakarta.mail.Message;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 *
 */
public class ApplyMatchOperationsTaskTest
{

    @Test
    public void testGlobalMinAge()
    {
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        TrackMatchMessageOperation msgop = new TrackMatchMessageOperation();
        MatchOperation mo = new MatchOperation(new Predicate<Message>()
        {
            @Override
            public boolean test(@Nullable Message message)
            {
                return true;
            }
        }, msgop, 0);

        //task.addRule("INBOX", mo, false);
    }

    private class TrackMatchMessageOperation implements MessageOperation
    {
        private List<Message> matches = Lists.newArrayList();

        @Override
        public boolean apply(MailToolContext context, Message m)
        {
            matches.add(m);
            return true;
        }

        @Override
        public boolean finishApplying()
        {
            return true;
        }
    }

    @AfterEach
    public void reset()
    {
        MockData.clear();
    }

    @Test
    public void testSimpleShortcut()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertThat(data.folderSize("F1")).isEqualTo(5);
        assertThat(data.folderSize("F2")).isEqualTo(-1);
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age = new AgeMatcher("3 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age), List.of(age), new MoveOperation("F2"), false);
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


        assertThat(data.folderSize("F1")).isEqualTo(3);
        assertThat(data.folderSize("F2")).isEqualTo(2);
        assertThat(((DefaultContext) context).messageCheckedCount).isEqualTo(3);
    }

    @Test
    public void testMultipleShortcut()
    {
        MockData data = MockData.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }

        ClockFactory.setClock(c.getTimeInMillis());


        assertThat(data.folderSize("F1")).isEqualTo(5);
        assertThat(data.folderSize("F2")).isEqualTo(-1);
        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> age1 = new AgeMatcher("4 days", true, task);
        Predicate<Message> age2 = new AgeMatcher("2 days", true, task);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age1), List.of(age1), new MoveOperation("F2"), false);
        task.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age2), List.of(age2), new MoveOperation("F3"), false);
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


        assertThat(data.folderSize("F1")).isEqualTo(2);
        assertThat(data.folderSize("F2")).isEqualTo(1);
        assertThat(data.folderSize("F3")).isEqualTo(2);
        assertThat(((DefaultContext) context).messageCheckedCount).isEqualTo(4);
    }

}
```

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.ApplyMatchOperationsTaskTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/ApplyMatchOperationsTaskTest.java
git commit -m "Migrate ApplyMatchOperationsTaskTest to JUnit 6 / Truth"
```

---

### Task 13: Migrate `javascript/JavascriptConfigurationTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/javascript/JavascriptConfigurationTest.java`

No jmock usage. Drop the unused `org.junit.Ignore` import (never applied to a method).

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2.javascript;

import java.io.File;
import java.net.URL;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.FileConfigurationHandler;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * test loading a js file
 */
public class JavascriptConfigurationTest
{
    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }


    @Test
    public void testLoadJavascript() throws Exception
    {
        FileConfigurationHandler h = new JavascriptFileConfigurationHandler();

        URL testFileLocation = this.getClass().getResource("testjavascript.js");
        assertThat(testFileLocation).isNotNull();
        File f = new File(testFileLocation.getFile());

        MailToolConfiguration conf = h.readConfiguration(f);

        assertThat(conf).isNotNull();
        assertThat(conf.getUser()).isEqualTo("edward");
        assertThat(conf.getMinAge()).isEqualTo("3 months");
        assertThat(conf.getOperationLimit()).isEqualTo(300);
        assertThat(conf.getMailProperties().get("mail.store.protocol")).isEqualTo("imap");

        Task t = conf.getTask();
        assertThat(t).isNotNull();
        assertThat(t).isInstanceOf(ApplyMatchOperationsTask.class);
    }

    @Test
    public void testJSMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new JavascriptFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testjavascript.js").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("test", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world world"));
        data.addMessage("spamtest", MockMessage.create("2012-01-01", "from1@example.com", "Hello world").addHeader("X-Spam-Score", "1.5"));
        data.addMessage("spamtest", MockMessage.create("2012-01-01", "from1@example.com", "Hello world 2").addHeader("X-Spam-Score", "2.6"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(-1);
        assertThat(data.folderSize("test")).isEqualTo(2);
        assertThat(data.folderSize("repeated")).isEqualTo(-1);
        assertThat(data.folderSize("spamtest")).isEqualTo(2);
        assertThat(data.folderSize("spamscore")).isEqualTo(-1);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(0);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(1);
        assertThat(data.folderSize("test")).isEqualTo(1);
        assertThat(data.folderSize("repeated")).isEqualTo(1);
        assertThat(data.folderSize("spamtest")).isEqualTo(1);
        assertThat(data.folderSize("spamscore")).isEqualTo(1);
    }



    @Test
    public void testOverloaded() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new JavascriptFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("overloaded.js").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "banana@fruit.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "cheddar@cheese.com", "Hello subject2 world"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.javascript.JavascriptConfigurationTest"`
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/javascript/JavascriptConfigurationTest.java
git commit -m "Migrate JavascriptConfigurationTest to JUnit 6 / Truth"
```

---

### Task 14: Migrate `propertiesfile/PropertiesFileConfigurationTest.java`

**Files:**
- Modify: `src/test/java/org/ethelred/mymailtool2/propertiesfile/PropertiesFileConfigurationTest.java`

No jmock usage. The original file also had two unused imports (`java.io.IOException` and `org.ethelred.mymailtool2.javascript.JavascriptFileConfigurationHandler`, neither referenced in the file body) — dropped below along with the JUnit4 imports.

- [ ] **Step 1: Rewrite the file**

```java
package org.ethelred.mymailtool2.propertiesfile;

import java.io.File;

import org.ethelred.mymailtool2.ApplyMatchOperationsTask;
import org.ethelred.mymailtool2.MailToolConfiguration;
import org.ethelred.mymailtool2.Main;
import org.ethelred.mymailtool2.Task;
import org.ethelred.mymailtool2.mock.MockData;
import org.ethelred.mymailtool2.mock.MockDefaultConfiguration;
import org.ethelred.mymailtool2.mock.MockMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 *
 */
public class PropertiesFileConfigurationTest
{
    File confFile;

    @BeforeEach
    public void setup()
    {
        confFile = new File(getClass().getResource("testproperties.properties").getFile());
    }

    @AfterEach
    public void cleanup()
    {
        MockData.clear();
    }

    @Test
    public void testProperties() throws Exception
    {
        MailToolConfiguration conf = new PropertiesFileConfiguration(confFile);
        assertThat(conf.getOperationLimit()).isEqualTo(300);

        Task t = conf.getTask();
        assertThat(t).isNotNull();
        assertThat(t).isInstanceOf(ApplyMatchOperationsTask.class);

    }

    @Test
    public void testPropertiesMain() throws Exception
    {
        MockDefaultConfiguration conf = new MockDefaultConfiguration();
        conf.addFileHandler(new PropertiesFileConfigurationHandler());
        conf.addFile(this.getClass().getResource("testproperties.properties").getFile());

        MockData data = MockData.getInstance();
        data.addFolder("Inbox");
        data.addFolder("archive");
        data.addFolder("test");

        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject1 world"));
        data.addMessage("Inbox", MockMessage.create("2012-01-01", "from1@example.com", "Hello subject2 world"));

        assertThat(data.folderSize("Inbox")).isEqualTo(3);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(-1);

        Main main = new Main();
        main.setDefaultConfiguration(conf);
        main.init(new String[]{});
        main.run();


        assertThat(data.folderSize("Inbox")).isEqualTo(0);
        assertThat(data.folderSize("archive")).isEqualTo(0);
        assertThat(data.folderSize("archive.2012.01-Jan-2012")).isEqualTo(3);
    }
}
```

- [ ] **Step 2: Run this test class**

Run: `./gradlew test --tests "org.ethelred.mymailtool2.propertiesfile.PropertiesFileConfigurationTest"`
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/org/ethelred/mymailtool2/propertiesfile/PropertiesFileConfigurationTest.java
git commit -m "Migrate PropertiesFileConfigurationTest to JUnit 6 / Truth"
```

---

## Chunk 3: Cutover and PR

### Task 15: Remove JUnit 4 / jmock and do final verification

**Files:**
- Modify: `build.gradle`

All 14 files are now converted. Remove the JUnit 4/jmock dependencies and the vintage engine that were kept temporarily for a safe incremental migration.

- [ ] **Step 1: Edit `build.gradle`**

Remove these three lines (added originally, still present from before Task 1):

```groovy
    testImplementation group: 'junit', name: 'junit', version: '4.13.2'
    testImplementation group: 'org.jmock', name: 'jmock', version: '2.13.1'
    testImplementation group: 'org.jmock', name: 'jmock-imposters', version: '2.13.1'
```

Remove this line (added in Task 1, no longer needed once nothing runs on JUnit 4):

```groovy
    testRuntimeOnly 'org.junit.vintage:junit-vintage-engine'
```

- [ ] **Step 2: Confirm no JUnit 4 / jmock references remain**

Run: `grep -rl "org.junit.Test\|org.junit.Before\|org.junit.After\|org.junit.Assert\|org.junit.Ignore\|jmock" src/test/java`
Expected: no output (empty result).

- [ ] **Step 3: Run the full test suite**

Run: `./gradlew clean test`
Expected: BUILD SUCCESSFUL, all test classes run under `junit-jupiter` only (no vintage engine involved), same total test count as the original JUnit 4 suite: 29 tests across the 13 `@Test`-bearing classes (`TestUtil.java` is a helper with no tests of its own) — 1(MapWithDefaultTest) + 1(MockTest) + 3(MatchOperationTest) + 1(MailUtilTest) + 2(MatchersTest) + 3(MessageOperationsTest) + 1(MainTest) + 2(TaskBaseTest) + 3(CompositeConfigurationTest) + 4(RecentMessageIterableTest) + 3(ApplyMatchOperationsTaskTest) + 3(JavascriptConfigurationTest) + 2(PropertiesFileConfigurationTest) = 29.

- [ ] **Step 4: Confirm JaCoCo report still generates correctly**

Run: `ls build/reports/jacoco/test/html/index.html build/reports/jacoco/test/jacocoTestReport.xml`
Expected: both files exist.

- [ ] **Step 5: Commit**

```bash
git add build.gradle
git commit -m "Remove JUnit 4 and jmock now that the test suite runs on JUnit 6"
```

- [ ] **Step 6: Push and open a PR**

```bash
git push -u origin test/junit6-truth-mockito
gh pr create --title "Migrate test suite to JUnit 6, Truth, and Mockito" --body "$(cat <<'EOF'
## Summary
- Migrates all 14 test files from JUnit 4 + jmock to JUnit 6 (Jupiter) + Google Truth + Mockito
- No test behavior changes intended — same assertions, same mock expectations, just via the new APIs
- See docs/superpowers/specs/2026-07-29-junit6-truth-mockito-design.md for the full design

## Test plan
- [x] ./gradlew clean test passes with all tests migrated
- [x] No org.junit.Test/Assert/jmock references remain in src/test/java
- [x] JaCoCo coverage report still generates
EOF
)"
```

---

## Verification Summary

After Task 15, the entire suite should be running purely on JUnit 6:
```bash
./gradlew clean test
grep -rl "org.junit.Test\|jmock" src/test/java   # expect empty
```
And the JaCoCo report added earlier continues to work unmodified, now measuring coverage of a JUnit 6-based suite.
