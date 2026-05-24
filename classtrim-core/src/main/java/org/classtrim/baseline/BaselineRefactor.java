package org.classtrim.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.classtrim.baseline.BaselineEnum;
import org.classtrim.core.config.DatasetEnum;
import org.classtrim.core.metric.ThresholdViolationCounts;
import org.classtrim.core.model.JavaClass;
import org.classtrim.core.model.JavaMethod;
import org.classtrim.core.model.JavaProject;
import org.classtrim.core.util.FileUtils;
import org.classtrim.core.metric.MetricUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class BaselineRefactor {
    public static void main(String[] args) {
//        doRefactor(BaselineEnum.HMOVE);
//        doRefactor(BaselineEnum.JMOVE);
        for (BaselineEnum baseline : BaselineEnum.values()) {
            doRefactor(baseline);
        }
    }

    private static void doRefactor(BaselineEnum baseline) {
        log.info("--- Start baseline {} ---", baseline.getName());
        RefactorSuggestionParser parser = ParserFactory.getParser(baseline);
        for (DatasetEnum dataset : DatasetEnum.values()) {
            if (dataset == DatasetEnum.TEST) continue; // Skip test dataset
            log.info("Processing dataset: {}", dataset.getName());

            JavaProject project = JavaProject.load(dataset);
            Path file = Paths.get("baseline", baseline.getName(), dataset.getName(),
                    dataset.getName() + ".tsv");
            if (!Files.exists(file)) {
                log.warn("Project {}, no suggestion: {}", dataset.getName(), file);
                FileUtils.appendToBaselineTsv(baseline.getName(), dataset.getName()+"*", ThresholdViolationCounts.ZERO);
                continue;
            }

            List<Pair<JavaMethod, JavaClass>> suggestions = parser.parse(file, project);
            Map<JavaClass, List<JavaMethod>> methodsByClass = applySuggestions(project, suggestions);

            ThresholdViolationCounts counts = MetricUtils.calculateMetric(methodsByClass, dataset.getThreshold());
            FileUtils.appendToBaselineTsv(baseline.getName(), dataset.getName(), counts);
        }
    }

    private static Map<JavaClass, List<JavaMethod>> applySuggestions(JavaProject project,
                                                                     List<Pair<JavaMethod, JavaClass>> suggestions) {
        Map<JavaClass, List<JavaMethod>> methodsByClass = convertToMap(project);
        for (Pair<JavaMethod, JavaClass> suggestion : suggestions) {
            JavaMethod method = suggestion.getLeft();
            JavaClass targetClass = suggestion.getRight();
            JavaClass sourceClass = method.getClazz();
            if (methodsByClass.containsKey(sourceClass)) {
                methodsByClass.get(sourceClass).remove(method);
                methodsByClass.computeIfAbsent(targetClass, k -> new ArrayList<>()).add(method);
            } else {
                log.warn("Class not refactorable: {}.", sourceClass.toString());
            }
        }
        return methodsByClass;
    }

    /**
     * Refactorable class, Declaring all methods.
     */
    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));
    }
}
