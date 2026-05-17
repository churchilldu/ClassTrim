package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Property 2: Demo_Class inventory completeness.
 *
 * <p>Asserts exactly one {@code .class} file exists for each name in the
 * canonical Demo_Class inventory
 * {@code {Order, OrderItem, Customer, Product, Inventory, ShippingCalculator,
 * Invoice, OrderProcessor}} directly under
 * {@link SmellyDemoLoader#resolveCompiledClassesDirectory()}, and that no
 * other {@code .class} files exist in that directory.
 *
 * <p><b>Validates: Requirements 2.1, 8.2</b>
 */
public class Property2DemoClassInventoryCompletenessTest {

    /**
     * Property 2: the directory contains exactly the eight expected
     * {@code .class} files — no missing entries, no extra entries, and no
     * duplicates.
     *
     * <p><b>Validates: Requirements 2.1, 8.2</b>
     */
    @Test
    public void compiledClassesDirectoryContainsExactlyTheCanonicalInventory() {
        Path classesDir = SmellyDemoLoader.resolveCompiledClassesDirectory();
        assertTrue(
                "smelly-demo compiled classes directory must exist: " + classesDir
                        + ". Run `mvn -pl smelly-demo -am compile` first.",
                Files.isDirectory(classesDir));

        // 1. List all `.class` files directly under the compiled-classes
        //    directory. Do NOT recurse — Property 2 is a flat-directory claim.
        List<Path> classFiles = listDirectClassFiles(classesDir);

        // 2. Assert there are exactly 8 such files.
        assertEquals(
                "Expected exactly 8 .class files directly under " + classesDir
                        + " but found: " + classFiles,
                SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.size(),
                classFiles.size());

        // 3. Compute the set of simple names (filename minus the `.class`
        //    extension), preserving the multiplicity of each name so we can
        //    detect duplicates.
        Map<String, Integer> countsBySimpleName = new LinkedHashMap<>();
        for (Path classFile : classFiles) {
            String fileName = classFile.getFileName().toString();
            String simpleName = fileName.substring(0, fileName.length() - ".class".length());
            countsBySimpleName.merge(simpleName, 1, Integer::sum);
        }

        // 4. Assert that the set of simple names equals
        //    {@code SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES}.
        assertEquals(
                "Set of compiled .class simple names must equal the canonical "
                        + "Demo_Class inventory",
                SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES,
                new TreeSet<>(countsBySimpleName.keySet()));

        // 5. Verify each expected name corresponds to exactly one file
        //    (explicit no-duplicates check via file count, even though set
        //    equality above already implies it for a flat listing).
        for (String expectedName : SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES) {
            Integer count = countsBySimpleName.get(expectedName);
            if (count == null) {
                fail("Missing compiled .class file for Demo_Class: " + expectedName
                        + " under " + classesDir);
            }
            assertEquals(
                    "Demo_Class " + expectedName
                            + " must correspond to exactly one .class file under "
                            + classesDir,
                    Integer.valueOf(1),
                    count);
        }
    }

    /**
     * Property 2 (negative form): every {@code .class} file in the directory
     * carries an inventory-recognized simple name. This guards against extra
     * inner/anonymous/synthetic class files (e.g. {@code Order$1.class}) that
     * a future regression might introduce.
     *
     * <p><b>Validates: Requirements 2.1, 8.2</b>
     */
    @Test
    public void noUnexpectedClassFilesExistInCompiledClassesDirectory() {
        Path classesDir = SmellyDemoLoader.resolveCompiledClassesDirectory();
        assertTrue(
                "smelly-demo compiled classes directory must exist: " + classesDir,
                Files.isDirectory(classesDir));

        List<Path> classFiles = listDirectClassFiles(classesDir);

        Map<String, Path> unexpected = new HashMap<>();
        for (Path classFile : classFiles) {
            String fileName = classFile.getFileName().toString();
            String simpleName = fileName.substring(0, fileName.length() - ".class".length());
            if (!SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES.contains(simpleName)) {
                unexpected.put(simpleName, classFile);
            }
        }

        assertTrue(
                "Found unexpected .class files outside the canonical inventory: "
                        + unexpected,
                unexpected.isEmpty());
    }

    /**
     * List {@code .class} files directly inside {@code dir} (no recursion).
     * Returned list is sorted alphabetically by filename for stable failure
     * output.
     */
    private static List<Path> listDirectClassFiles(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to enumerate " + dir, e);
        }
    }
}
