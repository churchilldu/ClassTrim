package org.example.util;

import org.example.model.JavaClass;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


public class MetricUtils {

    // Coupling
    public static double evalCoupling(JavaProject project, Map<Integer, List<Integer>> classByPackage) {
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

            for (Integer classID : classIDList) {
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

                Set<JavaPackage> packRelyOn = cls.getExtendedClass().stream().map(JavaClass::getPackage).collect(Collectors.toSet());
                packRelyOn.addAll(cls.getDerivedClass().keySet().stream().map(JavaClass::getPackage).collect(Collectors.toSet()));

                if (packRelyOn.contains(pack)) {
                    PRA.getAndAdd(packRelyOn.size() - 1);
                } else {
                    PRA.getAndAdd(packRelyOn.size());
                }

            }

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

    // Cohension
    public static double evalCohesion(JavaProject project, Map<Integer, List<Integer>> classByPackage) {
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

            for (Integer classID : classIDList) {
                JavaClass cls = classList.get(classID);

                // Super class
                if (pack.equals(cls.getSuperClass().getPackage())) {
                    EI.getAndIncrement();
                } else {
                    EX.getAndIncrement();
                }

                cls.getExtendedClass().forEach(extendedCls -> {
                    if (!pack.equals(extendedCls.getPackage())) {
                        EXB.getAndIncrement();
                    }
                });

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

    // LOC Line of code

    // NOA Number of attribute

    // NOM Number of Methods
    // Metric related to size doesn't change after refactor.

    // WMC Weighted Method per Class

    // LCOM Lack of Cohesion in Methods

    // CBO Coupling Between Objects

    // DIT Depth of Inheritance Tree

    // NOC Number of Children
}

