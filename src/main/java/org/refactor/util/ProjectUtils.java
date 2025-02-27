package org.refactor.util;


import org.refactor.common.DatasetConst;
import org.refactor.common.DatasetEnum;
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

    public static void main(String[] argv) {
        JavaProject project = new JavaProject(DatasetEnum.ANT_7);
        project.start();
        Map<JavaClass, List<JavaMethod>> map = convertToMap(project);
        Map<JavaClass, Integer> wmcOfClass = MetricUtils.getWmcOfClass(map);
        Map<JavaClass, Integer> cboOfClass = MetricUtils.getCboOfClass(map);
        Map<JavaClass, Integer> rfcOfClass = MetricUtils.getRfcOfClass(map);
        FileUtils.write("fixed-wmc-ant7.csv", "wmc", wmcOfClass);
        FileUtils.write("fixed-cbo-ant7.csv", "cbo", cboOfClass);
        FileUtils.write("fixed-rfc-ant7.csv", "rfc", rfcOfClass);
    }

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
