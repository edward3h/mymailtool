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
import org.junit.jupiter.api.io.TempDir;

import javax.annotation.Nullable;
import jakarta.mail.Message;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

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

    // --- Stage 8: scan cache wiring ---------------------------------------------------------

    private static final String FP1 = "fp-1";
    private static final String FP2 = "fp-2";
    private static final String ACCOUNT_KEY = "unknown@";

    private MockDefaultConfiguration configWithStateFile(File stateFile)
    {
        return new MockDefaultConfiguration()
        {
            @Override
            public String getScanStateFile()
            {
                return stateFile.getAbsolutePath();
            }
        };
    }

    private MockDefaultConfiguration configWithScanCacheDisabled(File stateFile)
    {
        return new MockDefaultConfiguration()
        {
            @Override
            public String getScanStateFile()
            {
                return stateFile.getAbsolutePath();
            }

            @Override
            public boolean disableScanCache()
            {
                return true;
            }
        };
    }

    private void runTask(ApplyMatchOperationsTask task, MailToolContext context)
    {
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
    public void testFirstRunPopulatesScanState(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration config = configWithStateFile(stateFile);

        ApplyMatchOperationsTask task = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll = new AgeMatcher("0 days", true, task);
        task.addRule("F1", matchAll, List.of(matchAll), new MoveOperation("F2"), false);

        MailToolContext context = new DefaultContext(config, FP1);
        runTask(task, context);

        assertThat(data.folderSize("F1")).isEqualTo(0);
        assertThat(data.folderSize("F2")).isEqualTo(5);
        assertThat(((DefaultContext) context).messageCheckedCount).isEqualTo(5);

        ScanState state = ScanState.loadOrEmpty(stateFile);
        Optional<ScanState.FolderScanState> stored = state.get(ACCOUNT_KEY, "f1");
        assertThat(stored.isPresent()).isTrue();
        assertThat(stored.get().nextUid()).isEqualTo(6L);
    }

    @Test
    public void testSecondRunNoNewMailChecksZero(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration config = configWithStateFile(stateFile);

        ApplyMatchOperationsTask task1 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll1 = new AgeMatcher("0 days", true, task1);
        task1.addRule("F1", matchAll1, List.of(matchAll1), new MoveOperation("F2"), false);
        MailToolContext context1 = new DefaultContext(config, FP1);
        runTask(task1, context1);
        assertThat(((DefaultContext) context1).messageCheckedCount).isEqualTo(5);

        // second run, same folder + state file + fingerprint, no new mail in between
        ApplyMatchOperationsTask task2 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll2 = new AgeMatcher("0 days", true, task2);
        task2.addRule("F1", matchAll2, List.of(matchAll2), new MoveOperation("F2"), false);
        MailToolContext context2 = new DefaultContext(config, FP1);
        runTask(task2, context2);

        assertThat(((DefaultContext) context2).messageCheckedCount).isEqualTo(0);
    }

    @Test
    public void testSecondRunWithNewMailChecksOnlyNewMessages(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration config = configWithStateFile(stateFile);

        ApplyMatchOperationsTask task1 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll1 = new AgeMatcher("0 days", true, task1);
        task1.addRule("F1", matchAll1, List.of(matchAll1), new MoveOperation("F2"), false);
        MailToolContext context1 = new DefaultContext(config, FP1);
        runTask(task1, context1);
        assertThat(((DefaultContext) context1).messageCheckedCount).isEqualTo(5);
        assertThat(data.folderSize("F2")).isEqualTo(5);

        // add 2 new messages after the first run, then advance the clock so they're not "too new"
        int newMessageCount = 2;
        for (int i = 0; i < newMessageCount; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", "new" + i));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        ApplyMatchOperationsTask task2 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll2 = new AgeMatcher("0 days", true, task2);
        task2.addRule("F1", matchAll2, List.of(matchAll2), new MoveOperation("F2"), false);
        MailToolContext context2 = new DefaultContext(config, FP1);
        runTask(task2, context2);

        assertThat(((DefaultContext) context2).messageCheckedCount).isEqualTo(newMessageCount);
        assertThat(data.folderSize("F2")).isEqualTo(5 + newMessageCount);
    }

    @Test
    public void testFingerprintChangeForcesFullRescan(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration config = configWithStateFile(stateFile);

        // Rule matches everything but does not move the messages, so a leftover, unmoved
        // population remains in F1 after run 1: this is what lets us tell "resumed, 0 checked"
        // apart from "rescanned in full" in run 2.
        ApplyMatchOperationsTask task1 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll1 = new AgeMatcher("0 days", true, task1);
        task1.addRule("F1", matchAll1, List.of(matchAll1), new TrackMatchMessageOperation(), false);
        MailToolContext context1 = new DefaultContext(config, FP1);
        runTask(task1, context1);
        assertThat(((DefaultContext) context1).messageCheckedCount).isEqualTo(5);
        assertThat(data.folderSize("F1")).isEqualTo(5);

        // second run, same folder + state file, but a different fingerprint: cache must be
        // treated as invalid, forcing a full rescan of the still-present 5 messages
        ApplyMatchOperationsTask task2 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll2 = new AgeMatcher("0 days", true, task2);
        task2.addRule("F1", matchAll2, List.of(matchAll2), new TrackMatchMessageOperation(), false);
        MailToolContext context2 = new DefaultContext(config, FP2);
        runTask(task2, context2);

        assertThat(((DefaultContext) context2).messageCheckedCount).isEqualTo(5);
    }

    @Test
    public void testDisableScanCacheAlwaysFullRescan(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis());

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration cacheEnabledConfig = configWithStateFile(stateFile);

        // First run: normal cache-enabled context, establishes a valid resume point.
        ApplyMatchOperationsTask task1 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll1 = new AgeMatcher("0 days", true, task1);
        task1.addRule("F1", matchAll1, List.of(matchAll1), new TrackMatchMessageOperation(), false);
        MailToolContext context1 = new DefaultContext(cacheEnabledConfig, FP1);
        runTask(task1, context1);
        assertThat(((DefaultContext) context1).messageCheckedCount).isEqualTo(5);
        assertThat(data.folderSize("F1")).isEqualTo(5);

        // Second run: same state file, same fingerprint, but scan cache disabled -- must still
        // check every message despite a valid, matching cache entry existing on disk.
        MockDefaultConfiguration cacheDisabledConfig = configWithScanCacheDisabled(stateFile);
        ApplyMatchOperationsTask task2 = ApplyMatchOperationsTask.create();
        Predicate<Message> matchAll2 = new AgeMatcher("0 days", true, task2);
        task2.addRule("F1", matchAll2, List.of(matchAll2), new TrackMatchMessageOperation(), false);
        MailToolContext context2 = new DefaultContext(cacheDisabledConfig, FP1);
        runTask(task2, context2);

        assertThat(((DefaultContext) context2).messageCheckedCount).isEqualTo(5);
    }

    @Test
    public void testAgeMatcherShortcutMessageIsReExaminedOnceOldEnough(@TempDir File tempDir)
    {
        MockData data = MockData.getInstance();
        data.setUidCapable("F1", true);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.set(2013, Calendar.JANUARY, 1);
        // messages dated 2013-01-02 .. 2013-01-06, UIDs 1..5 in that same order
        for (int i = 1; i <= 5; i++)
        {
            c.add(Calendar.DATE, 1);
            data.addMessage("F1", MockMessage.create(dateFormat.format(c.getTime()), "foo@example.com", String.valueOf(i)));
        }
        ClockFactory.setClock(c.getTimeInMillis()); // clock = 2013-01-06

        File stateFile = new File(tempDir, "scan-state.properties");
        MockDefaultConfiguration config = configWithStateFile(stateFile);

        // Run 1: age >= 3 days matches (moves to F2). Oldest-first traversal:
        //   msg1 (01-02, age 4d) matches -> moved
        //   msg2 (01-03, age 3d) matches -> moved
        //   msg3 (01-04, age 2d) too new -> ShortcutFolderScanException, scan stops here
        // msg3 is therefore "considered but not resolved": it must remain available for a
        // future run once it becomes old enough, per the plan's central correctness claim.
        ApplyMatchOperationsTask task1 = ApplyMatchOperationsTask.create();
        Predicate<Message> age1 = new AgeMatcher("3 days", true, task1);
        task1.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age1), List.of(age1), new MoveOperation("F2"), false);
        MailToolContext context1 = new DefaultContext(config, FP1);
        runTask(task1, context1);

        assertThat(data.folderSize("F1")).isEqualTo(3); // msg3, msg4, msg5 remain
        assertThat(data.folderSize("F2")).isEqualTo(2); // msg1, msg2 moved
        assertThat(((DefaultContext) context1).messageCheckedCount).isEqualTo(3);

        // The persisted resume point must be AT msg3 (UID 3, the unresolved shortcut message),
        // not past it and not the folder's full UIDNEXT (6).
        ScanState stateAfterRun1 = ScanState.loadOrEmpty(stateFile);
        Optional<ScanState.FolderScanState> storedAfterRun1 = stateAfterRun1.get(ACCOUNT_KEY, "f1");
        assertThat(storedAfterRun1.isPresent()).isTrue();
        assertThat(storedAfterRun1.get().nextUid()).isEqualTo(3L);

        // Advance the clock by 2 days (to 2013-01-08) so msg3 and msg4 are now old enough
        // (age 4d and 3d respectively); msg5 (age 2d) is still too new and will shortcut again.
        c.add(Calendar.DATE, 2);
        ClockFactory.setClock(c.getTimeInMillis());

        ApplyMatchOperationsTask task2 = ApplyMatchOperationsTask.create();
        Predicate<Message> age2 = new AgeMatcher("3 days", true, task2);
        task2.addRule("F1", Predicates.and(Predicates.alwaysTrue(), age2), List.of(age2), new MoveOperation("F2"), false);
        MailToolContext context2 = new DefaultContext(config, FP1);
        runTask(task2, context2);

        // (a) msg3 and msg4, previously blocked by the shortcut, are now correctly re-examined
        // and matched/moved -- proving the cache did not skip them.
        assertThat(data.folderSize("F1")).isEqualTo(1); // only msg5 remains
        assertThat(data.folderSize("F2")).isEqualTo(4); // msg1, msg2 (run 1) + msg3, msg4 (run 2)

        // (b) only the previously-unresolved tail (msg3, msg4, msg5) is re-checked in run 2;
        // msg1 and msg2, already fully resolved and moved in run 1, are correctly NOT re-checked.
        assertThat(((DefaultContext) context2).messageCheckedCount).isEqualTo(3);

        // The resume point now moves to msg5 (UID 5), the new unresolved shortcut message.
        ScanState stateAfterRun2 = ScanState.loadOrEmpty(stateFile);
        Optional<ScanState.FolderScanState> storedAfterRun2 = stateAfterRun2.get(ACCOUNT_KEY, "f1");
        assertThat(storedAfterRun2.isPresent()).isTrue();
        assertThat(storedAfterRun2.get().nextUid()).isEqualTo(5L);
    }

}
