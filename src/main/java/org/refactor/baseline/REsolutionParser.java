package org.refactor.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.FileUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class REsolutionParser implements RefactorSuggestionParser {

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();
        List<Triple<String, String, String>> rawSuggestions = FileUtils.readRefactoringSuggestions(file);

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
}


