package org.refactor.util;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefactorOutput {
    private JavaProject project;
    private List<IntegerSolution> solutions;
    public RefactorOutput(JavaProject project, List<IntegerSolution> solutions) {
        this.project = project;
        this.solutions = solutions;
    }

    public static Map<JavaClass, List<JavaMethod>> convertSolution(JavaProject project, List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodsToRefactor();
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

    public void diffOutput() {
        int seq = 1;
        for (IntegerSolution solution : this.solutions) {
            List<Triple<JavaMethod, JavaClass, JavaClass>> diff = new ArrayList<>();
            convertSolution(this.project, solution.variables()).forEach((clazz, methods) -> {
                        for (JavaMethod method : methods) {
                            if (!clazz.equals(method.getClazz())) {
                                diff.add(new ImmutableTriple<>(method, method.getClazz(), clazz));
                            }
                        }
                    }
            );
            FileUtils.writeDiff(project.getName() + "-" + "diff" + "-" + seq, diff);
            seq++;
        }
    }

}
