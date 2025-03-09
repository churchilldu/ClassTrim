package org.refactor;

import org.refactor.common.DatasetEnum;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.MetricUtils;
import org.refactor.util.ProjectUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;


public class RefactoringProblem extends AbstractIntegerProblem {
    private final JavaProject project;
    private final ObjectiveCalculator objectiveCalculator;

    public RefactoringProblem(DatasetEnum dataSet) {
        File file = new File(dataSet.getName());
        if (file.exists()) {
            this.project = JavaProject.load(dataSet.getName());
        } else {
            this.project = new JavaProject(dataSet);
            this.project.parse();
            this.project.save();
        }
        objectiveCalculator = new ObjectiveCalculator(project);
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
        return 6;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return "Method refactoring";
    }

    public JavaProject getProject() {
        return this.project;
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

        /* The following objectives is to guide algorithm to right direction. **/
        // WMC
        solution.objectives()[3] = objectiveCalculator.sumClassWmcOverThreshold();
        // CBO
        solution.objectives()[4] = objectiveCalculator.sumClassCboOverThreshold();
        // RFC
        solution.objectives()[5] = objectiveCalculator.sumClassRfcOverThreshold();

        return solution;
    }

    private static class ObjectiveCalculator {
        private Map<JavaClass, List<JavaMethod>> methodsByClass;
        private final JavaProject project;

        private ObjectiveCalculator(JavaProject project) {
            this.project = project;
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
            return MetricUtils.sumClassWmcOverThreshold(methodsByClass, project.getThreshold().getWMC());
        }

        private long sumClassCboOverThreshold() {
            return MetricUtils.sumClassCboOverThreshold(methodsByClass, project.getThreshold().getCBO());
        }

        private long sumClassRfcOverThreshold() {
            return MetricUtils.sumClassRfcOverThreshold(methodsByClass, project.getThreshold().getRFC());
        }
        private long countClassWmcOverThreshold() {
            return MetricUtils.countClassWmcOverThreshold(methodsByClass, project.getThreshold().getWMC());
        }

        private long countClassCboOverThreshold() {
            return MetricUtils.countClassCboOverThreshold(methodsByClass, project.getThreshold().getCBO());
        }

        private long countClassRfcOverThreshold() {
            return MetricUtils.countClassRfcOverThreshold(methodsByClass, project.getThreshold().getRFC());
        }
    }
}

