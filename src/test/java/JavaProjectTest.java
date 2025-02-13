import org.junit.Assert;
import org.junit.Before;
import org.objectweb.asm.Type;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.junit.Test;
import org.refactor.util.MetricUtils;
import org.refactor.util.ProjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private JavaProject project;

    @Before
    public void setup() {
        project = new JavaProject(TEST_DATA);
        project.start();
    }

    @Test
    public void testCbo01() {
        Map<JavaClass, Integer> cboByClass = MetricUtils.getCboByClass(convertToMap(project));

        Optional<JavaClass> classA = project.getClass(CLASS_A);
        assertTrue(classA.isPresent());
        assertEquals(Integer.valueOf(7), cboByClass.get(classA.get()));
    }

    @Test
    public void testCbo02() {
        Map<JavaClass, Integer> cboByClass = MetricUtils.getCboByClass(convertToMap(project));

        Optional<JavaClass> classB = project.getClass(CLASS_B);
        assertTrue(classB.isPresent());
        assertEquals(Integer.valueOf(1), cboByClass.get(classB.get()));
    }

    @Test
    public void testCbo03() {
        Map<JavaClass, Integer> cboByClass = MetricUtils.getCboByClass(convertToMap(project));

        Optional<JavaClass> classC = project.getClass(CLASS_C);
        assertTrue(classC.isPresent());
        assertEquals(Integer.valueOf(1), cboByClass.get(classC.get()));
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassList()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }

}
