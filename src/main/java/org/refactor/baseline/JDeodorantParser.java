package org.refactor.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JDeodorantParser implements RefactorSuggestionParser {

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();
        List<Pair<String, String>> rawSuggestions = read(file);

        for (Pair<String, String> raw : rawSuggestions) {
            String sourceMethodFullName = raw.getLeft();
            String targetClass = raw.getRight();
            String[] parts = sourceMethodFullName.split("::");
            String sourceClass = parts[0];
            String methodWithParameters = parts[1];
            String methodName = methodWithParameters.split("\\(")[0];
            String parameters = methodWithParameters.split("\\(")[1].split("\\)")[0];
            String[] parameterTypes = parameters.split(",");

            JavaClass source = project.findClass(sourceClass).orElse(null);
            JavaClass target = project.findClass(targetClass).orElse(null);
            if (source == null || target == null) {
                log.error("Class not found: source={}, target={}", sourceClass, targetClass);
                continue;
            }
            source.findMethod(methodName, parameterTypes)
                    .ifPresentOrElse(method -> suggestions.add(Pair.of(method, target)),
                            () -> log.error("Method not found: {}.", sourceMethodFullName));
        }

        return suggestions;
    }

    // org.apache.tools.ant.taskdefs.FixCRLF::endOfCharConst(org.apache.tools.ant.taskdefs.FixCRLF.OneLiner.BufferLine, char):void

    private static List<Pair<String, String>> read(Path file) {
        List<Pair<String, String>> suggestions = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // skip header
            String line = reader.readLine();
            if (line == null) {
                return Collections.emptyList();
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                String sourceMethodFullName = parts[1].trim();
                String targetClass = parts[2].trim();

                suggestions.add(Pair.of(sourceMethodFullName, targetClass));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return suggestions;
    }
}


