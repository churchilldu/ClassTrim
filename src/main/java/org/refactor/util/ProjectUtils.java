package org.refactor.util;


import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectUtils {
    public static long calculateWMC(JavaProject project) {
        List<JavaClass> classList = project.getClassList();

        Map<JavaClass, Integer> wmcByClass = new HashMap<>();
        classList.parallelStream().forEach(
                cls -> {
                    cls.getDeclaredMethodList().forEach(
                            m -> {
                                wmcByClass.merge(cls, m.getComplexity(), Integer::sum);
                            }
                    );
                }
        );

        return wmcByClass.values().parallelStream().filter(
                wmc -> wmc > project.getThreshold().getWMC()
        ).count();
    }
}
