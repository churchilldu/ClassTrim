package org.refactor.util;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactorOutput {
    private final JavaProject project;
    private final List<IntegerSolution> solutions;
    private final String outputPath;
    private final Map<String, Object> configs;

    public RefactorOutput(JavaProject project,
                          List<IntegerSolution> solutions,
                          Map<String, Object> configs) {
        this.project = project;
        this.solutions = solutions;
        this.outputPath = FileUtils.getOutputPath(project.getName()) + "/";
        this.configs = configs;
    }

    public static Map<JavaClass, List<JavaMethod>> convertSolution(JavaProject project, List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodsCanRefactor();
        List<JavaClass> classList = project.getClassCanRefactor();
        Map<JavaClass, List<JavaMethod>> methodsByClass = new HashMap<>();
        for (int methodId = 0; methodId < solution.size(); methodId++) {
            Integer classId = solution.get(methodId);
            methodsByClass.computeIfAbsent(
                    classList.get(classId),
                    k -> new ArrayList<>()).add(methodList.get(methodId));
        }

        return methodsByClass;
    }

    public void write() {
        resultOutput();
        solutionOutput();
        configOutput();
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
            FileUtils.writeDiff(outputPath + project.getName() + "-" + "diff" + "-" + seq, diff);
            seq++;
        }
    }

    private void resultOutput() {
        new SolutionListOutput(solutions)
                .setVarFileOutputContext(new DefaultFileOutputContext(
                        outputPath + project.getName() + "-" + "VAR.csv", ","))
                .setFunFileOutputContext(new DefaultFileOutputContext(
                        outputPath + project.getName() + "-" + "FUN.csv", ","))
                .print();
    }

    private void configOutput() {
        FileUtils.writeLog(outputPath + "log.txt", configs);
    }
}
