# classtrim-plugin

IntelliJ IDEA plugin module for ClassTrim — built with **Gradle + IntelliJ
Platform Gradle Plugin 2.x**, not Maven.

The IntelliJ Platform is distributed as a ZIP (the IDE itself), not a JAR, so
plain Maven can't resolve `com.jetbrains.intellij.idea:ideaIC`. The IntelliJ
Platform Gradle Plugin handles ZIP extraction, classpath wiring, sandbox setup,
and `runIde` / `buildPlugin` for us.

## Prerequisites

- JDK 17 (the IntelliJ Platform 2024.1 minimum)
- A network connection on first build (Gradle wrapper, ideaIC, jqwik, etc.)

## Build

The plugin depends on `classtrim-core`, which is built by the sibling Maven
module. Install it to your local Maven repo once per change:

```bash
# from the repo root
mvn -pl classtrim-core -am install
```

Then run the Gradle build:

```bash
# from this directory
./gradlew build           # compile + test
./gradlew runIde          # launch a sandbox IDE with the plugin loaded
./gradlew buildPlugin     # produce the distributable .zip in build/distributions
./gradlew verifyPlugin    # run JetBrains Plugin Verifier
```

## Tests

Standard JUnit Jupiter and jqwik property tests live under
`src/test/java/org/classtrim/plugin/...`. The repository also ships
manual jqwik runner scripts under `target/pbt-runner/` that compile and run
each property test against a tiny set of IntelliJ stubs without the full
platform — these are useful when you want to iterate on a property without
the cost of bringing the whole IDE classpath up.

Once `./gradlew test` is wired into CI the manual runners can be retired.
