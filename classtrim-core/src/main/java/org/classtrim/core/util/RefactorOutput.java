package org.classtrim.core.util;


import org.classtrim.core.metric.MetricUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.classtrim.core.config.AlgorithmParameter;
import org.classtrim.core.model.JavaClass;
import org.classtrim.core.model.JavaMethod;
import org.classtrim.core.model.JavaProject;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactorOutput {
    private final JavaProject project;
    private final String projectName;
    private final List<IntegerSolution> solutions;
    private final String folderId;
    private final Path outputPath;
    private final AlgorithmParameter parameter;

    public RefactorOutput(JavaProject project,
                          List<IntegerSolution> solutions,
                          AlgorithmParameter parameter) {
        this.project = project;
        this.projectName = project.getName();
        this.solutions = solutions;
        this.folderId = FileUtils.getFolderId(projectName);
        this.outputPath = Paths.get(AppProperties.getString("outputFolder"), projectName, folderId);
        FileUtils.createDir(outputPath);
        this.parameter = parameter;
    }

    public static Map<JavaClass, List<JavaMethod>> convertSolution(JavaProject project, List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodsCanRefactor();
        List<JavaClass> classList = project.getClassCanRefactor();
        Map<JavaClass, List<JavaMethod>> methodsByClass = new HashMap<>();
        for (int methodId = 0; methodId < solution.size(); methodId++) {
            Integer classId = solution.get(methodId);
            methodsByClass.computeIfAbsent(classList.get(classId),
                    k -> new ArrayList<>()).add(methodList.get(methodId));
        }

        return methodsByClass;
    }

    public void write() {
        // Select the knee of the Pareto front (Das 1999, NBI) and put it first so that the
        // recommended refactoring is diff-01 / metrics-01. Only the 3 real objectives
        // (WMC, CBO, RFC) take part; guiding objectives are search heuristics only.
        List<IntegerSolution> ordered = new ArrayList<>(this.solutions);
        IntegerSolution knee = KneeSelection.select(ordered, 3);
        ordered.remove(knee);
        ordered.add(0, knee);

        algorithmResultOutput();
        solutionOutput(ordered);
        metricsOutput(ordered);
        kneeOutput(knee);
    }

    private void metricsOutput(List<IntegerSolution> ordered) {
        Map<JavaClass, List<JavaMethod>> before = this.project.toMap();
        int seq = 1;
        for (IntegerSolution solution : ordered) {
            Map<JavaClass, List<JavaMethod>> after = convertSolution(this.project, solution.variables());
            FileUtils.writeMetrics(getMetriceFilePath(seq),
                    MetricUtils.getMetricsOfClass(before), MetricUtils.getMetricsOfClass(after));
            seq++;
        }
    }

    private Path getMetriceFilePath(int seq) {
        return Paths.get(outputPath.toString(), projectName + "-" + "metrics" + "-" + String.format("%02d", seq)
                + ".tsv");
    }

    private void solutionOutput(List<IntegerSolution> ordered) {
        int seq = 1;
        for (IntegerSolution solution : ordered) {
            List<Triple<JavaMethod, JavaClass, JavaClass>> diff = new ArrayList<>();
            convertSolution(this.project, solution.variables()).forEach((clazz, methods) ->
                    {
                        for (JavaMethod method : methods) {
                            if (!clazz.equals(method.getClazz())) {
                                diff.add(new ImmutableTriple<>(method, method.getClazz(), clazz));
                            }
                        }
                    }
            );
            FileUtils.writeDiff(this.getDiffFilePath(seq), diff);
            seq++;
        }
    }

    private Path getDiffFilePath(int seq) {
        return Paths.get(outputPath.toString(), projectName + "-" + "diff" + "-" + String.format("%02d", seq)
                + ".tsv");
    }


    /**
     * Writes the objectives of the selected knee to KNEE.csv (a single-row CSV), so downstream
     * consumers (e.g. the Minecraft plugin / human-eval pipeline) can identify the recommended
     * solution without re-deriving the knee.
     */
    private void kneeOutput(IntegerSolution knee) {
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                Paths.get(AppProperties.getString("outputFolder"), projectName,
                        projectName + "-" + "KNEE.csv"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("WMC" + "," + "CBO" + "," + "RFC");
            writer.newLine();
            writer.write(knee.objectives()[0] + "," + knee.objectives()[1] + "," + knee.objectives()[2]);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write knee output", e);
        }
    }

    @SuppressWarnings("rawtypes")
    // Append population size, generation, algorithm name, wmc, cbo, rfc, folderName.
    private void algorithmResultOutput() {
        Triple[] objectives = solutions.stream()
                .map(Solution::objectives)
                .map(o -> Triple.of(o[0], o[1], o[2]))
                .toArray(Triple[]::new);
        FileUtils.writeSummary(Paths.get(AppProperties.getString("outputFolder"), projectName, projectName + "-" + "summary.tsv"),
                parameter, objectives, folderId);

        new SolutionListOutput(solutions)
                .setVarFileOutputContext(new DefaultFileOutputContext(
                        outputPath + "/" + projectName + "-" + "VAR.csv", ","))
                .setFunFileOutputContext(new DefaultFileOutputContext(
                        outputPath + "/" + projectName + "-" + "FUN.csv", ","))
                .print();
    }
}
