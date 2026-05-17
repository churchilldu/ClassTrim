package org.classtrim.smellydemo.verify;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loader / inventory utility for the smelly-demo bytecode verification harness.
 *
 * <p>Resolves and loads the eight Demo_Classes shipped by the
 * {@code smelly-demo} sibling module and exposes shared constants used by
 * downstream property tests under
 * {@code classtrim-core/src/test/java/org/classtrim/smellydemo/verify}.
 *
 * <p>Implemented purely against core ASM ({@code asm:9.7}) — the
 * {@code asm-tree} package is intentionally not relied on. A custom
 * {@link ClassVisitor} captures the subset of bytecode information the
 * downstream {@code 8.x} property tests need into immutable
 * {@link DemoClassModel}s.
 *
 * <p>This class is package-private intentionally; the harness is a test-only
 * shared library and is not part of the {@code classtrim-core} public API.
 */
final class SmellyDemoLoader {

    /** Simple names of the canonical Demo_Class inventory (size 8). */
    static final Set<String> EXPECTED_DEMO_CLASS_NAMES = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
            "Order",
            "OrderItem",
            "Customer",
            "Product",
            "Inventory",
            "ShippingCalculator",
            "Invoice",
            "OrderProcessor"
    )));

    /** Source-level package of every Demo_Class. */
    static final String DEMO_PACKAGE = "org.classtrim.demo.ecommerce";

    /** Bytecode internal-name prefix (slash-separated) for the Demo_Package. */
    static final String DEMO_PACKAGE_INTERNAL_PREFIX = "org/classtrim/demo/ecommerce";

    private SmellyDemoLoader() {
        // utility
    }

    /**
     * Resolve {@code smelly-demo/target/classes/org/classtrim/demo/ecommerce/}
     * relative to the {@code classtrim-core} module root (the working directory
     * Maven uses when running tests).
     */
    static Path resolveCompiledClassesDirectory() {
        return Paths.get("..", "smelly-demo", "target", "classes",
                "org", "classtrim", "demo", "ecommerce")
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Resolve {@code smelly-demo/src/main/java/org/classtrim/demo/ecommerce/}
     * relative to the {@code classtrim-core} module root.
     */
    static Path resolveSourceDirectory() {
        return Paths.get("..", "smelly-demo", "src", "main", "java",
                "org", "classtrim", "demo", "ecommerce")
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Load every {@code .class} file directly under
     * {@link #resolveCompiledClassesDirectory()} into a {@link DemoClassModel}.
     * The returned map is keyed by simple class name and is unmodifiable.
     * Iteration order is alphabetical by simple name to keep test output stable.
     */
    static Map<String, DemoClassModel> loadAllDemoClassModels() {
        Path dir = resolveCompiledClassesDirectory();
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(
                    "smelly-demo compiled classes directory not found: " + dir
                            + ". Run `mvn -pl smelly-demo -am compile` first.");
        }

        Map<String, DemoClassModel> byName = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> classFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .sorted()
                    .collect(Collectors.toList());
            for (Path classFile : classFiles) {
                DemoClassModel model = readClassModel(classFile);
                String simpleName = simpleNameFromInternalName(model.internalName);
                byName.put(simpleName, model);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to enumerate " + dir, e);
        }
        return Collections.unmodifiableMap(byName);
    }

    /**
     * Convenience view of {@link #loadAllDemoClassModels()} as an immutable list,
     * sorted by simple name for deterministic iteration.
     */
    static List<DemoClassModel> loadAllDemoClassModelsAsList() {
        return Collections.unmodifiableList(new ArrayList<>(loadAllDemoClassModels().values()));
    }

    /**
     * List the {@code .java} source files under {@link #resolveSourceDirectory()}.
     * Returns an immutable, alphabetically sorted list of absolute paths.
     */
    static List<Path> listDemoSourceFiles() {
        Path dir = resolveSourceDirectory();
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(
                    "smelly-demo source directory not found: " + dir);
        }
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .collect(Collectors.toList());
            return Collections.unmodifiableList(files);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to enumerate " + dir, e);
        }
    }

    /** Read a single {@code .class} file into a {@link DemoClassModel}. */
    static DemoClassModel readClassModel(Path classFile) {
        try (InputStream in = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(in);
            CapturingClassVisitor visitor = new CapturingClassVisitor();
            reader.accept(visitor, 0);
            return visitor.toModel();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read class file: " + classFile, e);
        }
    }

    // ---------------------------------------------------------------
    // Name conversions
    // ---------------------------------------------------------------

    /**
     * Convert a Demo_Class simple name (e.g. {@code "Order"}) to its bytecode
     * internal name (e.g. {@code "org/classtrim/demo/ecommerce/Order"}).
     */
    static String simpleNameToInternalName(String simpleName) {
        return DEMO_PACKAGE_INTERNAL_PREFIX + "/" + simpleName;
    }

    /**
     * Convert an internal name (slash-separated) to its simple name (text after
     * the last {@code /}, or the whole string if no slash is present).
     */
    static String simpleNameFromInternalName(String internalName) {
        int slash = internalName.lastIndexOf('/');
        return slash < 0 ? internalName : internalName.substring(slash + 1);
    }

    /**
     * @return {@code true} when {@code internalName} refers to one of the eight
     *         Demo_Classes in {@link #EXPECTED_DEMO_CLASS_NAMES}.
     */
    static boolean isDemoClassInternalName(String internalName) {
        if (internalName == null) {
            return false;
        }
        if (!internalName.startsWith(DEMO_PACKAGE_INTERNAL_PREFIX + "/")) {
            return false;
        }
        String simple = simpleNameFromInternalName(internalName);
        return EXPECTED_DEMO_CLASS_NAMES.contains(simple);
    }

    // ---------------------------------------------------------------
    // ASM visitor that captures the bits of bytecode the harness needs
    // ---------------------------------------------------------------

    private static final int ASM_API = Opcodes.ASM9;

    private static final class CapturingClassVisitor extends ClassVisitor {
        int classAccess;
        String classInternalName;
        String classSuperInternalName;
        List<String> classInterfaces = new ArrayList<>();
        boolean innerOrNested;
        final List<DemoMethodModel> methods = new ArrayList<>();

        CapturingClassVisitor() {
            super(ASM_API);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.classAccess = access;
            this.classInternalName = name;
            this.classSuperInternalName = superName;
            if (interfaces != null) {
                this.classInterfaces = new ArrayList<>(Arrays.asList(interfaces));
            }
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            // ASM emits an InnerClasses entry for *this* type if it is nested/inner.
            if (classInternalName != null && classInternalName.equals(name)) {
                innerOrNested = true;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            CapturingMethodVisitor mv = new CapturingMethodVisitor(access, name, descriptor);
            methods.add(null); // placeholder for ordering; replaced below
            int index = methods.size() - 1;
            mv.onFinished = (model) -> methods.set(index, model);
            return mv;
        }

        DemoClassModel toModel() {
            // Defensive: drop any unfinished placeholders (should not happen).
            List<DemoMethodModel> finished = new ArrayList<>(methods.size());
            for (DemoMethodModel m : methods) {
                if (m != null) {
                    finished.add(m);
                }
            }
            return new DemoClassModel(
                    classAccess,
                    classInternalName,
                    classSuperInternalName,
                    classInterfaces,
                    innerOrNested,
                    finished);
        }
    }

    private static final class CapturingMethodVisitor extends MethodVisitor {
        private final int access;
        private final String name;
        private final String descriptor;
        private final List<String> visibleAnnotations = new ArrayList<>();
        private final List<DemoInsn> insns = new ArrayList<>();
        java.util.function.Consumer<DemoMethodModel> onFinished;

        CapturingMethodVisitor(int access, String name, String descriptor) {
            super(ASM_API);
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (visible) {
                visibleAnnotations.add(descriptor);
            }
            return null;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            insns.add(DemoInsn.var(opcode, var));
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            insns.add(DemoInsn.field(opcode, owner, name, descriptor));
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name,
                                    String descriptor, boolean isInterface) {
            insns.add(DemoInsn.method(opcode, owner, name, descriptor, isInterface));
        }

        @Override
        public void visitLdcInsn(Object value) {
            insns.add(DemoInsn.ldc(value));
        }

        // The remaining insn callbacks just record the opcode so size-based
        // pure-getter / pure-setter checks see the right instruction count.

        @Override
        public void visitInsn(int opcode) {
            insns.add(DemoInsn.other(opcode));
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            insns.add(DemoInsn.other(opcode));
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            insns.add(DemoInsn.other(opcode));
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor,
                                           Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            insns.add(DemoInsn.other(Opcodes.INVOKEDYNAMIC));
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            insns.add(DemoInsn.other(opcode));
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            insns.add(DemoInsn.other(Opcodes.IINC));
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            insns.add(DemoInsn.other(Opcodes.TABLESWITCH));
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            insns.add(DemoInsn.other(Opcodes.LOOKUPSWITCH));
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            insns.add(DemoInsn.other(Opcodes.MULTIANEWARRAY));
        }

        @Override
        public void visitEnd() {
            DemoMethodModel model = new DemoMethodModel(
                    access, name, descriptor, visibleAnnotations, insns);
            if (onFinished != null) {
                onFinished.accept(model);
            }
        }
    }
}
