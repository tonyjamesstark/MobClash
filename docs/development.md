# Development

Needs Java 21 and Maven 3.6+.

```bash
mvn clean verify        # format check, unit and integration tests, jar
mvn spotless:apply      # fix formatting
mvn test                # unit tests only
```

The jar lands in `target/mobclash-<version>.jar`.

Formatting is Google Java Format through Spotless, with unused imports removed and the POM
sorted. `mvn verify` fails on any formatting difference.

Tests are listed in [TEST-SUMMARY.md](../TEST-SUMMARY.md).

## Layout

```
src/main/java/io/tjs/mobclash/
├── MobClashPlugin.java
├── commands/
├── listeners/
└── managers/
```
