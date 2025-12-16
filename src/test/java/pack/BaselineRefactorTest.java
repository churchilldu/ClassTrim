package pack;

import org.junit.Test;
import org.classtrim.baseline.BaselineRefactor;
import org.classtrim.common.DatasetEnum;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class BaselineRefactorTest {

    @Test
    public void testConvertToMapMatchesExpected() throws Exception {
        JavaProject project = new JavaProject(DatasetEnum.TEST);
        project.parse();

        // Invoke private static method convertToMap(JavaProject)
        Method convertToMap = BaselineRefactor.class.getDeclaredMethod("convertToMap", JavaProject.class);
        convertToMap.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<JavaClass, List<JavaMethod>> actual = (Map<JavaClass, List<JavaMethod>>) convertToMap.invoke(null, project);

        // Expected: same logic as production method
        Map<JavaClass, List<JavaMethod>> expected = project.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), c -> new ArrayList<>(c.getDeclaredMethods())));

        assertEquals(expected, actual);
    }
}



