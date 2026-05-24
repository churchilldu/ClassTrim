package org.classtrim.core.engine;

import lombok.Getter;
import org.classtrim.core.config.DatasetEnum;
import org.classtrim.core.metric.Threshold;
import org.classtrim.core.model.JavaClass;
import org.classtrim.core.model.JavaMethod;
import org.classtrim.core.model.JavaProject;
import org.classtrim.core.metric.MetricUtils;
import org.classtrim.core.metric.ProjectUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


public class RefactoringProblem extends AbstractIntegerProblem {
    @Getter
    private final JavaProject project;
    private final Threshold threshold;
    private final boolean useGuidingObjectives;
    private final ObjectiveCalculator objectiveCalculator;

    public RefactoringProblem(DatasetEnum dataset) {
        this(JavaProject.load(dataset), dataset.getThreshold(), true);
    }

    public RefactoringProblem(JavaProject project, Threshold threshold) {
        this(project, threshold, true);
    }

    public RefactoringProblem(JavaProject project, Threshold threshold, boolean useGuidingObjectives) {
        this.project = project;
        this.threshold = threshold;
        this.useGuidingObjectives = useGuidingObjectives;
        objectiveCalculator = new ObjectiveCalculator(project, threshold);
        this.setBounds();

        JMetalLogger.logger.info("Original number of class exceeds WMC threshold = " + ProjectUtils.countClassWmcOverThreshold(project));
        JMetalLogger.logger.info("Original number of class exceeds CBO threshold = " + ProjectUtils.countClassCboOverThreshold(project));
        JMetalLogger.logger.info("Original number of class exceeds RFC threshold = " + ProjectUtils.countClassRfcOverThreshold(project));
    }

    private void setBounds() {
        int numberOfMethod = project.getMethodsCanRefactor().size();
        int numberOfClass = project.getClassCanRefactor().size();

        List<Integer> lowerLimit = new ArrayList<>(numberOfMethod);
        List<Integer> upperLimit = new ArrayList<>(numberOfMethod);
        IntStream.range(0, numberOfMethod).forEach(k -> {
            lowerLimit.add(0);
            upperLimit.add(numberOfClass - 1);
        });

        super.variableBounds(lowerLimit, upperLimit);

        JMetalLogger.logger.info("Number of class = " + numberOfClass);
        JMetalLogger.logger.info("Number of method = " + numberOfMethod);
    }

    @Override
    public int numberOfObjectives() {
        return useGuidingObjectives ? 6 : 3;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return "Method refactoring";
    }

    public IntegerSolution createSolution() {
        IntegerSolution solution = super.createSolution();

        List<JavaClass> classList = project.getClassCanRefactor();
        List<JavaMethod> methodList = project.getMethodsCanRefactor();
        for (int i = 0; i < numberOfVariables(); i++) {
            solution.variables().set(i, classList.indexOf(methodList.get(i).getClazz()));
        }

        return solution;
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        objectiveCalculator.setSolution(solution.variables());
        // WMC
        solution.objectives()[0] = objectiveCalculator.countClassWmcOverThreshold();
        // CBO
        solution.objectives()[1] = objectiveCalculator.countClassCboOverThreshold();
        // RFC
        solution.objectives()[2] = objectiveCalculator.countClassRfcOverThreshold();

        if (useGuidingObjectives) {
            /** The following objectives is to guide algorithm to right direction. */
            // WMC
            solution.objectives()[3] = objectiveCalculator.sumClassWmcOverThreshold();
            // CBO
            solution.objectives()[4] = objectiveCalculator.sumClassCboOverThreshold();
            // RFC
            solution.objectives()[5] = objectiveCalculator.sumClassRfcOverThreshold();
        }

        return solution;
    }

    private static class ObjectiveCalculator {
        private Map<JavaClass, List<JavaMethod>> methodsByClass;
        private final JavaProject project;
        private final Threshold threshold;

        private ObjectiveCalculator(JavaProject project, Threshold threshold) {
            this.project = project;
            this.threshold = threshold;
        }

        private void setSolution(List<Integer> solution) {
            List<JavaMethod> methodList = project.getMethodsCanRefactor();
            List<JavaClass> classList = project.getClassCanRefactor();
            methodsByClass = new HashMap<>();
            for (int methodId = 0; methodId < solution.size(); methodId++) {
                Integer classId = solution.get(methodId);
                methodsByClass.computeIfAbsent(
                        classList.get(classId),
                        k -> new ArrayList<>()).add(methodList.get(methodId));
            }
            methodsByClass.forEach((clazz, methods) -> methods.addAll(clazz.getFixedMethods()));
        }

        private long sumClassWmcOverThreshold() {
            return MetricUtils.sumClassWmcOverThreshold(methodsByClass, threshold.getWMC());
        }

        private long sumClassCboOverThreshold() {
            return MetricUtils.sumClassCboOverThreshold(methodsByClass, threshold.getCBO());
        }

        private long sumClassRfcOverThreshold() {
            return MetricUtils.sumClassRfcOverThreshold(methodsByClass, threshold.getRFC());
        }
        private long countClassWmcOverThreshold() {
            return MetricUtils.countClassWmcOverThreshold(methodsByClass, threshold.getWMC());
        }

        private long countClassCboOverThreshold() {
            return MetricUtils.countClassCboOverThreshold(methodsByClass, threshold.getCBO());
        }

        private long countClassRfcOverThreshold() {
            return MetricUtils.countClassRfcOverThreshold(methodsByClass, threshold.getRFC());
        }
    }
}
