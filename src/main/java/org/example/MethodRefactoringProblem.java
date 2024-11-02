package org.example;

import org.example.model.JavaClass;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;
import org.example.util.MetricUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MethodRefactoringProblem extends AbstractIntegerProblem {

    private static final String dataSetPath = "C:/codeRefactoring/datasource/xom-1.2.1/output";
    //    private static final String dataSetPath = "C:/codeRefactoring/datasource/mango/output";

    private final JavaProject project = new JavaProject();

    public MethodRefactoringProblem() {
        this.project.addSource(dataSetPath);
        project.save(project.getName());

        int numberOfMethod = project.getMethodList().size();

        List<Integer> lowerLimit = new ArrayList<>(numberOfMethod);
        List<Integer> upperLimit = new ArrayList<>(numberOfMethod);
        for (int i = 0; i < numberOfMethod; i++) {
            lowerLimit.add(0);
            upperLimit.add(numberOfMethod - 1);
        }
        super.variableBounds(lowerLimit, upperLimit);
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
    public IntegerSolution evaluate(IntegerSolution solution) {
        // classId, List<methodId>
        Map<Integer, List<Integer>> methodsByClas = new HashMap<>();

        List<Integer> variables = solution.variables();
        for (int i = 0; i < variables.size(); i++) {
            Integer classId = variables.get(i);
            Integer methodId = i;

            methodsByClas.computeIfAbsent(classId, k -> new ArrayList<>()).add(methodId);
        }

        // cohesion
        solution.objectives()[0] = MetricUtils.evalWMC(project, methodsByClas);
        // coupling
        solution.objectives()[1] = MetricUtils.evalCBO(project, solution.variables());

        return solution;
    }

}

