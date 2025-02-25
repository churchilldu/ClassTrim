package org.refactor.util;


import org.refactor.common.DatasetConst;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProjectUtils.class);

    private static Set<JavaMethod> getResponseSetOf(JavaProject project, String className) {
        JavaClass clazz = project.findClass(className.replace(".", "/")).get();
        List<JavaMethod> methods = clazz.getDeclaredMethods();
        return Stream.concat(methods.stream(), methods.stream().map(JavaMethod::getInvokeMethods).flatMap(List::stream))
                .collect(Collectors.toSet());
    }

    public static long countClassWmcOverThreshold(JavaProject project) {
        return MetricUtils.countClassWmcOverThreshold(
                convertToMap(project),
                project.getThreshold().getWMC()
        );
    }

    public static long countClassCboOverThreshold(JavaProject project) {
        return MetricUtils.countClassCboOverThreshold(
                convertToMap(project),
                project.getThreshold().getCBO()
        );
    }

    public static long countClassRfcOverThreshold(JavaProject project) {
        return MetricUtils.countClassRfcOverThreshold(
                convertToMap(project),
                project.getThreshold().getRFC()
        );
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }
}
