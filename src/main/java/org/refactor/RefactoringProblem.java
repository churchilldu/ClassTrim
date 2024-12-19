package org.refactor;

import org.refactor.common.DataSet;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.refactor.util.MetricUtils;
import org.refactor.util.ObjectiveCalculator;
import org.refactor.util.ProjectUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.JMetalLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;


public class RefactoringProblem extends AbstractIntegerProblem {
    private final JavaProject project;
    private final ObjectiveCalculator objectiveCalculator;
    private int[] fixedMethods;

    public RefactoringProblem(DataSet dataSet) {
        this.project = new JavaProject(dataSet);
        objectiveCalculator = new ObjectiveCalculator(project);
        this.project.startParse();
        this.setBounds();
        this.initFixedAssignments();

        JMetalLogger.logger.info("Original number of class exceeds WMC threshold = " + ProjectUtils.countClassWmcOverThreshold(project));
        JMetalLogger.logger.info("Original number of class exceeds CBO threshold = " + ProjectUtils.countClassCboOverThreshold(project));
        JMetalLogger.logger.info("Original number of class exceeds RFC threshold = " + ProjectUtils.countClassRfcOverThreshold(project));
    }

    private void setBounds() {
        int numberOfMethod = project.getMethodList().size();
        int numberOfClass = project.getClassList().size();

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

    private void initFixedAssignments() {
        List<JavaClass> classList = project.getClassList();
        List<JavaMethod> methodList = project.getMethodList();
        fixedMethods = new int[methodList.size()];

        for (int i = 0; i < methodList.size(); i++) {
            JavaMethod m = methodList.get(i);
            if (m.canRefactor()) {
                fixedMethods[i] = -1;
            } else {
                fixedMethods[i] = classList.indexOf(m.getClazz());
            }
        }

        JMetalLogger.logger.info("Number of method to refactor = " + Arrays.stream(fixedMethods).filter(i -> i < 0).count());
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


    @Override
    public IntegerSolution createSolution() {
        IntegerSolution solution = super.createSolution();

        for (int i = 0; i < numberOfVariables(); i++) {
            if (fixedMethods[i] > 0) {
                solution.variables().set(i, fixedMethods[i]);
            }
        }

        return solution;
    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        objectiveCalculator.setSolution(solution.variables());
        // WMC
        solution.objectives()[0] = objectiveCalculator.sumClassWmcOverThreshold();
        // CBO
        solution.objectives()[1] = objectiveCalculator.sumClassCboOverThreshold();
        // RFC
        solution.objectives()[2] = objectiveCalculator.sumClassRfcOverThreshold();
        // WMC
        solution.objectives()[3] = objectiveCalculator.countClassWmcOverThreshold();
        // CBO
        solution.objectives()[4] = objectiveCalculator.countClassCboOverThreshold();
        // RFC
        solution.objectives()[5] = objectiveCalculator.countClassRfcOverThreshold();

        return solution;
    }

}

