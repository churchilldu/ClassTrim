package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Property 3: Source layout matches package and filename.
 *
 * <p>For every {@code .java} source file under {@code smelly-demo/src/main/java},
 * asserts the file declares package {@code org.classtrim.demo.ecommerce}, declares
 * exactly one top-level {@code public class}, and the filename stem equals that
 * class's simple name. Additionally asserts no {@code .java} files exist outside
 * the expected {@code org/classtrim/demo/ecommerce/} subpath under
 * {@code src/main/java}.
 *
 * <p>Implementation uses a simple regex-based parser (no full Java parser
 * required): a multiline anchor at column 0 distinguishes top-level
 * declarations from lines that only mention {@code class} inside javadoc,
 * comments, or nested types.
 *
 * <p><b>Validates: Requirements 2.3, 2.4</b>
 */
public class Property3SourceLayoutTest {

    /** Matches the package declaration anywhere it appears at the start of a line. */
    private static final Pattern PACKAGE_DECL = Pattern.compile(
            "(?m)^\\s*package\\s+org\\.classtrim\\.demo\\.ecommerce\\s*;");

    /**
     * Matches a top-level {@code public class Foo} declaration anchored at column 0.
     * Tolerates an optional {@code final} or {@code abstract} modifier between
     * {@code public} and {@code class}. Anchoring at column 0 means nested type
     * declarations (which are indented inside their enclosing class) do not
     * match.
     */
    private static final Pattern PUBLIC_TOP_LEVEL_CLASS = Pattern.compile(
            "(?m)^public\\s+(?:final\\s+|abstract\\s+)?class\\s+(\\w+)");

    /** Subpath the spec requires every Demo_Class source file to live under. */
    private static final Path EXPECTED_SUBPATH =
            Paths.get("org", "classtrim", "demo", "ecommerce");

    @Test
    public void everyDemoSourceFileDeclaresExpectedPackage() throws IOException {
        List<Path> sources = SmellyDemoLoader.listDemoSourceFiles();
        assertEquals(
                "Expected exactly 8 .java source files under "
                        + SmellyDemoLoader.resolveSourceDirectory(),
                8,
                sources.size());

        for (Path source : sources) {
            String content = Files.readString(source);
            Matcher m = PACKAGE_DECL.matcher(content);
            assertTrue(
                    "Source file `" + source.getFileName()
                            + "` must declare package `"
                            + SmellyDemoLoader.DEMO_PACKAGE + "`",
                    m.find());
        }
    }

    @Test
    public void everyDemoSourceFileDeclaresExactlyOneTopLevelPublicClass() throws IOException {
        for (Path source : SmellyDemoLoader.listDemoSourceFiles()) {
            String content = Files.readString(source);
            List<String> declared = new ArrayList<>();
            Matcher m = PUBLIC_TOP_LEVEL_CLASS.matcher(content);
            while (m.find()) {
                declared.add(m.group(1));
            }
            assertEquals(
                    "Source file `" + source.getFileName()
                            + "` must declare exactly one top-level public class"
                            + " (found " + declared + ")",
                    1,
                    declared.size());
        }
    }

    @Test
    public void everyDemoSourceFileFilenameStemEqualsItsPublicClassName() throws IOException {
        for (Path source : SmellyDemoLoader.listDemoSourceFiles()) {
            String filename = source.getFileName().toString();
            String stem = filename.substring(0, filename.length() - ".java".length());

            String content = Files.readString(source);
            Matcher m = PUBLIC_TOP_LEVEL_CLASS.matcher(content);
            assertTrue(
                    "Source file `" + filename
                            + "` must declare a top-level public class",
                    m.find());
            String declaredName = m.group(1);
            assertNotNull(
                    "Captured class name must be non-null for `" + filename + "`",
                    declaredName);
            assertEquals(
                    "Filename stem must equal the declared top-level public class"
                            + " simple name for `" + filename + "`",
                    stem,
                    declaredName);
        }
    }

    @Test
    public void noJavaFilesExistOutsideExpectedPackagePath() throws IOException {
        // resolveSourceDirectory() => .../smelly-demo/src/main/java/org/classtrim/demo/ecommerce
        // Step up four levels (ecommerce → demo → classtrim → org → java) to
        // reach the src/main/java root and walk it recursively.
        Path javaRoot = SmellyDemoLoader.resolveSourceDirectory()
                .getParent().getParent().getParent().getParent();
        assertTrue(
                "Expected `src/main/java` root to exist at " + javaRoot,
                Files.isDirectory(javaRoot));

        List<Path> allJavaFiles;
        try (Stream<Path> stream = Files.walk(javaRoot)) {
            allJavaFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
        }

        List<Path> stragglers = new ArrayList<>();
        for (Path file : allJavaFiles) {
            Path relative = javaRoot.relativize(file);
            if (!relative.startsWith(EXPECTED_SUBPATH)) {
                stragglers.add(relative);
            }
        }
        if (!stragglers.isEmpty()) {
            fail("Found .java file(s) outside `" + EXPECTED_SUBPATH
                    + "` under `" + javaRoot + "`: " + stragglers);
        }

        // Sanity: the recursive walk should yield exactly the same eight files
        // the loader's flat listing exposes (no future subpackage has slipped in).
        assertEquals(
                "Recursive .java count under `src/main/java` must equal the"
                        + " flat Demo_Package count",
                SmellyDemoLoader.listDemoSourceFiles().size(),
                allJavaFiles.size());
    }
}
