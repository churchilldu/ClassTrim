package org.refactor.util;


import org.refactor.model.JavaClass;
import org.refactor.model.JavaProject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProjectUtils {
    public static long countClassWmcOverThreshold(JavaProject project) {
        Map<JavaClass, Integer> wmcByClass = new HashMap<>();
        project.getClassList().forEach(cls -> {
            cls.getDeclaredMethodList().forEach(m -> {
                        wmcByClass.merge(cls, m.getComplexity(), Integer::sum);
                    }
            );
        });

        return wmcByClass.values().parallelStream().filter(
                wmc -> wmc > project.getThreshold().getWMC()
        ).count();
    }

    public static long countClassCboOverThreshold(JavaProject project) {
        Map<JavaClass, Set<JavaClass>> cboByClass = new HashMap<>();
        project.getClassList().forEach(cls -> {
            cls.getInvokeMethodList().forEach(m -> {
                JavaClass clsOnCall = m.getCls();
                if (!cls.equals(clsOnCall)) {
                    cboByClass.computeIfAbsent(cls, k -> new HashSet<>()).add(clsOnCall);
                }
            });
        });

        return cboByClass.entrySet().stream().filter(
                entry -> {
                    JavaClass cls = entry.getKey();
                    Set<JavaClass> dependencies = entry.getValue();

                    return cls.getOuterCbo() + dependencies.size() > project.getThreshold().getCBO();
                }
        ).count();
    }

}
