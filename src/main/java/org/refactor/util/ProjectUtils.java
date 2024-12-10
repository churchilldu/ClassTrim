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
            cls.getDeclaredMethods().forEach(m -> {
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
            cls.getInvokedMethods().forEach(m -> {
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

                    return cls.getExternalCbo() + dependencies.size() > project.getThreshold().getCBO();
                }
        ).count();
    }

    public static long countClassRfcOverThreshold(JavaProject project) {
        Map<JavaClass, Integer> rfcByClass = new HashMap<>();
        project.getClassList().forEach(cls -> {
            cls.getDeclaredMethods().forEach(m -> {
                rfcByClass.merge(cls, m.getRfc(), Integer::sum);
            });
        });

        return rfcByClass.values().parallelStream().filter(
                rfc -> rfc > project.getThreshold().getRFC()
        ).count();
    }
}
