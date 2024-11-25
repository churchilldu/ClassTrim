import org.example.model.JavaProject;
import org.junit.Test;

public class JavaProjectTest {
    @Test
    public void test() {
        JavaProject project = new JavaProject();
        project.addSource("target/test-classes/file");
        project.getMethodToRefactor();
    }
}
