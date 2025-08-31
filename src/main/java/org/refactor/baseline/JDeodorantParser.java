package org.refactor.baseline;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
public class JDeodorantParser implements RefactorSuggestionParser {
    
    // Regex to extract method signature components from JDeodorant format
    // Format: ClassName::methodName(parameters):returnType
    private static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile(
        "^([^:]+)::([^(]+)\\(([^)]*)\\):([^\\s]*)$"
    );

    @Override
    public List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project) {
        List<Pair<JavaMethod, JavaClass>> suggestions = new ArrayList<>();
        List<Pair<String, String>> rawSuggestions = read(file);

        for (Pair<String, String> raw : rawSuggestions) {
            String sourceMethodFullName = raw.getLeft();
            String targetClassName = raw.getRight();
            
            // Parse method signature using regex
            ParsedMethodInfo parsedInfo = parseMethodSignature(sourceMethodFullName);
            if (parsedInfo == null) {
                log.error("Invalid method signature format: {}", sourceMethodFullName);
                continue;
            }

            JavaClass source = project.findClass(parsedInfo.getSourceClass()).orElse(null);
            JavaClass target = project.findClass(targetClassName).orElse(null);
            if (source == null || target == null) {
                if (source == null) log.warn("class not found: source {}.", parsedInfo.getSourceClass());
                if (target == null) log.warn("class not found: target {}.", targetClassName);
                continue;
            }
            source.findMethod(parsedInfo.getMethodName(), parsedInfo.getParameterTypes())
                    .ifPresentOrElse(method -> suggestions.add(Pair.of(method, target)),
                            () -> log.error("Method not found: {}.", sourceMethodFullName));
        }

        return suggestions;
    }
    
    /**
     * Parse method signature using regex pattern
     * @param methodSignature The method signature in format: ClassName::methodName(parameters):returnType
     * @return ParsedMethodInfo containing class, method, parameters, and return type, or null if invalid
     */
    private ParsedMethodInfo parseMethodSignature(String methodSignature) {
        Matcher matcher = METHOD_SIGNATURE_PATTERN.matcher(methodSignature);
        if (!matcher.matches()) {
            log.error("Invalid method signature format: {}", methodSignature);
            return null;
        }
        
        String sourceClass = matcher.group(1).trim();
        String methodName = matcher.group(2).trim();
        String parameters = matcher.group(3).trim();
        String returnType = matcher.group(4).trim();
        
        // Parse parameters into array
        String[] parameterTypes = parameters.isEmpty() ? new String[0] : parameters.split(",");
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = parameterTypes[i].trim();
        }
        
        return new ParsedMethodInfo(sourceClass, methodName, parameterTypes, returnType);
    }
    
    /**
     * Inner class to hold parsed method information
     */
    @Getter
    private static class ParsedMethodInfo {
        private final String sourceClass;
        private final String methodName;
        private final String[] parameterTypes;
        private final String returnType;
        
        public ParsedMethodInfo(String sourceClass, String methodName, String[] parameterTypes, String returnType) {
            this.sourceClass = sourceClass;
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

    }


    private static List<Pair<String, String>> read(Path file) {
        List<Pair<String, String>> suggestions = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // skip header
            String line = reader.readLine();
            if (line == null) {
                return Collections.emptyList();
            }

            while (StringUtils.isNoneEmpty(line = reader.readLine())) {
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


