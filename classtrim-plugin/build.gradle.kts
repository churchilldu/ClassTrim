import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    // classtrim-core is built by the sibling Maven module; install it locally
    // with `mvn -pl classtrim-core -am install` from the repo root once per
    // change before running `./gradlew build`.
    mavenLocal()
    mavenCentral()

    // The IntelliJ Platform Gradle Plugin pulls ideaIC + bundled plugins from
    // here; this replaces the broken `com.jetbrains.intellij.idea:ideaIC:jar`
    // coordinate that the old Maven pom tried to resolve.
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // classtrim-core is the analysis engine the plugin wraps. Version is read
    // from gradle.properties via the parent Maven SNAPSHOT line so the two
    // build systems stay in lock-step.
    //
    // Two `<type>pom</type>` aggregators in classtrim-core's dependency graph
    // would otherwise put `.pom` files on the runtime classpath, which
    // IntelliJ's PathClassLoader rejects with
    // `java.util.zip.ZipException: Archive is not a ZIP archive`:
    //
    // 1. `org.uma.jmetal:jmetal` — BOM aggregator. Its `jmetal-core`,
    //    `jmetal-algorithm`, and `jmetal-problem` sub-modules are declared
    //    separately in classtrim-core's pom, so excluding the aggregator
    //    does not lose any classes.
    //
    // 2. `com.github.fommil.netlib:all` — netlib BOM. Its individual
    //    `netlib:core` and per-platform `netlib-native_ref-*` /
    //    `netlib-native_system-*` artifacts are correctly typed as JARs and
    //    remain on the classpath through their direct dependencies; the
    //    aggregator pom carries no compiled code.
    implementation("org.classtrim:classtrim-core:1.0-SNAPSHOT") {
        exclude(group = "org.uma.jmetal", module = "jmetal")
        exclude(group = "com.github.fommil.netlib", module = "all")
    }

    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion"))

        // Bundled plugins this plugin depends on (declared in plugin.xml as
        // <depends>com.intellij.modules.{platform,java}</depends>). Java
        // module ships in the bundled "Java" plugin.
        bundledPlugin("com.intellij.java")

        // Verifier and signer helpers — pulls JetBrains annotations and the
        // Plugin Verifier Gradle task into the build. (instrumentationTools()
        // was removed in 2.x; bytecode instrumentation is now wired in
        // automatically by the platform plugin.)
        pluginVerifier()
        zipSigner()

        // BasePlatformTestCase / HeavyPlatformTestCase + the JUnit 3 assert
        // overloads (assertFalse(String, boolean), getProject(), …) live in
        // the platform's testFramework jars; declaring them here puts them on
        // the test classpath. Required for the four IntelliJ light tests
        // (RunClassTrimAnalysisActionLightTest, PluginXmlExtensionsLightTest,
        // AnalysisRunIntegrationTest) the spec mandates.
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("net.jqwik:jqwik:1.8.4")
    // BasePlatformTestCase extends junit.framework.TestCase; the JUnit-3
    // assertX(String, ...) overloads live in this JAR, which platform 2.x
    // no longer pulls in transitively.
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    // junit-vintage-engine teaches the JUnit Platform how to run JUnit 3 /
    // JUnit 4 test classes — required for the IntelliJ light / heavy tests
    // (RunClassTrimAnalysisActionLightTest, PluginXmlExtensionsLightTest,
    // AnalysisRunIntegrationTest) which extend BasePlatformTestCase /
    // HeavyPlatformTestCase (both ultimately TestCase subclasses).
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        failureLevel = listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INVALID_PLUGIN
        )
    }

    // Disable searchable-options index build — costs a few minutes per `:build`
    // and we don't ship a Settings page deep enough to need it. Re-enable when
    // the configurable grows beyond five spinners.
    buildSearchableOptions = false
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        // Run all three engines: jupiter for plain JUnit 5 unit tests, jqwik
        // for the property tests, and vintage for the IntelliJ light / heavy
        // tests that extend BasePlatformTestCase / HeavyPlatformTestCase.
        includeEngines("junit-jupiter", "jqwik", "junit-vintage")
    }
}
