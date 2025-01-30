import org.junit.Assert;
import org.junit.Before;
import org.objectweb.asm.Type;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.junit.Test;
import org.refactor.util.MetricUtils;

import java.util.Optional;

public class JavaProjectTest {
    private static final DataSet TEST_DATA = new DataSet(
            "pack",
            "",
            "target/test-classes/pack",
            new Threshold(0, 1, 1)
    );
    private static final String SUPER_CLASS = "pack/SuperClass";

    private JavaProject project;

    @Before
    public void setup() {
        project = new JavaProject(TEST_DATA);
        project.start();
    }

    @Test
    public void testOverride() {
        Optional<JavaMethod> inheritMethod = project.getOrCreateClass("pack/ClassA")
                .getMethod("inheritMethod", Type.getMethodDescriptor(Type.VOID_TYPE));
        Assert.assertTrue(inheritMethod.isPresent());
        Assert.assertFalse(inheritMethod.get().canRefactor());

        Optional<JavaMethod> compareTo = project.getOrCreateClass("pack/ClassA")
                .getMethod("compareTo",
                        Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(Object.class)));
        Assert.assertTrue(compareTo.isPresent());
        Assert.assertFalse(compareTo.get().canRefactor());
    }

    @Test
    public void testGetterSetter() {
        Optional<JavaMethod> getter = project.getOrCreateClass("pack/ClassA")
                .getMethod("getStr", Type.getMethodDescriptor(Type.getType(String.class)));
        Assert.assertTrue(getter.isPresent());
        Assert.assertFalse(getter.get().canRefactor());

        Optional<JavaMethod> setter = project.getOrCreateClass("pack/ClassA")
                .getMethod("setStr", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Integer.class)));
        Assert.assertTrue(setter.isPresent());
        Assert.assertFalse(setter.get().canRefactor());
    }

    @Test
    public void testMetrics() {
    }

}
