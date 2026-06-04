# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MOSIP Demo SDK is a Java library implementing demographic authentication for the [MOSIP ID-Authentication](https://github.com/mosip/id-authentication) subsystem. It is consumed as a Maven dependency — it has no runnable entry point of its own.

The SDK implements two interfaces from `kernel-demographics-api`:
- `IDemoApi` — demographic data matching (exact, partial, phonetic)
- `IDemoNormalizer` — name and address string normalization driven by Spring Environment properties

## Build Commands

All commands run from the `demosdk/` subdirectory (where `pom.xml` lives):

```bash
# Build and install to local Maven repo (skip GPG signing and Javadoc for local dev)
mvn clean install -Dmaven.javadoc.skip=true -Dgpg.skip=true

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClientV1UnitTest
mvn test -Dtest=NormalizerV1UnitTest
mvn test -Dtest=TextMatcherUtilTest

# Run Sonar analysis (requires sonar credentials)
mvn verify -Psonar
```

The surefire plugin passes `--enable-preview` and several `--add-opens` flags automatically — no extra JVM args are needed when running tests through Maven.

## Architecture

```
demosdk/src/main/java/io/mosip/demosdk/client/
├── impl/spec_1_0/
│   ├── Client_V_1_0.java       # IDemoApi implementation
│   └── Normalizer_V_1_0.java   # IDemoNormalizer implementation
├── utils/
│   └── TextMatcherUtil.java    # Phonetic matching (BeiderMorse + Soundex)
└── config/
    └── LoggerConfig.java       # MOSIP rolling-file logger setup
```

### Matching Logic (`Client_V_1_0`)

All three match methods return an integer 0–100:

- **Exact match**: tokenizes both strings by whitespace (lowercased), returns 100 only if token sets are equal (order-insensitive).
- **Partial match**: `matchedTokens * 100 / (entityTokenCount + unmatchedRefTokenCount)`. Single-character ref tokens may match any entity token that starts with that character.
- **Phonetic match**: delegates to `TextMatcherUtil.phoneticsMatch`, which encodes both strings with Apache BeiderMorse (`PhoneticEngine`) then scores the Soundex difference: `(soundexDifference + 1) * 20` → range 20–100.

### Normalization (`Normalizer_V_1_0`)

Patterns are loaded lazily from Spring `Environment` properties using the key template:

```
ida.demo.<type>.normalization.regex.<language>[<index>]
```

where `type` is `name`, `address`, or `common`; `language` is the BCP 47 tag or `any`. Each value is `<regex>=<replacement>` (separator configurable via `ida.norm.sep`, default `=`). Indices are iterated 0–999 and stop at the first missing key.

`normalizeName` additionally strips title prefixes (e.g., "Mr", "Dr") supplied by the caller before applying regex patterns. `normalizeWithCommonAttributes` always merges patterns for the specific language + `any` + `common/<language>` + `common/any`.

### Testing Patterns

Tests use JUnit 4 + Mockito 5. Because `Normalizer_V_1_0.environment` is `@Autowired` (private field), tests inject a mocked `Environment` via reflection — see `NormalizerV1UnitTest.setUp()`. Static methods in `TextMatcherUtil` are mocked with `mockStatic` from Mockito's `MockedStatic` API.

## Key Dependencies

| Artifact | Purpose |
|---|---|
| `kernel-demographics-api:1.3.0` | `IDemoApi` and `IDemoNormalizer` interfaces |
| `kernel-logger-logback:1.3.0` | MOSIP Logback wrapper (`Logfactory`) |
| `commons-codec` | Soundex and BeiderMorse phonetic encoding |
| `spring-web` / `spring-core` | `Environment` injection in normalizer |
| `jackson-databind`, `jackson-dataformat-xml` | JSON/XML support pulled in transitively |

## Configuration Properties

The normalizer reads from whatever Spring `Environment` is active (typically `id-authentication-default.properties` in the consuming service). Relevant property keys:

- `ida.demo.<type>.normalization.regex.<language>[<n>]` — normalization pattern at index `n`
- `ida.norm.sep` — separator between regex and replacement (default `=`)

## Release / Publishing

Artifacts are signed with GPG (key in `.github/keys/`) and published to Maven Central via `central-publishing-maven-plugin`. The `autoPublish` flag is `false`, so promotion to release must be done manually in the Sonatype portal. Snapshot builds go to `https://central.sonatype.com/repository/maven-snapshots`.