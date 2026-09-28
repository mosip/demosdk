# demosdk

```
demosdk · JDK21 · Boot 4.1.1 parent · lib jar · NO kernel-bom · versions only in pom.xml
├─ run   cd demosdk && run-local.{bat,sh} init|test|coverage|install|javadoc|deps|sonar|clean|all
│        mvn test -Dtest=<Class>        mvn verify -Dgpg.skip=true  (jacoco ≥90% gate)
├─ dep   kernel-core ${kernel.core.version} = IDemoApi+IDemoNormalizer+Logfactory
│        ban kernel-bom · kernel-demographics-api · kernel-logger-logback · repackage
├─ src   demosdk/src/main/java/io/mosip/demosdk/client/
│  ├─ impl/spec_1_0/Client_V_1_0      IDemoApi → 0..100
│  │  ├─ exact    ws-tokens lowercase, same size, any order → 100|0
│  │  ├─ partial  matched*100/(entity+unmatchedRef); 1-char ref = initial
│  │  └─ phonetic TextMatcherUtil; unknown BM lang → IAE (uncaught)
│  ├─ impl/spec_1_0/Normalizer_V_1_0  IDemoNormalizer, @Autowired Environment
│  │  ├─ key  ida.demo.<name|address|common>.normalization.regex.<lang|any>[i]
│  │  ├─ val  <regex><ida.norm.sep|=><repl>; stop at first gap; ≤1000
│  │  ├─ ord  type/lang → type/any → common/lang → common/any
│  │  └─ bug  removeAllCases loops forever on mixed-case title ("sR" vs "Sr")
│  ├─ utils/TextMatcherUtil           BeiderMorse+Soundex → (diff+1)*20
│  └─ config/LoggerConfig             coverage-excluded
├─ test  ClientV1UnitTest · NormalizerV1UnitTest · TextMatcherUtilTest
│        JUnit Jupiter + Mockito (@InjectMocks Environment, mockStatic TextMatcherUtil)
├─ rule  javadoc every class/field/method · no hardcoded versions in docs/scripts
├─ ship  Central, autoPublish=false · CI .github/workflows/push-trigger.yml (kattu)
└─ skip  target/ .local/ licenses/*.txt THIRD-PARTY-NOTICES* NOTICE .github/keys/
```
