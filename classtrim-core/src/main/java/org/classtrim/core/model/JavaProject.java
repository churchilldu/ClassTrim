package org.classtrim.core.model;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.classtrim.core.config.DatasetEnum;
import org.classtrim.core.metric.Threshold;
import org.classtrim.core.util.ASMUtils;
import org.classtrim.core.util.AppProperties;
import org.classtrim.core.util.FileUtils;
import org.classtrim.core.parser.ClazzVisitor;
import org.classtrim.core.parser.CouplingVisitor;
import org.classtrim.core.parser.MethodInvocationVisitor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class JavaProject extends JavaObject {
    private static final long serialVersionUID = 3242194786299334773L;
    private final List<JavaClass> classList = new ArrayList<>();
    private final DatasetEnum dataSet;
    private final Threshold threshold;
    private List<JavaClass> classToRefactor;
    private List<JavaMethod> methodsToRefactor;

    public JavaProject(DatasetEnum dataSet) {
        super(dataSet.getName());
        this.dataSet = dataSet;
        this.threshold = dataSet.getThreshold();
    }

    private JavaProject(String projectName, Threshold threshold) {
        super(projectName);
        this.dataSet = null;
        this.threshold = threshold;
    }

    public static JavaProject load(DatasetEnum dataSet) {
        try {
            File file = getCacheFilePath(dataSet.getName()).toFile();
            if (file.exists()) {
                log.info("Load {} from cache.", dataSet.getName());
                return SerializationUtils.deserialize(new FileInputStream(file));
            }
            JavaProject project = new JavaProject(dataSet);
            project.parse();
            project.save();
            return project;
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static JavaProject parse(String projectName,
                                    Collection<String> classpathRoots,
                                    Threshold threshold) {
        JavaProject project = new JavaProject(projectName, threshold);
        project.parse(classpathRoots);
        return project;
    }

    public static JavaProject parse(String projectName,
                                    String classpathRoot,
                                    Threshold threshold) {
        return parse(projectName, Collections.singletonList(classpathRoot), threshold);
    }

    private void save() {
        try {
            File file = this.getCacheFilePath().toFile();
            if (file.isDirectory()) {
                throw new IllegalArgumentException("cannot overwrite directory");
            }
            FileOutputStream str = new FileOutputStream(file);
            SerializationUtils.serialize(this, str);
            str.close();
            log.info("Serialize {} to {}.", this.getName(), this.getCacheFilePath());
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public void parse() {
        parse(Collections.singletonList(dataSet.getPath()));
    }

    private void parse(Collection<String> classpathRoots) {
        List<String> classFiles = new ArrayList<>();
        for (String root : classpathRoots) {
            classFiles.addAll(Arrays.asList(FileUtils.getAllClassFiles(root)));
        }
        String[] allClassFiles = classFiles.toArray(new String[0]);
        Arrays.stream(allClassFiles).forEach(this::parseMethods);
        Arrays.stream(allClassFiles).forEach(this::parseInheritance);
        Arrays.stream(allClassFiles).forEach(this::parseInvocation);
    }

    private void parseMethods(String classFilePath) {
        this.parse(classFilePath, new ClazzVisitor(this));
    }

    private void parseInheritance(String classFilePath) {
        this.parse(classFilePath, new CouplingVisitor(this));
    }

    private void parseInvocation(String classFilePath) {
        this.parse(classFilePath, new MethodInvocationVisitor(this));
    }

    private void parse(String filePath, ClassVisitor visitor) {
        try (InputStream classFileInputStream = Files.newInputStream(Paths.get(filePath))) {
            ClassReader classReader = new ClassReader(classFileInputStream);
            classReader.accept(visitor, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public JavaClass createClass(String className) {
        JavaClass clazz = new JavaClass(className, this);
        classList.add(clazz);
        return clazz;
    }

    public Optional<JavaMethod> getMethod(String className, String methodName, String descriptor) {
        return this.findClass(className).flatMap(c -> c.findMethod(methodName, descriptor));
    }

    public Optional<JavaMethod> findMethodRecursively(String owner, String methodName, String descriptor) {
        Optional<JavaClass> c = this.findClass(owner);
        while (c.isPresent()) {
            if (c.get().getProject() == null && c.get().getDeclaredMethods().isEmpty()) {
                ASMUtils.loadMethodsToClass(c.get());
            }
            Optional<JavaMethod> method = c.get().findMethod(methodName, descriptor);
            if (method.isPresent()) {
                return method;
            }
            c = c.get().getSuperClass();
        }

        return Optional.empty();
    }

    /**
     * Find a class by its fully qualified name. 
     * Example: "org.refactor.model.JavaProject" or "org/refactor/model/JavaProject"
     * Accepts names separated by '.' or '/'; internally normalizes to '/'.
     * @param className The fully qualified name of the class to find.
     * @return An Optional containing the JavaClass if found, otherwise empty.
     */
    public Optional<JavaClass> findClass(String className) {
        String normalized = className.replace('.', '/');
        return classList.stream().filter(c -> normalized.equals(c.getName()))
                .findFirst();
    }

    public List<JavaClass> getClassCanRefactor() {
        if (classToRefactor == null) {
            classToRefactor = this.classList.stream()
                    .filter(JavaClass::canRefactor)
                    .collect(Collectors.toUnmodifiableList());
        }
        return classToRefactor;
    }

    public List<JavaMethod> getMethodsCanRefactor() {
        if (methodsToRefactor == null) {
            methodsToRefactor = this.getClassCanRefactor().stream()
                    .map(JavaClass::getDeclaredMethods)
                    .flatMap(List::stream)
                    .filter(JavaMethod::canRefactor)
                    .collect(Collectors.toUnmodifiableList());
        }
        return methodsToRefactor;
    }

    public Threshold getThreshold() {
        return this.threshold;
    }

    public List<JavaClass> getClassList() {
        return Collections.unmodifiableList(classList);
    }

    /**
     * Convert a JavaProject to a map with JavaClass and it's declaring methods.
     * @return JavaClass and it's declaring methods.
     */
    public Map<JavaClass, List<JavaMethod>> toMap() {
        return this.getClassCanRefactor()
                .stream().unordered()
                .collect(Collectors.toMap(Function.identity(), JavaClass::getDeclaredMethods));
    }


    private static Path getCacheFilePath(String projectName) {
        return Paths.get(AppProperties.getString("projectCacheFolder"), projectName);
    }

    private Path getCacheFilePath() {
        return getCacheFilePath(this.getName());
    }

}
