package org.refactor.baseline;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.refactor.common.BaselineEnum;
import org.refactor.common.DatasetEnum;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.AppProperties;
import org.refactor.util.FileUtils;
import org.refactor.util.MetricUtils;

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
        for (BaselineEnum baseline : BaselineEnum.values()) {
            doRefactor(baseline);
        }
    }

    private static void doRefactor(BaselineEnum baseline) {
        RefactorSuggestionParser parser = ParserFactory.getParser(baseline);
        for (DatasetEnum dataset : DatasetEnum.values()) {
            if (dataset == DatasetEnum.TEST) continue; // Skip test dataset
            log.info("Processing dataset: {}", dataset.getName());

            JavaProject project = JavaProject.load(AppProperties.getString("datasetRoot") + dataset.getName());
            Map<JavaClass, List<JavaMethod>> methodsByClass = convertToMap(project);
            Path file = Paths.get("baseline", dataset.getName(), dataset.getName() + ".tsv");
            if (!Files.exists(file)) {
                log.warn("Suggestion file not found: {}", file);
                continue;
            }

            List<Pair<JavaMethod, JavaClass>> suggestions = parser.parse(file, project);
            for (Pair<JavaMethod, JavaClass> suggestion : suggestions) {
                JavaMethod method = suggestion.getLeft();
                JavaClass targetClass = suggestion.getRight();
                JavaClass sourceClass = method.getClazz();
                if (methodsByClass.containsKey(sourceClass)) {
                    methodsByClass.get(sourceClass).remove(method);
                    methodsByClass.computeIfAbsent(targetClass, k -> new ArrayList<>()).add(method);
                } else {
                    log.error("Class not refactorable: {}.", sourceClass.toString());
                }
            }

            // Calculate metrics
            long wmcOverThreshold = MetricUtils.countClassWmcOverThreshold(methodsByClass, dataset.getThreshold().getWMC());
            long cboOverThreshold = MetricUtils.countClassCboOverThreshold(methodsByClass, dataset.getThreshold().getCBO());
            long rfcOverThreshold = MetricUtils.countClassRfcOverThreshold(methodsByClass, dataset.getThreshold().getRFC());

            // Append results to baseline.tsv
            FileUtils.appendToBaselineTsv(dataset.getName(), wmcOverThreshold, cboOverThreshold, rfcOverThreshold);
        }
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));
    }
}