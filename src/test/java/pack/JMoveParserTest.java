package pack;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.refactor.baseline.JMoveParser;
import org.refactor.common.DatasetEnum;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class JMoveParserTest {

    private JMoveParser parser;
    private File testFile;
    private JavaProject testProject;

    @Before
    public void setUp() throws IOException {
        parser = new JMoveParser();

        // Use the mock file instead of creating temporary files
        testFile = new File("src/test/java/pack/JMoveParserTest001.tsv");

        // Create a test project
        testProject = new JavaProject(DatasetEnum.TEST);
        testProject.parse();
    }

    @Test
    public void testParseWithRealClassesFromTestProject() throws IOException {
        // Get actual classes from the test project
        Optional<JavaClass> classA = testProject.findClass("pack/A");
        Optional<JavaClass> classB = testProject.findClass("pack/B");
        
        assertTrue("Class A should exist in test project", classA.isPresent());
        assertTrue("Class B should exist in test project", classB.isPresent());
        
        // Parse the mock file
        List<Pair<JavaMethod, JavaClass>> result = parser.parse(testFile.toPath(), testProject);
        
        // Should find suggestions from the mock file
        assertNotNull(result);
        assertFalse("Should find at least one suggestion", result.isEmpty());
        
        // Find the specific suggestion for pack.A::throwException():void -> pack.B
        Optional<Pair<JavaMethod, JavaClass>> throwExceptionSuggestion = result.stream()
                .filter(suggestion -> {
                    JavaMethod method = suggestion.getLeft();
                    return "throwException".equals(method.getName()) && 
                           method.getClazz().equals(classA.get());
                })
                .findFirst();
        
        assertTrue("Should find throwException suggestion", throwExceptionSuggestion.isPresent());
        assertEquals("Target class should match", classB.get(), throwExceptionSuggestion.get().getRight());
    }

    @Test
    public void testParseWithVariousMethodSignatures() throws IOException {
        // Parse the mock file which contains various method signature patterns
        List<Pair<JavaMethod, JavaClass>> result = parser.parse(testFile.toPath(), testProject);
        
        assertNotNull(result);
        assertEquals("Should have exactly seven suggestions", 7, result.size());
        
        // Verify each suggestion was parsed correctly
        verifySuggestion(result, 0, "pack/A", "throwException", "pack/B");
        verifySuggestion(result, 1, "pack/F", "method_F4", "pack/G");
        verifySuggestion(result, 2, "pack/I", "method_I1", "pack/J");
        verifySuggestion(result, 3, "pack/H", "build", "pack/K");
        verifySuggestion(result, 4, "pack/B", "compareTo", "pack/A");
        verifySuggestion(result, 5, "pack/C", "method_C", "pack/D");
        verifySuggestion(result, 6, "pack/E", "method_E2", "pack/F");
    }

    @Test
    public void testMockFileExists() {
        // Verify that the mock file exists and is readable
        assertTrue("Mock file should exist", Files.exists(testFile.toPath()));
        assertTrue("Mock file should be readable", Files.isReadable(testFile.toPath()));
    }

    @Test
    public void testMockFileContent() throws IOException {
        // Verify that the mock file contains the expected content
        String content = Files.readString(testFile.toPath());
        assertNotNull("Mock file content should not be null", content);
        assertTrue("Mock file should contain header", content.contains("Refactoring Type"));
        assertTrue("Mock file should contain move method suggestions", content.contains("Move Method"));
        assertTrue("Mock file should contain method signatures", content.contains("::"));
    }

    // Helper method to verify suggestions
    private void verifySuggestion(List<Pair<JavaMethod, JavaClass>> suggestions, int index,
                                  String expectedSourceClass, String expectedMethodName, String expectedTargetClass) {
        Pair<JavaMethod, JavaClass> suggestion = suggestions.get(index);

        // Verify source class and method
        Optional<JavaClass> sourceClass = testProject.findClass(expectedSourceClass);
        assertTrue("Source class " + expectedSourceClass + " should exist", sourceClass.isPresent());

        Optional<JavaMethod> foundMethod = sourceClass.get().getDeclaredMethods().stream()
                .filter(m -> expectedMethodName.equals(m.getName()))
                .findFirst();
        assertTrue("Should find method " + expectedMethodName, foundMethod.isPresent());
        assertEquals("Source method should match", foundMethod.get(), suggestion.getLeft());

        // Verify target class
        Optional<JavaClass> targetClass = testProject.findClass(expectedTargetClass);
        assertTrue("Target class " + expectedTargetClass + " should exist", targetClass.isPresent());
        assertEquals("Target class should match", targetClass.get(), suggestion.getRight());
    }
}
