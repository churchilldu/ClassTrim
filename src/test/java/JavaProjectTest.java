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
    private static final String SUPER_CLASS = "pack/SuperClass";
    private static final String CLASS_A = "pack/A";

    private JavaProject project;

    @Before
    public void setup() {
        project = new JavaProject(TEST_DATA);
        project.start();
    }



    @Test
    public void testCbo() {
        Map<JavaClass, Integer> cboByClass = MetricUtils.getCboByClass(convertToMap(project));

        Optional<JavaClass> classA = project.getClass(CLASS_A);
        assertTrue(classA.isPresent());
        assertEquals(Integer.valueOf(7), cboByClass.get(classA.get()));
    }

    @Test
    public void testOverride() {
        Optional<JavaMethod> inheritMethod = project.getOrCreateClass("pack/ClassA")
                .getMethod("inheritMethod", Type.getMethodDescriptor(Type.VOID_TYPE));
        assertTrue(inheritMethod.isPresent());
        Assert.assertFalse(inheritMethod.get().canRefactor());

        Optional<JavaMethod> compareTo = project.getOrCreateClass("pack/ClassA")
                .getMethod("compareTo",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(Object.class)));
        assertTrue(compareTo.isPresent());
        Assert.assertFalse(compareTo.get().canRefactor());
    }

    @Test
    public void testGetterSetter() {
        Optional<JavaMethod> getter = project.getOrCreateClass("pack/ClassA")
                .getMethod("getStr", Type.getMethodDescriptor(Type.getType(String.class)));
        assertTrue(getter.isPresent());
        Assert.assertFalse(getter.get().canRefactor());

        Optional<JavaMethod> setter = project.getOrCreateClass("pack/ClassA")
                .getMethod("setStr", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Integer.class)));
        assertTrue(setter.isPresent());
        Assert.assertFalse(setter.get().canRefactor());
    }

    @Test
    public void testMetrics() {
    }

    private static Map<JavaClass, List<JavaMethod>> convertToMap(JavaProject project) {
        return project.getClassList()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }

}
