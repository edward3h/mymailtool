# Migrate test suite to JUnit 6, Truth, and Mockito

## Context

The project's test suite (14 test classes under `src/test/java`) currently uses JUnit 4
(`org.junit.Test`, `org.junit.Assert`) and jmock (with `ByteBuddyClassImposteriser`) for
mocking. JaCoCo coverage reporting was recently added (`build.gradle`), and before investing
in raising coverage, the test code should be brought up to current preferences: JUnit 6
(JUnit Jupiter) as the test framework/runner, [Truth](https://truth.dev/) for assertions, and
Mockito for mocking, replacing jmock.

This is a mechanical migration of existing tests, not a rewrite of test behaviour — each test
should assert the same things it does today, just through the new APIs.

## Scope

All 14 test files are migrated together in one branch/PR (no incremental coexistence of
JUnit 4 and JUnit 6):

- `ApplyMatchOperationsTaskTest`
- `CompositeConfigurationTest`
- `javascript/JavascriptConfigurationTest`
- `MailUtilTest`
- `MainTest`
- `matcher/MatchersTest`
- `MatchOperationTest`
- `MessageOperationsTest`
- `mock/MockTest`
- `propertiesfile/PropertiesFileConfigurationTest`
- `RecentMessageIterableTest`
- `TaskBaseTest`
- `util/MapWithDefaultTest`
- `util/TestUtil.java` (test helper, not a test class itself)

**Out of scope:** the hand-rolled Jakarta Mail test doubles in `src/test/.../mock/`
(`MockStore`, `MockFolder`, `MockMessage`, `MockData`, `MockAuthenticator`,
`MockDefaultConfiguration`) are left untouched — they aren't jmock- or JUnit-based, they model
real Jakarta Mail API behaviour (folder/message state), and replacing them with Mockito stubs
would lose that state-modelling clarity for no benefit. Adding new tests / raising coverage is
explicit follow-up work, not part of this migration.

## Dependency changes (`build.gradle`)

Remove:
- `testImplementation group: 'junit', name: 'junit', version: '4.13.2'`
- `testImplementation group: 'org.jmock', name: 'jmock', version: '2.13.1'`
- `testImplementation group: 'org.jmock', name: 'jmock-imposters', version: '2.13.1'`

Add:
- `testImplementation platform('org.junit:junit-bom:6.0.0')`
- `testImplementation 'org.junit.jupiter:junit-jupiter'`
- `testRuntimeOnly 'org.junit.platform:junit-platform-launcher'`
- `testImplementation 'com.google.truth:truth:1.4.4'`
- `testImplementation 'org.mockito:mockito-core:5.14.2'`
- `testImplementation 'org.mockito:mockito-junit-jupiter:5.14.2'`

And switch the `test` task to run on the JUnit Platform:

```groovy
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}
```

(JaCoCo and `jacocoTestReport` are unaffected — JaCoCo instruments bytecode regardless of test
framework.)

## Conversion patterns

Applied consistently across all files:

| Old (JUnit 4 / jmock) | New (JUnit 6 / Mockito / Truth) |
|---|---|
| `import org.junit.Test` / `@Test` | `import org.junit.jupiter.api.Test` / `@Test` |
| `import org.junit.Before` / `@Before` | `import org.junit.jupiter.api.BeforeEach` / `@BeforeEach` |
| `import org.junit.After` / `@After` | `import org.junit.jupiter.api.AfterEach` / `@AfterEach` |
| `Mockery context = new Mockery(){{setImposteriser(...);}}` field + manual `context.mock(X.class)` calls | `@ExtendWith(MockitoExtension.class)` on the class + `@Mock X x;` fields (Mockito mocks interfaces and concrete classes natively — no imposteriser needed) |
| `context.checking(new Expectations(){{ oneOf(x).method(args); will(returnValue(v)); }})` | `when(x.method(args)).thenReturn(v);` for stubbing, plus `verify(x).method(args);` to assert the call happened |
| `allowing(x).method(args); will(returnValue(v));` | `lenient().when(x.method(args)).thenReturn(v);` (or plain `when(...)` if always exercised) — no `verify` since the call isn't required |
| `context.assertIsSatisfied();` | removed — Mockito's `@Mock`/strict stubbing plus explicit `verify(...)` calls cover this; add `verifyNoMoreInteractions(x)` only where a test's current exhaustiveness is actually load-bearing |
| `import static org.junit.Assert.*;` + `assertEquals/assertTrue/assertNull/...` | `import static com.google.common.truth.Truth.assertThat;` (note the package is `com.google.common.truth`, not `com.google.truth`, despite the Maven coordinate being `com.google.truth:truth`) + `assertThat(actual).isEqualTo(expected)` / `.isTrue()` / `.isNull()` / etc. |
| `TestUtil.assertEmpty(...)`, `TestUtil.assertEquals(Iterable, Iterable)` | Same method signatures kept (call sites unchanged), but reimplemented internally using Truth: `assertThat(value).isEmpty()`, `assertThat(actual).containsExactlyElementsIn(expected).inOrder()` |
| `exactly(n).of(x).method(args); will(returnValue(v));` (counted expectation, e.g. `MainTest.java:34-35`, `MessageOperationsTest.java:103`) | `when(x.method(args)).thenReturn(v);` for stubbing, `verify(x, times(n)).method(args);` for the count assertion |
| `oneOf(x).method(with(hamcrestMatcher1), with(hamcrestMatcher2));` (Hamcrest arg matchers via jmock's `with(...)`, e.g. `MessageOperationsTest.java:78,107` — `with(hasItemInArray(msg))`, `with(equal(moveTo))`) | `verify(x).method(argThat(...), eq(...));` using Mockito's `ArgumentMatchers`. **Gotcha:** once any argument in a call uses a Mockito matcher, *every* argument in that call must use a matcher (mixing raw values and matchers throws `InvalidUseOfMatchersException`) — wrap plain-value args in `eq(...)` |
| `import org.junit.Ignore` / `@Ignore` (appears imported but unused in `JavascriptConfigurationTest.java`) | `import org.junit.jupiter.api.Disabled` / `@Disabled` if ever applied; since it's currently unused, just drop the import |

Example — `MatchOperationTest.testSuccess()` before/after:

```java
// before
context.checking(new Expectations(){{
    oneOf(matcher).test(m); will(returnValue(true));
    oneOf(operation).apply(mailContext, m); will(returnValue(true));
    allowing(operation).finishApplying(); will(returnValue(true));
    oneOf(mailContext).countOperation();
}});
MatchOperation test = new MatchOperation(matcher, operation, 1);
test.testApply(m, mailContext);
context.assertIsSatisfied();

// after
when(matcher.test(m)).thenReturn(true);
when(operation.apply(mailContext, m)).thenReturn(true);
lenient().when(operation.finishApplying()).thenReturn(true);
MatchOperation test = new MatchOperation(matcher, operation, 1);
test.testApply(m, mailContext);
verify(matcher).test(m);
verify(operation).apply(mailContext, m);
verify(mailContext).countOperation();
```

## Verification

- `./gradlew clean test` passes fully under the JUnit Platform.
- `build/reports/jacoco/test/html/index.html` still generates with the same class set covered.
- No jmock/JUnit 4 imports remain: `grep -rl "org.junit.Test\|org.junit.Assert\|jmock" src/test/java` returns nothing.
- Spot-check that Mockito's strict stubbing doesn't flag `UnnecessaryStubbingException` for
  `allowing(...)`-derived stubs that aren't exercised on every path — use `lenient()` for those
  rather than suppressing strictness suite-wide.

## Notes for the implementer

- Required static imports per migrated file (in addition to `com.google.common.truth.Truth.assertThat`):
  `org.mockito.Mockito.*` (for `when`, `verify`, `times`, `lenient`, etc.) and, where argument
  matchers are used, `org.mockito.ArgumentMatchers.*` (for `argThat`, `eq`, `any`, etc.).
- Drop now-unnecessary `@SuppressWarnings("unchecked")` annotations on mock fields/casts (e.g.
  `MatchOperationTest.java:22`) where Mockito's `@Mock` no longer requires the raw-type cast
  that jmock's `context.mock(Predicate.class)` did.
- `MockitoExtension` runs with strict stubbing by default (`Strictness.STRICT_STUBS`), which is
  desirable — it surfaces unused stubs as errors rather than silently ignoring them, closer in
  spirit to jmock's `assertIsSatisfied()`. Only reach for `lenient()` on stubs that are
  genuinely optional across branches, not as a blanket fix for strictness failures.
