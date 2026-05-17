package org.classtrim.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class REsolutionParser implements RefactorSuggestionParser {

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();
        List<Triple<String, String, String>> rawSuggestions = readRefactoringSuggestions(file);

        for (Triple<String, String, String> raw : rawSuggestions) {
            String methodName = raw.getLeft().replace(raw.getMiddle() + ".", "");
            String sourceClass = raw.getMiddle().replace(".", "/");
            String targetClass = raw.getRight().replace(".", "/");

            JavaClass source = project.findClass(sourceClass).orElse(null);
            JavaClass target = project.findClass(targetClass).orElse(null);
            if (source == null || target == null) {
                log.error("Class not found: source={}, target={}", sourceClass, targetClass);
                continue;
            }

            source.getDeclaredMethods().stream()
                .filter(method -> method.toString().equals(methodName))
                .findFirst()
                .ifPresentOrElse(method -> suggestions.add(Pair.of(method, target)),
                    () -> log.error("Method not found: {}.", raw.getLeft()));
        }

        return suggestions;
    }

     /**
     * Read refactoring suggestions from a TSV file.
     * The TSV file should have the format:
     * Method Name    Source Class    Target Class
     * org.example.Class.method(Type1, Type2)    org.example.Class    org.example.TargetClass
     * Note: Move-field suggestions (lines where method string doesn't end with ')') are skipped.
     *
     * @param tsvFile Path to the TSV file
     * @return List of Triple containing (method string, source class, target class)
     */
    private static List<Triple<String, String, String>> readRefactoringSuggestions(Path tsvFile) {
        List<Triple<String, String, String>> suggestions = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(tsvFile, StandardCharsets.UTF_8)) {
            // Skip header line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length != 3) {
                    continue; // Skip invalid lines
                }

                String methodString = parts[0].trim();
                // Skip move-field suggestions (lines where method string doesn't end with ')')
                if (!methodString.endsWith(")")) {
                    continue;
                }

                String sourceClass = parts[1].trim();
                String targetClass = parts[2].trim();

                suggestions.add(Triple.of(methodString, sourceClass, targetClass));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return suggestions;
    }
}


