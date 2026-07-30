# Test coverage for zero-coverage classes

## Context

After the JUnit 6 / Truth / Mockito migration (PR #139), overall JaCoCo line
coverage is 61.6% (branch 52.2%). Eight classes have 0% line coverage:

| Class | Lines |
|---|---|
| `SearchTask` | 81 |
| `DefaultConfiguration` | 17 |
| `FlagOperation` | 16 |
| `HasAttachmentMatcher` | 19 |
| `SplitTask` | 17 |
| `ListFoldersTask` | 13 |
| `HasFlagMatcher` | 11 |
| `JavascriptFileConfiguration$FlagBuilder` | 8 |

This work adds tests for these eight classes only. Partial-coverage classes
(e.g. `CommandLineConfiguration` at 21%, `Main` at 60%) are out of scope for
this pass.

Goal is meaningful behavioural coverage, not 100% — trivial code (plain
getters returning a stored field, `toString()`) is not worth testing for its
own sake.

## Approach

Follow the testing conventions already established in the codebase (JUnit 6,
Google Truth, Mockito) and match each class to the existing pattern used for
similar classes, rather than inventing a new style:

### `FlagOperation` (`MessageOperation`)

Mockito-based unit test, same shape as `MessageOperationsTest.java`/
`MatchOperationTest.java` (`@ExtendWith(MockitoExtension.class)`, `@Mock
Message`). New test class `FlagOperationTest.java`.

Cases:
- flag already in the desired state (add=true + already present, or
  add=false + already absent) → no-op, returns `false`, `setFlags` never
  called
- flag toggled (add=true + absent, or add=false + present) → returns `true`,
  `setFlags` called with expected args
- `MessagingException` from `getFlags()` → returns `false`

Skip: `toString()`.

### `HasAttachmentMatcher`, `HasFlagMatcher` (`Predicate<Message>`)

New test methods added to the existing `matcher/MatchersTest.java`, reusing
its established Mockito `Message` mock pattern (`mock(Message.class, "...")`,
`when(...)`, try/catch `MessagingException`/`fail(...)`).

`HasAttachmentMatcher` cases:
- null message → `false`
- non-`multipart/mixed` message → `false`
- multipart with matching attachment filename (regex) → `true`
- multipart with attachment present but filename doesn't match pattern →
  `false`
- multipart with a body part that isn't an attachment (no/blank disposition
  or filename) → `false`
- `MessagingException`/`IOException` while reading content → `false`

`HasFlagMatcher` cases:
- null message → `false`
- flag present → `true`
- flag absent → `false`
- `MessagingException` → `false`

Skip: `toString()`.

### `SearchTask`, `ListFoldersTask`, `SplitTask` (`TaskBase` subclasses)

New test classes, one per class, using the existing hand-rolled
`MockData`/`MockMessage`/`MockDefaultConfiguration`/`DefaultContext`
fixture — the same framework `TaskBaseTest` and
`ApplyMatchOperationsTaskTest` use — driving the real `run()` /
`traverseFolder()` flow rather than mocking `Folder`/`Message` directly.
This is the established pattern for task-level (as opposed to
operation-level) tests in this codebase.

`SearchTaskTest`:
- message matches predicate → logged as a match (verify via side effects
  reachable through the mock framework, e.g. attachment download / flag
  printing paths below — not log output itself)
- message doesn't match → no match processing
- `addMatcher` composes multiple matchers with AND
- `addMatcher` with a `HasAttachmentMatcher` turns on attachment printing;
  with a `HasFlagMatcher` turns on flag printing
- attachment download: attachment written to `outputDirectory` when set;
  skipped if the target file already exists
- recursive vs non-recursive traversal (via `setRecursive`)

Skip: exact log message formats, `flagToString` for every enum value
individually (one representative case is enough), the `printFlags` console
`System.out.println()` call itself.

`ListFoldersTaskTest`:
- `run()` traverses from `context.getDefaultFolder()` without reading
  messages (folder message counts unaffected)

`SplitTaskTest`:
- `run()` traverses the given folder in read-write mode, applies
  `SplitOperation` to each message, and counts messages/operations via the
  context — mirrors the existing `ApplyMatchOperationsTaskTest` shortcut
  tests in style

### `DefaultConfiguration`

New test class `DefaultConfigurationTest.java`, no mocking — direct
instantiation and Truth assertions, but limited to getters with actual
behaviour rather than every constant:
- `getTask()` returns an `ApplyMatchOperationsTask`
- `getFileLocations()` contains the expected home-directory and `/etc` paths
- `getFileHandlers()` contains a `PropertiesFileConfigurationHandler`

Skip: plain constant getters (`getPassword`, `getUser`, `getOperationLimit`,
`getMinAge`, `getTimeLimit`, `verbose`, `getChunkSize`,
`randomTraversal`, `toString`) — these return literals with no branching
logic.

### `JavascriptFileConfiguration$FlagBuilder`

Extend the existing JS fixture (`testjavascript.js`, exercised via
`testJSMain` in `JavascriptConfigurationTest`) with an `addFlag(...)`/
`inFolder(...)` DSL call, exercised end-to-end — the same way sibling
builders (`MoveBuilder`, `SplitBuilder`, `DeleteBuilder`) already reach 100%
coverage, without a dedicated unit test.

## Non-goals

- No changes to partial-coverage classes.
- No pursuit of 100% coverage on any of the eight classes — defensive/
  trivial code is left untested where testing it wouldn't catch a real bug.
- No production code changes except where a genuine bug is found while
  writing tests (would be called out separately, not assumed).

## Testing

Standard: `./gradlew test jacocoTestReport`, all tests green, re-check the
per-class coverage numbers for the eight target classes afterward and report
back before/after.
