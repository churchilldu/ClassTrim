package org.refactor.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileUtils {
    public static final List<String> IGNORED_DIRECTORIES = new ArrayList<>();

    //Initialize ignored directories with .git.
    static {
        //Use separator so this works on both Windows and Unix-like systems!
        IGNORED_DIRECTORIES.add(String.format("%c.git%c", File.separatorChar, File.separatorChar));
        IGNORED_DIRECTORIES.add("$");
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
                    .filter(x -> !x.toString().contains("$"))
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
}
