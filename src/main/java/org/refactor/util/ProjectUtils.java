package org.refactor.util;


import org.refactor.common.DataSetConst;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ProjectUtils {
    private static final Logger logger = LoggerFactory.getLogger(ProjectUtils.class);

    public static void main(String[] args) {
        JavaProject project = new JavaProject(DataSetConst.ANT_7);
        project.start();
        FileUtils.write("cbo.csv", MetricUtils.getCboByClass(convertToMap(project)));
        FileUtils.write("rfc.csv", MetricUtils.getRfcByClass(convertToMap(project)));
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
