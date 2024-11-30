package org.example;

import org.example.common.DataSetConst;
import org.example.model.JavaClass;
import org.example.model.JavaMethod;
import org.example.model.JavaProject;
import org.example.util.MetricUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


public class MethodRefactoringProblem extends AbstractIntegerProblem {
    private final JavaProject project = new JavaProject(DataSetConst.Ant.THRESHOLD);
    private int[] fixedMethods;

    public MethodRefactoringProblem() {
        this.project.addSource(DataSetConst.Ant.output);
        this.setBounds();
        this.setFixedAssignments();
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
    }

    private void setFixedAssignments() {
        List<JavaClass> classList = project.getClassList();
        List<JavaMethod> methodList = project.getMethodList();
        fixedMethods = new int[methodList.size()];

        for (int i = 0; i < methodList.size(); i++) {
            JavaMethod m = methodList.get(i);
            if (m.canRefactor()) {
                fixedMethods[i] = -1;
            } else {
                fixedMethods[i] = classList.indexOf(m.getCls());
            }
        }
    }

    @Override
    public int numberOfObjectives() {
        return 2;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return null;
    }


    @Override
    public IntegerSolution createSolution() {
        IntegerSolution solution = super.createSolution();

        for (int i = 0; i < numberOfVariables(); i++) {
            if (fixedMethods[i] < 0) {
                solution.variables().set(i,
                        JMetalRandom.getInstance().nextInt(
                                solution.getBounds(i).getLowerBound(),
                                solution.getBounds(i).getUpperBound()));
            } else {
                solution.variables().set(i, fixedMethods[i]);
            }
        }

        return solution;

    }

    @Override
    public IntegerSolution evaluate(IntegerSolution solution) {
        // WMC
        solution.objectives()[0] = MetricUtils.evalWMC(project, solution.variables());
        // CBO
        solution.objectives()[1] = MetricUtils.evalCBO(project, solution.variables());

        return solution;
    }

}

