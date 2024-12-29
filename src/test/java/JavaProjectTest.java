import org.junit.Assert;
import org.junit.Before;
import org.objectweb.asm.Type;
import org.refactor.common.DataSet;
import org.refactor.common.Threshold;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;
import org.junit.Test;

import java.util.Optional;

public class JavaProjectTest {
    public static final DataSet TEST = new DataSet(
            "file",
           "" ,
            "target/test-classes/file",
            new Threshold(0, 1, 1)
    );

    private JavaProject project;

    @Before
    public void setup() {
        project = new JavaProject(TEST);
        project.start();
    }

    @Test
    public void testOverride() {
        Optional<JavaMethod> inheritMethod = project.getOrCreateClass("file/ClassA")
                .getMethod("inheritMethod", Type.getMethodDescriptor(Type.VOID_TYPE));
        Assert.assertTrue(inheritMethod.isPresent());
        Assert.assertFalse(inheritMethod.get().canRefactor());

        Optional<JavaMethod> compareTo = project.getOrCreateClass("file/ClassA")
                .getMethod("compareTo",
                Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(Object.class)));
        Assert.assertTrue(compareTo.isPresent());
        Assert.assertFalse(compareTo.get().canRefactor());
    }

    @Test
    public void testGetterSetter() {
        Optional<JavaMethod> getter = project.getOrCreateClass("file/ClassA")
                .getMethod("getStr", Type.getMethodDescriptor(Type.getType(String.class)));
        Assert.assertTrue(getter.isPresent());
        Assert.assertFalse(getter.get().canRefactor());

        Optional<JavaMethod> setter = project.getOrCreateClass("file/ClassA")
                .getMethod("setStr", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Integer.class)));
        Assert.assertTrue(setter.isPresent());
        Assert.assertFalse(setter.get().canRefactor());
    }

    @Test
    public void testMetrics() {
    }
}
