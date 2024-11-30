package org.example.util;

import org.example.model.JavaClass;
import org.example.model.JavaMethod;
import org.example.model.JavaProject;

import java.util.*;


public class MetricUtils {

    // WMC Weighted Method per Class
    public static long evalWMC(JavaProject project, List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodList();
        List<JavaClass> classList = project.getClassList();

        Map<Integer, Integer> wmcByClass = new HashMap<>();
        for (int classId = 0; classId < classList.size(); classId++) {
            JavaClass cls = classList.get(classId);

            for (JavaMethod m : cls.getDeclaredMethodList()) {
                int methodId = methodList.indexOf(m);
                Integer newClassId = solution.get(methodId);
                if (newClassId.equals(classId)) {
                    wmcByClass.merge(classId, m.getComplexity(), Integer::sum);
                }
            }
        }

        return wmcByClass.values().parallelStream().filter(
                wmc -> wmc > project.getThreshold().getWMC()
        ).count();
    }

    // CBO Coupling Between Objects
    public static long evalCBO(JavaProject project, List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodList();
        List<JavaClass> classList = project.getClassList();

        Map<Integer, Set<Integer>> cboByClass = new HashMap<>();
        for (int classId = 0; classId < classList.size(); classId++) {
            JavaClass cls = classList.get(classId);

            for (JavaMethod m : cls.getInvokeMethodList()) {
                int methodId = methodList.indexOf(m);
                Integer newClassId = solution.get(methodId);
                if (!newClassId.equals(classId)) {
                    cboByClass.computeIfAbsent(classId, k -> new HashSet<>()).add(newClassId);
                }
            }
        }

        return cboByClass.values().parallelStream().map(Set::size).filter(
                cbo -> cbo > project.getThreshold().getCBO()
        ).count();
    }

    // LCOM Lack of Cohesion in Methods
}


