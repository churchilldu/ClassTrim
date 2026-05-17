package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Property 4 — Build output rooted under {@code target/classes}.
 *
 * <p>For every {@code .class} file emitted anywhere under
 * {@code smelly-demo/target/}, the file's path must be rooted under
 * {@code smelly-demo/target/classes/}. Any {@code .class} file landing
 * outside {@code target/classes/} (e.g. accidentally in
 * {@code target/test-classes/}) fails this property.
 *
 * <p>Validates: Requirements 1.7, 8.1.
 */
public class Property4BuildOutputRootedTest {

    /** Canonical Demo_Class inventory size — exactly 8 {@code .class} files expected. */
    private static final int EXPECTED_CLASS_FILE_COUNT = 8;

    @Test
    public void everyEmittedClassFileIsRootedUnderTargetClasses() throws IOException {
        // Resolve smelly-demo/target/ relative to the classtrim-core module root
        // (which is the working directory Maven uses when running tests).
        Path targetDir = Paths.get("..", "smelly-demo", "target")
                .toAbsolutePath()
                .normalize();
        assertTrue(
                "smelly-demo target directory must exist (run `mvn -pl smelly-demo -am compile` first): "
                        + targetDir,
                Files.isDirectory(targetDir));

        // Absolute, normalized path of the only acceptable root for emitted .class files.
        Path targetClassesDir = targetDir.resolve("classes").normalize();
        assertTrue(
                "smelly-demo target/classes directory must exist: " + targetClassesDir,
                Files.isDirectory(targetClassesDir));

        // Recursively enumerate every .class file under smelly-demo/target/.
        List<Path> classFiles;
        try (Stream<Path> stream = Files.walk(targetDir)) {
            classFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .map(p -> p.toAbsolutePath().normalize())
                    .sorted()
                    .collect(Collectors.toList());
        }

        // At least one .class file must exist (otherwise the module did not compile).
        assertFalse(
                "Expected at least one .class file under " + targetDir
                        + " — was the module compiled?",
                classFiles.isEmpty());

        // Every .class file path MUST be rooted under target/classes/. Path.startsWith is
        // path-segment-aware, so this rejects sibling roots like target/test-classes/ even
        // though they share a string prefix.
        List<Path> outsideTargetClasses = classFiles.stream()
                .filter(p -> !p.startsWith(targetClassesDir))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(ArrayList::new));

        assertTrue(
                "Every .class file under " + targetDir + " must be rooted under "
                        + targetClassesDir + ", but found "
                        + outsideTargetClasses.size() + " misplaced file(s): "
                        + outsideTargetClasses,
                outsideTargetClasses.isEmpty());

        // The expected inventory is exactly 8 Demo_Classes; assert the count matches so a
        // stray inner/anonymous/local class slipping into target/classes also fails this
        // property.
        assertEquals(
                "Expected exactly " + EXPECTED_CLASS_FILE_COUNT
                        + " .class files under " + targetDir + ", but found "
                        + classFiles.size() + ": " + classFiles,
                EXPECTED_CLASS_FILE_COUNT,
                classFiles.size());
    }
}
