# MOSIP Demo SDK

[![Maven Package upon a push](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml/badge.svg?branch=develop)](https://github.com/mosip/demosdk/actions/workflows/push-trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=mosip_demosdk&metric=alert_status)](https://sonarcloud.io/dashboard?id=mosip_demosdk)

The **Demo SDK** is the reference implementation of **demographic authentication** for
[MOSIP ID-Authentication](https://github.com/mosip/id-authentication). ID-Authentication uses it to
compare names, addresses and other demographic values in an authentication request against the values
stored for a resident.

It is a plain Java **library** (no main class, no server). You add it as a Maven dependency.

---

## Contents

- [At a glance](#at-a-glance)
- [What it does](#what-it-does)
- [Quick start](#quick-start)
- [Using it in your project](#using-it-in-your-project)
- [Configuration](#configuration)
- [Build, test and coverage](#build-test-and-coverage)
- [Test coverage](#test-coverage)
- [Project layout](#project-layout)
- [Dependencies](#dependencies)
- [Release and publishing](#release-and-publishing)
- [Contributing](#contributing)
- [License](#license)

---

## At a glance

| Item | Value |
|---|---|
| Maven coordinates | `io.mosip.demosdk:demosdk` (version: see `demosdk/pom.xml`) |
| Java | 21 |
| Parent POM | `org.springframework.boot:spring-boot-starter-parent:4.1.1` |
| MOSIP dependency | `io.mosip.kernel:kernel-core`, version set by `kernel.core.version` in `pom.xml` (no `kernel-bom`) |
| Implements | `IDemoApi`, `IDemoNormalizer` (from `kernel-core`) |
| Test coverage | **100 %** line, branch and instruction ([details](#test-coverage)). The build fails below 90 %. |
| License | [MPL 2.0](LICENSE) |

---

## What it does

The SDK has two Spring beans. Each implements one MOSIP SPI interface.

### 1. Matching — `Client_V_1_0` (implements `IDemoApi`)

Every method returns a **score from 0 to 100**. 100 means a full match.

| Method | How the score is computed | Example |
|---|---|---|
| `doExactMatch` | Lower-case both values and split on whitespace. Returns 100 when both have the same tokens in any order, otherwise 0. | `"John Doe"` vs `"doe JOHN"` → **100** |
| `doPartialMatch` | `matched × 100 / (entityTokens + unmatchedRequestTokens)`. A one-letter request token (an initial) cancels its penalty if a stored token starts with that letter. | `"J Doe"` vs `"John Doe"` → **50** |
| `doPhoneticsMatch` | Encodes both values with BeiderMorse, then scores the Soundex difference: `(difference + 1) × 20`. | `"Smith"` vs `"Smyth"` → **≥ 80** |

> `doPhoneticsMatch` expects a BeiderMorse language name such as `english`. An unknown language
> throws `IllegalArgumentException`.

### 2. Normalization — `Normalizer_V_1_0` (implements `IDemoNormalizer`)

Values are cleaned up before matching:

- **`normalizeName`** removes titles such as `Mr`, `Dr` (the caller supplies them per language), then applies
  the configured regex rules.
- **`normalizeAddress`** applies the configured regex rules.

Rules come from Spring properties. See [Configuration](#configuration).

---

## Quick start

**Prerequisites:** JDK 21, Maven 3.9+ and the `kernel-core` version named in `pom.xml`. If it isn't in your
local Maven repository, Maven downloads it from Central snapshots. You can also build it from
[mosip/commons](https://github.com/mosip/commons):

```bash
cd ../commons/kernel/kernel-core && mvn clean install -Dgpg.skip=true -DskipTests
```

Then build and install the SDK:

```bash
git clone https://github.com/mosip/demosdk.git
cd demosdk/demosdk

./run-local.sh all        # Linux / macOS / Git Bash
run-local.bat all         # Windows cmd
```

`all` packages the JAR, runs tests with the coverage gate, and installs the JAR into `~/.m2`.

---

## Using it in your project

```xml
<dependency>
    <groupId>io.mosip.demosdk</groupId>
    <artifactId>demosdk</artifactId>
    <version>${demosdk.version}</version><!-- use the latest published version -->
</dependency>
```

Component-scan `io.mosip.demosdk.client` and inject the SPI interfaces:

```java
@Autowired private IDemoApi demoApi;
@Autowired private IDemoNormalizer demoNormalizer;

String stored  = demoNormalizer.normalizeName(storedName,  "eng", titles);
String request = demoNormalizer.normalizeName(requestName, "eng", titles);
int score = demoApi.doPartialMatch(request, stored, Map.of());
```

A self-contained `demosdk-<version>-jar-with-dependencies.jar` is also built, for deployments that load the
SDK from a loader path.

---

## Configuration

`Normalizer_V_1_0` reads regex rules from the active Spring `Environment`. In MOSIP this is usually
`id-authentication-default.properties`.

**Key format**

```properties
ida.demo.<type>.normalization.regex.<language>[<index>]=<regex><sep><replacement>
```

| Part | Allowed values |
|---|---|
| `type` | `name`, `address`, or `common` (applied to both names and addresses) |
| `language` | Language code (for example `eng`) or `any` for every language |
| `index` | `0`, `1`, `2` … Reading stops at the first missing index (maximum 1000). |
| `sep` | Value of `ida.norm.sep`. Default `=`. |
| `replacement` | Optional. If it's missing, matches are deleted. |

**Order in which rules are applied:** `type/language` → `type/any` → `common/language` → `common/any`.

**Example**

```properties
ida.norm.sep==
ida.demo.address.normalization.regex.eng[0]=\\bstreet\\b=st
ida.demo.address.normalization.regex.eng[1]=\\bapartment\\b=apt
ida.demo.common.normalization.regex.any[0]=[\\.,]=
ida.demo.common.normalization.regex.any[1]=\\s+= 
```

Patterns are compiled with `Pattern.UNICODE_CHARACTER_CLASS`, so `\w` and `\s` work for non-Latin scripts.

---

## Build, test and coverage

All commands run from `demosdk/demosdk/` (where `pom.xml` lives).

### Local runner

| Command | What it does |
|---|---|
| `init` | Package the JAR (skip tests) |
| `test` | Run unit tests |
| `coverage` | Tests, JaCoCo report and 90 % gate, with a per-class summary |
| `install` | Install the JAR into the local Maven repository |
| `javadoc` | Generate API docs in `target/reports/apidocs` |
| `deps` | Write the dependency tree to `.local/logs/deps.txt` |
| `sonar` | Sonar analysis (needs `SONAR_TOKEN`, optional `SONAR_ORG`) |
| `clean` | Remove `target/` and `.local/` |
| `all` | `init` + `coverage` + `install` |

```bash
./run-local.sh coverage      # or: run-local.bat coverage
```

Optional environment variables: `MAVEN_REPO` (custom local repository), `MVN_ARGS` (extra Maven arguments).

### Plain Maven

```bash
mvn clean install -Dgpg.skip=true -Dmaven.javadoc.skip=true   # build + install
mvn test                                                      # tests only
mvn test -Dtest=ClientV1UnitTest                              # one test class
mvn verify -Dgpg.skip=true                                    # tests + 90 % coverage gate
mvn verify -Psonar -Dgpg.skip=true                            # SonarCloud analysis
```

See [Test coverage](#test-coverage) for results, the quality gate and how to read the reports.

---

## Test coverage

### Summary

Every class that counts toward coverage is fully covered.

| Counter | Covered | Coverage |
|---|---|---|
| Lines | 111 / 111 | **100 %** |
| Branches | 38 / 38 | **100 %** |
| Instructions | 530 / 530 | **100 %** |
| Methods | 24 / 24 | **100 %** |
| Classes | 3 / 3 | **100 %** |
| **Sonar "Coverage"** (lines + branches) | 149 / 149 | **100 %** (target ≥ 90 %) |

These figures come from the last local `mvn verify` run. Re-run `./run-local.sh coverage` to get current numbers.

### Per class

| Class | Lines | Branches | Methods | Tests |
|---|---|---|---|---|
| `Client_V_1_0` | 38 / 38 | 12 / 12 | 14 / 14 | `ClientV1UnitTest` (17) |
| `Normalizer_V_1_0` | 66 / 66 | 26 / 26 | 9 / 9 | `NormalizerV1UnitTest` (15) |
| `TextMatcherUtil` | 7 / 7 | — (no branches) | 1 / 1 | `TextMatcherUtilTest` (5) |
| `LoggerConfig` | excluded | excluded | excluded | — |

The suite has **37 tests** and runs in about 2 seconds.

### What the tests check

Tests assert **exact scores and exact output strings**, not just "no exception".

**Matching (`ClientV1UnitTest`)**

| Area | Cases covered |
|---|---|
| Exact match | Identical values; different word order; mixed case and extra spaces; different token counts; same count with one different token; two empty strings |
| Partial match | 1 of 2 tokens (50); all tokens (100); initial only (0); initial plus a word (50); initial with no matching word (33); unmatched word (33); duplicated request token counted once (50) |
| Phonetic match | Delegates to `TextMatcherUtil` (static mock); `EncoderException` is logged and returns 0; real encoding of identical names returns 100 |
| Lifecycle | `init()` does not throw |

**Normalization (`NormalizerV1UnitTest`)**

| Area | Cases covered |
|---|---|
| Titles | Title removed; with a trailing dot and in upper, lower and original case; longest title removed first (`Mrs` before `Mr`); titles of another language ignored |
| Rule loading | `type/lang`, `type/any`, `common/lang` and `common/any` all applied; indexed rules read until the first gap; reading stops at the 1000-entry limit |
| Rule format | No separator deletes matches; empty replacement deletes matches; custom `ida.norm.sep` honoured |
| Replacement engine | All matches replaced; a replacement is never re-matched; a zero-length match does not loop; Unicode character classes work (`José Müller`); output is trimmed |

**Phonetics (`TextMatcherUtilTest`)**

| Area | Cases covered |
|---|---|
| Scoring | Identical strings return 100; similar spellings (`Smith` / `Smyth`) score ≥ 80; every score is a multiple of 20 between 20 and 100 |
| Errors | `null` input throws `IllegalArgumentException`; an unknown BeiderMorse language throws `IllegalArgumentException` |

### Quality gate

The gate is enforced in two places:

| Where | Rule | Fails when |
|---|---|---|
| JaCoCo `check` (`mvn verify`, `run-local coverage`) | `BUNDLE` instruction **and** line covered ratio ≥ `jacoco.coverage.ratio` (0.90) | Either ratio drops below 90 %. The build stops. |
| SonarCloud (`-Psonar`, CI `sonar_analysis` job) | Reads `target/site/jacoco/jacoco.xml` via `sonar.coverage.jacoco.xmlReportPaths` | The project quality gate on SonarCloud is not met |

Sonar computes **Coverage = (covered lines + covered branches) / (total lines + total branches)**. The JaCoCo
gate checks lines and instructions, so passing it locally reliably keeps Sonar above 90 %.

To change the threshold, edit `jacoco.coverage.ratio` in `demosdk/pom.xml`.

### What is excluded and why

| Pattern | Excluded from | Reason |
|---|---|---|
| `**/config/**` (`LoggerConfig`) | JaCoCo, Sonar coverage, Sonar duplication | Static logger wiring with no logic. Testing it would create log files on disk. |

Exclusions are set once in `pom.xml`: `sonar.coverage.exclusions`, `sonar.cpd.exclusions` and the JaCoCo
`<excludes>` block. Keep all three in sync.

### Reading the reports

| Report | Path (after `mvn verify`) |
|---|---|
| HTML, browsable per class and line | `demosdk/target/site/jacoco/index.html` |
| XML (used by Sonar) | `demosdk/target/site/jacoco/jacoco.xml` |
| CSV (used by `run-local coverage` summary) | `demosdk/target/site/jacoco/jacoco.csv` |
| Test results | `demosdk/target/surefire-reports/` |

`run-local coverage` prints a per-class summary at the end:

```text
coverage (excludes **/config/**)
  TextMatcherUtil      line 100%  branch  n/a
  Normalizer_V_1_0     line 100%  branch 100%
  Client_V_1_0         line 100%  branch 100%
  TOTAL                line 100%  branch 100%  instruction 100%
```

### Known behaviour not covered by a test

- **Mixed-case titles hang `normalizeName`.** A title such as `Sr` never finishes removing an input like
  `"sR John"`. The loop checks case-insensitively but deletes only the original, lower-case and upper-case
  forms. No test runs this input because the test would never finish.
- **Unsupported phonetic language.** `doPhoneticsMatch` does not catch the `IllegalArgumentException` from
  BeiderMorse. The exception is tested at the `TextMatcherUtil` level.

### Adding tests

- Use JUnit Jupiter and Mockito, both version-managed in `pom.xml`.
- Inject a mocked `Environment` into `Normalizer_V_1_0` with `@Mock` and `@InjectMocks`
  (see `NormalizerV1UnitTest`).
- Mock `TextMatcherUtil` statically with `try (MockedStatic<TextMatcherUtil> m = mockStatic(...))`
  (see `ClientV1UnitTest`).
- Assert exact scores and strings. Run `./run-local.sh coverage` before opening a PR.

---

## Project layout

```
demosdk/                              repository root
├── demosdk/                          Maven module (pom.xml)
│   ├── pom.xml
│   ├── run-local.bat / run-local.sh
│   └── src/
│       ├── main/java/io/mosip/demosdk/client/
│       │   ├── impl/spec_1_0/
│       │   │   ├── Client_V_1_0.java        IDemoApi implementation
│       │   │   └── Normalizer_V_1_0.java    IDemoNormalizer implementation
│       │   ├── utils/TextMatcherUtil.java   BeiderMorse + Soundex scoring
│       │   └── config/LoggerConfig.java     MOSIP rolling-file logger
│       └── test/java/…                      JUnit Jupiter + Mockito tests
├── licenses/                         Full license texts + copyright notices
├── NOTICE · THIRD-PARTY-NOTICES      Attribution
├── AGENTS.md                         Compact guide for AI coding agents
└── LICENSE                           MPL 2.0
```

---

## Dependencies

| Artifact | Why | License |
|---|---|---|
| `io.mosip.kernel:kernel-core` | `IDemoApi`, `IDemoNormalizer`, MOSIP `Logfactory` | MPL-2.0 |
| `org.springframework:spring-context` 7.0.x | `@Service`, `@Component`, `Environment` | Apache-2.0 |
| `commons-codec:commons-codec` 1.22.1 | BeiderMorse and Soundex | Apache-2.0 |
| `org.junit.jupiter:junit-jupiter` *(test)* | Unit tests | EPL-2.0 |
| `org.mockito:mockito-*` *(test)* | Mocks | MIT |

Spring Boot 4.1.1 manages all versions. `kernel-bom`, `kernel-demographics-api` and
`kernel-logger-logback` are **not** used: `kernel-core` already includes them. See
[THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) for the full list, including transitive dependencies.

---

## Release and publishing

- Artifacts are signed with GPG (`.github/keys/`) and published to Maven Central with
  `central-publishing-maven-plugin`.
- `autoPublish` is `false`, so releases must be promoted manually in the Sonatype portal.
- Snapshots go to `https://central.sonatype.com/repository/maven-snapshots/`.
- CI: [push-trigger.yml](.github/workflows/push-trigger.yml) (build, publish, Sonar via `mosip/kattu`).

---

## Contributing

- [How to contribute code to MOSIP](https://docs.mosip.io/community/code-contributions)
- Questions: [MOSIP Community](https://community.mosip.io/)
- Bugs and feature requests: [GitHub issues](https://github.com/mosip/demosdk/issues)
- Documentation: [docs.mosip.io](https://docs.mosip.io/)

Before opening a PR, run `./run-local.sh coverage`. The build fails if coverage drops below 90 %.

---

## License

This project is licensed under the [Mozilla Public License 2.0](LICENSE).
Third-party attributions are in [NOTICE](NOTICE), [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) and [licenses/](licenses/).
