package org.example;

import org.example.model.JavaClass;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toSet;


public class CodeRefactoring extends AbstractIntegerProblem {

    private static final String dataSetPath = "C:\\codeRefactoring\\datasource\\xom-1.2.1\\output";
    //    private static final String dataSetPath = "C:\\codeRefactoring\\datasource\\mango\\output";

    private final JavaProject project = new JavaProject();

    public CodeRefactoring() {
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

        JavaProject nu = JavaProject.load("nu");

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
        solution.objectives()[0] = -evalCohesion(classByPackage);
        // coupling
        solution.objectives()[1] = evalCoupling(classByPackage);

        return solution;
    }

    private double evalCohesion(Map<Integer, List<Integer>> classByPackage) {
        AtomicReference<Double> cohensionOfAll = new AtomicReference<>((double) 0);

        // ID, package
        List<JavaPackage> packageList = project.getPackageList();
        // ID, class
        List<JavaClass> classList = project.getClassList();

        // Compute cohension of each package
        classByPackage.forEach((packageID, classIDList) -> {
            double cohensionOfPack = 0;
            JavaPackage pack = packageList.get(packageID);

            AtomicInteger EI = new AtomicInteger();
            AtomicInteger EX = new AtomicInteger();
            AtomicInteger EXB = new AtomicInteger();

            classIDList.forEach(
                    classID -> {
                        JavaClass cls = classList.get(classID);

                        // Super class
                        if (pack.equals(cls.getSuperClass().getPackage())) {
                            EI.getAndIncrement();
                        } else {
                            EX.getAndIncrement();
                        }

                        cls.getExtendedClass().forEach(
                                extendedCls -> {
                                    if (!pack.equals(extendedCls.getPackage())) {
                                        EXB.getAndIncrement();
                                    }
                                }
                        );

                        cls.getDependClass().forEach((dependedCls, weight) -> {
                            // Within same package
                            if (pack.equals(dependedCls.getPackage())) {
                                EI.getAndAdd(weight);
                            } else {
                                EX.getAndAdd(weight);
                            }
                        });

                        cls.getDerivedClass().forEach((derivedCls, weight) -> {
                            // Within same package
                            if (!derivedCls.getPackage().equals(pack)) {
                                EXB.getAndAdd(weight);
                            }
                        });
                    }
            );

            try {
                cohensionOfPack = EI.get() / (EI.get() + EX.get() + 0.5 * EXB.get());
            } catch (ArithmeticException e) {
                cohensionOfPack = 0;
            }

            double finalCohensionOfPack = cohensionOfPack;
            cohensionOfAll.updateAndGet(v -> v + finalCohensionOfPack);
        });

        return cohensionOfAll.get() / packageList.size();
    }

    private double evalCoupling(Map<Integer, List<Integer>> classByPackage) {
        AtomicReference<Double> couplingOfAll = new AtomicReference<>((double) 0);

        // ID, package
        List<JavaPackage> packageList = project.getPackageList();
        // ID, class
        List<JavaClass> classList = project.getClassList();

        classByPackage.forEach((packageID, classIDList) -> {
            double couplingOfPack = 0;
            JavaPackage pack = packageList.get(packageID);
            AtomicInteger PRE = new AtomicInteger();
            AtomicInteger PRA = new AtomicInteger();
            AtomicInteger WPR = new AtomicInteger();

            classIDList.forEach(
                    classID -> {
                        JavaClass cls = classList.get(classID);

                        // Super class
                        if (pack.equals(cls.getSuperClass().getPackage())) {
                            WPR.getAndIncrement();
                        } else {
                            PRE.getAndIncrement();
                        }

                        cls.getDependClass().forEach((dependedCls, weight) -> {
                            // Within same package
                            if (pack.equals(dependedCls.getPackage())) {
                                WPR.getAndAdd(weight);
                            } else {
                                PRE.getAndAdd(weight);
                            }
                        });

                        Set<JavaPackage> packRelyOn = cls.getExtendedClass().stream().map(JavaClass::getPackage).collect(toSet());
                        packRelyOn.addAll(cls.getDerivedClass().keySet().stream().map(JavaClass::getPackage).collect(toSet()));

                        if (packRelyOn.contains(pack)) {
                            PRA.getAndAdd(packRelyOn.size() - 1);
                        } else {
                            PRA.getAndAdd(packRelyOn.size());
                        }

                    }
            );

            try {
                couplingOfPack = 1.0 * (PRE.get() + PRA.get()) / (WPR.get() + PRE.get() + PRA.get());
            } catch (ArithmeticException e) {
                couplingOfPack = 0;
            }

            double finalCouplingOfPack = couplingOfPack;
            couplingOfAll.updateAndGet(v -> v + finalCouplingOfPack);
        });


        return couplingOfAll.get() / packageList.size();
    }


}

