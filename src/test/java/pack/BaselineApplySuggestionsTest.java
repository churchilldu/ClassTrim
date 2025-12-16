package pack;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.classtrim.baseline.BaselineRefactor;
import org.classtrim.common.DatasetEnum;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
import org.classtrim.util.MetricUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class BaselineApplySuggestionsTest {

    @Test
    public void testApplySingleSuggestionMovesMethodBetweenClasses() {
        JavaProject project = new JavaProject(DatasetEnum.TEST);
        project.parse();

        Map<JavaClass, List<JavaMethod>> methodsByClass = project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));

        // pick a source class with at least one method and a different target class
        JavaClass source = methodsByClass.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(AssertionError::new);
        JavaMethod method = methodsByClass.get(source).get(0);
        JavaClass target = methodsByClass.keySet().stream()
                .filter(c -> !c.equals(source))
                .findFirst().orElseThrow(AssertionError::new);

        Map<JavaClass, Integer> wmcBefore = MetricUtils.getWmcOfClass(methodsByClass);
        int sourceBefore = wmcBefore.getOrDefault(source, 0);
        int targetBefore = wmcBefore.getOrDefault(target, 0);

        // Apply the same logic as BaselineRefactor
        if (methodsByClass.containsKey(source)) {
            methodsByClass.get(source).remove(method);
            methodsByClass.computeIfAbsent(target, k -> new ArrayList<>()).add(method);
        } else {
            fail("Source class not present in methodsByClass");
        }

        Map<JavaClass, Integer> wmcAfter = MetricUtils.getWmcOfClass(methodsByClass);
        int sourceAfter = wmcAfter.getOrDefault(source, 0);
        int targetAfter = wmcAfter.getOrDefault(target, 0);

        assertEquals(sourceBefore - 1, sourceAfter);
        assertEquals(targetBefore + 1, targetAfter);
        assertFalse(methodsByClass.get(source).contains(method));
        assertTrue(methodsByClass.get(target).contains(method));
    }

    @Test
    public void testApplySuggestionsMethod() throws Exception {
        JavaProject project = new JavaProject(DatasetEnum.TEST);
        project.parse();

        // Get the private applySuggestions method via reflection
        Method applySuggestions = BaselineRefactor.class.getDeclaredMethod("applySuggestions", 
            JavaProject.class, List.class);
        applySuggestions.setAccessible(true);

        // Create test suggestions
        Map<JavaClass, List<JavaMethod>> initialMethodsByClass = project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));

        // Find a source class with methods and a different target class
        JavaClass sourceClass = initialMethodsByClass.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(AssertionError::new);
        JavaMethod methodToMove = initialMethodsByClass.get(sourceClass).get(0);
        JavaClass targetClass = initialMethodsByClass.keySet().stream()
                .filter(c -> !c.equals(sourceClass))
                .findFirst().orElseThrow(AssertionError::new);

        // Create suggestion: move method from source to target
        List<Pair<JavaMethod, JavaClass>> suggestions = Arrays.asList(
            Pair.of(methodToMove, targetClass)
        );

        // Get initial counts
        int sourceInitialCount = initialMethodsByClass.get(sourceClass).size();
        int targetInitialCount = initialMethodsByClass.getOrDefault(targetClass, new ArrayList<>()).size();

        // Invoke applySuggestions method
        @SuppressWarnings("unchecked")
        Map<JavaClass, List<JavaMethod>> result = (Map<JavaClass, List<JavaMethod>>) 
            applySuggestions.invoke(null, project, suggestions);

        // Verify the result
        assertNotNull("Result should not be null", result);
        assertTrue("Source class should still exist in result", result.containsKey(sourceClass));
        assertTrue("Target class should exist in result", result.containsKey(targetClass));

        // Verify method was moved
        List<JavaMethod> sourceMethods = result.get(sourceClass);
        List<JavaMethod> targetMethods = result.get(targetClass);

        assertEquals("Source class should have one less method", sourceInitialCount - 1, sourceMethods.size());
        assertEquals("Target class should have one more method", targetInitialCount + 1, targetMethods.size());
        assertFalse("Source should not contain moved method", sourceMethods.contains(methodToMove));
        assertTrue("Target should contain moved method", targetMethods.contains(methodToMove));

        // Verify other classes remain unchanged
        for (Map.Entry<JavaClass, List<JavaMethod>> entry : result.entrySet()) {
            if (!entry.getKey().equals(sourceClass) && !entry.getKey().equals(targetClass)) {
                List<JavaMethod> originalMethods = initialMethodsByClass.get(entry.getKey());
                assertEquals("Other classes should remain unchanged", originalMethods, entry.getValue());
            }
        }
    }

    @Test
    public void testApplySuggestionsWithMultipleSuggestions() throws Exception {
        JavaProject project = new JavaProject(DatasetEnum.TEST);
        project.parse();

        Method applySuggestions = BaselineRefactor.class.getDeclaredMethod("applySuggestions", 
            JavaProject.class, List.class);
        applySuggestions.setAccessible(true);

        Map<JavaClass, List<JavaMethod>> initialMethodsByClass = project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));

        // Find multiple source classes with methods and a target class
        List<JavaClass> sourceClasses = initialMethodsByClass.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .limit(2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (sourceClasses.size() < 2) {
            // Skip test if not enough source classes
            return;
        }

        JavaClass targetClass = initialMethodsByClass.keySet().stream()
                .filter(c -> !sourceClasses.contains(c))
                .findFirst().orElseThrow(AssertionError::new);

        // Create multiple suggestions
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();
        for (JavaClass sourceClass : sourceClasses) {
            JavaMethod method = initialMethodsByClass.get(sourceClass).get(0);
            suggestions.add(Pair.of(method, targetClass));
        }

        // Get initial counts
        Map<JavaClass, Integer> initialCounts = new java.util.HashMap<>();
        for (JavaClass sourceClass : sourceClasses) {
            initialCounts.put(sourceClass, initialMethodsByClass.get(sourceClass).size());
        }
        int targetInitialCount = initialMethodsByClass.getOrDefault(targetClass, new ArrayList<>()).size();

        // Invoke applySuggestions method
        @SuppressWarnings("unchecked")
        Map<JavaClass, List<JavaMethod>> result = (Map<JavaClass, List<JavaMethod>>) 
            applySuggestions.invoke(null, project, suggestions);

        // Verify all methods were moved to target
        List<JavaMethod> targetMethods = result.get(targetClass);
        assertEquals("Target should have all moved methods", 
            targetInitialCount + suggestions.size(), targetMethods.size());

        // Verify source classes have fewer methods
        for (JavaClass sourceClass : sourceClasses) {
            List<JavaMethod> sourceMethods = result.get(sourceClass);
            assertEquals("Source class should have one less method", 
                initialCounts.get(sourceClass) - 1, sourceMethods.size());
        }
    }

    @Test
    public void testApplySuggestionsWithEmptySuggestions() throws Exception {
        JavaProject project = new JavaProject(DatasetEnum.TEST);
        project.parse();

        Method applySuggestions = BaselineRefactor.class.getDeclaredMethod("applySuggestions", 
            JavaProject.class, List.class);
        applySuggestions.setAccessible(true);

        // Create empty suggestions list
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();

        // Invoke applySuggestions method
        @SuppressWarnings("unchecked")
        Map<JavaClass, List<JavaMethod>> result = (Map<JavaClass, List<JavaMethod>>) 
            applySuggestions.invoke(null, project, suggestions);

        // Result should be identical to initial state
        Map<JavaClass, List<JavaMethod>> expected = project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));

        assertEquals("Empty suggestions should return unchanged methodsByClass", expected, result);
    }
}


