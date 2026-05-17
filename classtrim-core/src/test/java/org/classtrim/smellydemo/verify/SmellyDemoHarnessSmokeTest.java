package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Smoke test for the smelly-demo bytecode verification harness.
 *
 * <p>Verifies that {@link SmellyDemoLoader} can resolve and load every
 * compiled Demo_Class plus every source {@code .java} file. The substantive
 * property tests live in the sibling 8.x tasks; this test only confirms the
 * shared-library loader/inventory contract.
 */
public class SmellyDemoHarnessSmokeTest {

    @Test
    public void harnessLoadsAllEightDemoClasses() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        assertEquals(
                "Expected exactly 8 compiled Demo_Classes under "
                        + SmellyDemoLoader.resolveCompiledClassesDirectory(),
                8,
                byName.size());

        assertEquals(
                "Loaded class simple names should equal the canonical inventory",
                SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES,
                new TreeSet<>(byName.keySet()));

        for (Map.Entry<String, DemoClassModel> entry : byName.entrySet()) {
            DemoClassModel model = entry.getValue();
            assertNotNull("internalName must be non-null for " + entry.getKey(), model.internalName);
            assertNotNull("methods list must be non-null for " + entry.getKey(), model.methods);
            assertTrue(
                    "methods list must contain at least one method (constructor) for " + entry.getKey(),
                    !model.methods.isEmpty());
            assertEquals(
                    "Internal name must match the expected Demo_Package layout for "
                            + entry.getKey(),
                    SmellyDemoLoader.simpleNameToInternalName(entry.getKey()),
                    model.internalName);
        }
    }

    @Test
    public void harnessListAndMapViewsAreConsistent() {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();
        List<DemoClassModel> asList = SmellyDemoLoader.loadAllDemoClassModelsAsList();
        assertEquals("List view size must match map view size", byName.size(), asList.size());
        for (DemoClassModel model : asList) {
            String simple = SmellyDemoLoader.simpleNameFromInternalName(model.internalName);
            assertTrue(
                    "List-view class must be discoverable in map view: " + simple,
                    byName.containsKey(simple));
        }
    }

    @Test
    public void harnessFindsAllEightSourceFiles() {
        List<Path> sources = SmellyDemoLoader.listDemoSourceFiles();
        assertEquals(
                "Expected exactly 8 .java source files under "
                        + SmellyDemoLoader.resolveSourceDirectory(),
                8,
                sources.size());

        TreeSet<String> stems = sources.stream()
                .map(p -> {
                    String name = p.getFileName().toString();
                    return name.substring(0, name.length() - ".java".length());
                })
                .collect(Collectors.toCollection(TreeSet::new));

        assertEquals(
                "Source filename stems should equal the canonical Demo_Class inventory",
                SmellyDemoLoader.EXPECTED_DEMO_CLASS_NAMES,
                stems);
    }
}
