package org.example;

import org.example.model.JavaClass;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;
import org.example.util.MetricUtils;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.*;


public class ClassRefactoringProblem extends AbstractIntegerProblem {

    private static final String dataSetPath = "C:/codeRefactoring/datasource/xom-1.2.1/output";
    //    private static final String dataSetPath = "C:/codeRefactoring/datasource/mango/output";

    private final JavaProject project = new JavaProject();

    public ClassRefactoringProblem() {
        this.project.addSource(dataSetPath);
        project.save(project.getName());

        // ID, package
        List<JavaPackage> packageList = project.getPackageList();
        // ID, class
        List<JavaClass> classList = project.getClassList();

        int numberOfPackage = packageList.size();
        int numberOfClass = classList.size();

        List<Integer> lowerLimit = new ArrayList<>(numberOfClass);
        List<Integer> upperLimit = new ArrayList<>(numberOfClass);

        for (int i = 0; i < numberOfClass; i++) {
            lowerLimit.add(0);
            upperLimit.add(numberOfPackage - 1);
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
        // packageID, List<classID>
        Map<Integer, List<Integer>> classByPackage = new HashMap<>();

        List<Integer> variables = solution.variables();
        for (int i = 0; i < variables.size(); i++) {
            Integer packID = variables.get(i);
            Integer clsID = i;

            classByPackage.computeIfAbsent(packID, k -> new ArrayList<>()).add(clsID);
        }

        // cohesion
        solution.objectives()[0] = -MetricUtils.evalCohesion(project, classByPackage);
        // coupling
        solution.objectives()[1] = MetricUtils.evalCoupling(project, classByPackage);

        return solution;
    }

}

