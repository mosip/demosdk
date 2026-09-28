# demosdk (Maven module)

Reference demographic matching and normalization library for
[MOSIP ID-Authentication](https://github.com/mosip/id-authentication).

| | |
|---|---|
| Coordinates | `io.mosip.demosdk:demosdk` (version in `pom.xml`) |
| Parent | `spring-boot-starter-parent` 4.1.1 · Java 21 · no `kernel-bom` |
| Implements | `IDemoApi` → `Client_V_1_0`, `IDemoNormalizer` → `Normalizer_V_1_0` |
| Needs | `io.mosip.kernel:kernel-core` (`kernel.core.version` in `pom.xml`) |

## Build

```bash
./run-local.sh all          # Linux / macOS / Git Bash
run-local.bat all           # Windows cmd
```

Other commands: `init`, `test`, `coverage`, `install`, `javadoc`, `deps`, `sonar`, `clean`.
Run a script without arguments to see the full list.

## Configuration

Normalization rules are Spring properties:

```properties
ida.demo.<name|address|common>.normalization.regex.<language|any>[<index>]=<regex>=<replacement>
ida.norm.sep==
```

See the [root README](../README.md#configuration) for the full rules and examples, and
`id-authentication-default.properties` in the MOSIP config repository for the defaults.
