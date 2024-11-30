import org.example.model.JavaProject;
import org.junit.Test;

public class JavaProjectTest {
    @Test
    public void test() {
        JavaProject project = new JavaProject(null);
        project.setName("file");
        project.addSource("target/test-classes/file");

        project.addSource("C:/codeRefactoring/datasource/xom-1.2.1/output/nu");
    }
}
