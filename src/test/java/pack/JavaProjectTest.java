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
    public static final String CLASS_A = "pack/A";
    public static final String CLASS_B = "pack/B";
    public static final String CLASS_C = "pack/C";
    public static final String CLASS_D = "pack/D";
    public static final String CLASS_E = "pack/E";
    public static final String CLASS_F = "pack/F";
    public static final String CLASS_G = "pack/G";
    public static final String CLASS_H = "pack/H";
    public static final String CLASS_I = "pack/I";
    public static final String CLASS_J = "pack/J";

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

    @Test
    public void testCbo06() {
        this.doTest(CLASS_F);
    }

    @Test
    public void testCbo07() {
        this.doTest(CLASS_G);
    }

    @Test
    public void testCbo08() {
        this.doTest(CLASS_H);
    }

    @Test
    public void testCbo09() {
        this.doTest(CLASS_I);
    }

    @Test
    public void testCbo10() {
        this.doTest(CLASS_J);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    private void doTest(String className) {
        Optional<JavaClass> clazz = project.getClass(className);
        assertTrue(clazz.isPresent());
        Integer cbo = null;
        try {
            cbo = (Integer) Class.forName(clazz.get().toString())
                    .getField("CBO")
                    .get("CBO");
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(cbo, cboByClass.get(clazz.get()));
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassList()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }

}
