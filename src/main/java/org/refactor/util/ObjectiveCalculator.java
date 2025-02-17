package org.refactor.util;

import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectiveCalculator {
    private Map<JavaClass, List<JavaMethod>> methodsByClass;
    private final JavaProject project;

    public ObjectiveCalculator(JavaProject project) {
        this.project = project;
    }

    public void setSolution(List<Integer> solution) {
        List<JavaMethod> methodList = project.getMethodsToRefactor();
        List<JavaClass> classList = project.getClassCanRefactor();
        methodsByClass = new HashMap<>();
        for (int methodId = 0; methodId < solution.size(); methodId++) {
            Integer classId = solution.get(methodId);
            methodsByClass.computeIfAbsent(
                    classList.get(classId),
                    k -> new ArrayList<>()).add(methodList.get(methodId));
        }
    }

    public long sumClassWmcOverThreshold() {
        return MetricUtils.sumClassWmcOverThreshold(
                methodsByClass,
                project.getThreshold().getWMC());
    }

    public long sumClassCboOverThreshold() {
        return MetricUtils.sumClassCboOverThreshold(
                methodsByClass,
                project.getThreshold().getCBO()
        );
    }

    public long sumClassRfcOverThreshold() {
        return MetricUtils.sumClassRfcOverThreshold(
                methodsByClass,
                project.getThreshold().getRFC()
        );
    }
    public long countClassWmcOverThreshold() {
        return MetricUtils.countClassWmcOverThreshold(
                methodsByClass,
                project.getThreshold().getWMC());
    }

    public long countClassCboOverThreshold() {
        return MetricUtils.countClassCboOverThreshold(
                methodsByClass,
                project.getThreshold().getCBO()
        );
    }

    public long countClassRfcOverThreshold() {
        return MetricUtils.countClassRfcOverThreshold(
                methodsByClass,
                project.getThreshold().getRFC()
        );
    }
}
