package org.refactor.util;

import org.apache.commons.lang3.tuple.Triple;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileUtils {
    public static final List<String> IGNORED_DIRECTORIES = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    //Initialize ignored directories with .git.
    static {
        //Use separator so this works on both Windows and Unix-like systems!
        IGNORED_DIRECTORIES.add(String.format("%c.git%c", File.separatorChar, File.separatorChar));
        IGNORED_DIRECTORIES.add("$");
    }

    public static String[] getAllJarFiles(String... paths) {
        List<String> files = new ArrayList<>();
        for (String path : paths) {
            files.addAll(Arrays.asList(getAllFiles(path, "jar")));
        }

        return files.toArray(new String[0]);
    }

    public static String[] getAllJavaFiles(String path) {
        return getAllFiles(path, "java");
    }

    public static String[] getAllClassFiles(String path) {
        return getAllFiles(path, "class");
    }

    private static String[] getAllFiles(String path, String ending){
        try {
            return Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .filter(x -> !isIgnoredDir(x.toAbsolutePath().toString(), IGNORED_DIRECTORIES))
                    .filter(x -> x.toAbsolutePath().toString().toLowerCase().endsWith(ending))
                    .map(x -> x.toAbsolutePath().toString())
                    .toArray(String[]::new);
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isIgnoredDir(String path, Collection<String> blocked) {
        for (String ignoredDirectory : blocked) {
            if (path.contains(ignoredDirectory)) {
                return true;
            }
        }
        return false;
    }

    public static void write(String file, Map<JavaClass, Integer> metricByClass) {
        Path path = Paths.get(file);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Map.Entry<JavaClass, Integer> entry : metricByClass.entrySet()) {
                String className = entry.getKey().toString();
                Integer metric = entry.getValue();
                writer.write(className + ", " + metric);
                writer.newLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void write(String file, List<Triple<JavaMethod, JavaClass, JavaClass>> diffs) {
        Path path = Paths.get(file);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (Triple<JavaMethod, JavaClass, JavaClass> diff : diffs) {
                String method = diff.getLeft().toString();
                String originalClass = diff.getMiddle().toString();
                String newClass = diff.getRight().toString();
                writer.write(method + " " + originalClass + " " + newClass);
                writer.newLine();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
