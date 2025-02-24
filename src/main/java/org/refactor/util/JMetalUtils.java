package org.refactor.util;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMetalUtils {
    public Map<JavaClass, List<JavaMethod>> convertSolution(JavaProject project, List<Integer> solution) {
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

    public void diffOutput(Map<JavaClass, List<JavaMethod>> methodsByClass, String fileName) {
        List<Triple<JavaMethod, JavaClass, JavaClass>> diff = new ArrayList<>();

        methodsByClass.forEach(
                (clazz, methods) -> {
                    for (JavaMethod method : methods) {
                        if (!clazz.equals(method.getClazz())) {
                            diff.add(new ImmutableTriple<>(method, method.getClazz(), clazz));
                        }
                    }
                }
        );

        FileUtils.write(fileName, diff);
    }

}
