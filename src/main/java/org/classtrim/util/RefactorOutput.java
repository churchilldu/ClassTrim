package org.classtrim.util;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.classtrim.common.AlgorithmParameter;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
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
        algorithmResultOutput();
        solutionOutput();
        metricsOutput();
    }

    private void metricsOutput() {
        Map<JavaClass, List<JavaMethod>> before = this.project.toMap();
        int seq = 1;
        for (IntegerSolution solution : this.solutions) {
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

    private void solutionOutput() {
        int seq = 1;
        for (IntegerSolution solution : this.solutions) {
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
