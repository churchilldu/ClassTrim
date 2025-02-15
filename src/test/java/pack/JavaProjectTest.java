package pack;

import org.junit.Before;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.junit.Test;
import org.refactor.util.MetricUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JavaProjectTest {
    private static final DataSet TEST_DATA = new DataSet(
            "pack",
            "",
            "target/test-classes/pack",
            new Threshold(0, 1, 1)
    );
    private static final String CLASS_A = "pack/A";
    private static final String CLASS_B = "pack/B";
    private static final String CLASS_C = "pack/C";
    private static final String CLASS_D = "pack/D";
    private static final String CLASS_E = "pack/E";

    private JavaProject project;
    private Map<JavaClass, Integer> cboByClass;

    @Before
    public void setup() {
        project = new JavaProject(TEST_DATA);
        project.start();
        cboByClass = MetricUtils.getCboByClass(convertToMap(project));
    }

    @Test
    public void testCbo01() {
        this.doTest(CLASS_A);
    }

    @Test
    public void testCbo02() {
        this.doTest(CLASS_B);
    }

    @Test
    public void testCbo03() {
        this.doTest(CLASS_C);
    }

    @Test
    public void testCbo04() {
        this.doTest(CLASS_D);
    }

    @Test
    public void testCbo05() {
        this.doTest(CLASS_E);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void doTest(String className) {
        Optional<JavaClass> classA = project.getClass(className);
        assertTrue(classA.isPresent());
        try {
            assertEquals(Class.forName(classA.get().toString()).getField("CBO").get("CBO"),
                    cboByClass.get(classA.get()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassList()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }

}
