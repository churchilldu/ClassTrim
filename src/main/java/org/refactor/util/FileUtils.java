package org.refactor.util;

import org.apache.commons.lang3.tuple.Triple;
import org.refactor.common.AlgorithmParameter;
import org.refactor.common.Metric;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class FileUtils {
    public static final List<String> IGNORED_DIRECTORIES = new ArrayList<>();
    public static final char TAB = '\t';
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

    private static String[] getAllFiles(String path, String ending) {
        try {
            return Files.walk(Paths.get(path))
                    .filter(Files::isRegularFile)
                    .filter(x -> !isIgnoredDir(x.toAbsolutePath().toString(), IGNORED_DIRECTORIES))
                    .filter(x -> x.toAbsolutePath().toString().toLowerCase().endsWith(ending))
                    .map(x -> x.toAbsolutePath().toString())
                    .toArray(String[]::new);
        } catch (Exception e) {
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

    /**
     * Write the metrics of class that can be refactored before and after refactoring to a given file.
     *
     * @param file   File name that is going to be written.
     * @param before JavaClass with its metrics before refactor.
     * @param after  JavaClass with its metrics after refactor.
     */
    @SuppressWarnings("ConcatenationWithEmptyString")
    public static void writeMetrics(Path file,
                                    Map<JavaClass, Triple<Integer, Integer, Integer>> before,
                                    Map<JavaClass, Triple<Integer, Integer, Integer>> after) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("" + TAB +
                    "WMC" + TAB + "" + TAB + "" + TAB +
                    "CBO" + TAB + "" + TAB + "" + TAB +
                    "RFC" + TAB + "" + TAB + "" + TAB);
            writer.newLine();
            writer.write("Class" + TAB +
                    "before" + TAB + "after" + TAB + "delta" + TAB +
                    "before" + TAB + "after" + TAB + "delta" + TAB +
                    "before" + TAB + "after" + TAB + "delta" + TAB);
            for (Map.Entry<JavaClass, Triple<Integer, Integer, Integer>> entry : after.entrySet()) {
                JavaClass clazz = entry.getKey();
                Triple<Integer, Integer, Integer> metrics = entry.getValue();
                writer.newLine();
                Integer metric1After = metrics.getLeft();
                Integer metric2After = metrics.getMiddle();
                Integer metric3After = metrics.getRight();
                Integer metric1Before = before.get(clazz).getLeft();
                Integer metric2Before = before.get(clazz).getMiddle();
                Integer metric3Before = before.get(clazz).getRight();
                writer.write(clazz.toString() + TAB +
                        metric1Before + TAB + metric1After + TAB + Math.subtractExact(metric1Before, metric1After) + TAB +
                        metric2Before + TAB + metric2After + TAB + Math.subtractExact(metric2Before, metric2After) + TAB +
                        metric3Before + TAB + metric3After + TAB + Math.subtractExact(metric3Before, metric3After) + TAB);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void writeDiff(Path file, List<Triple<JavaMethod, JavaClass, JavaClass>> diffs) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Method" + TAB + "From" + TAB + "To");
            for (Triple<JavaMethod, JavaClass, JavaClass> diff : diffs) {
                writer.newLine();
                String method = diff.getLeft().toString();
                String from = diff.getMiddle().toString();
                String to = diff.getRight().toString();
                writer.write(method + TAB + from + TAB + to);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    // Append population size, generation, algorithm name, wmc, cbo, rfc, folderName.
    @SuppressWarnings("rawtypes")
    public static void writeSummary(Path file, AlgorithmParameter parameter, Triple[] objectives, String folder) {
        if (!file.toFile().exists()) {
            writeSummaryTitle(file);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND)) {
            for (Triple objective : objectives) {
                writer.newLine();
                writer.write(parameter.toString() +
                        objective.getLeft() + TAB + objective.getMiddle() + TAB + objective.getRight() + TAB +
                        folder);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void writeSummaryTitle(Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            writer.write("Population size" + TAB + "Generation" + TAB + "Algorithm" + TAB
                    + "WMC" + TAB + "CBO" + TAB + "RFC" + TAB + "Folder");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getFolderId(String projectName) {
        Path path = Paths.get(AppProperties.getString("outputFolder"), projectName);
        createDir(path);
        return getFolderName(path);
    }

    public static void createFile(Path path) {
        try {
            Files.createFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get folder to be written into of directory.
     *
     * @param directory Look for the folder of the directory.
     * @return Folder's name to be written into.
     */
    @SuppressWarnings("DataFlowIssue")
    private static String getFolderName(Path directory) {
        return Arrays.stream(directory.toFile().listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .mapToInt(Integer::valueOf)
                .max()
                .stream().mapToObj(i -> String.format("%02d", i + 1))
                .findFirst()
                .orElse("01");
    }

   

    public static void appendToBaselineTsv(String fileName, String datasetName, Metric metric) {
        Path baselineFile = Paths.get("baseline", fileName + ".tsv");
        boolean isNewFile = !Files.exists(baselineFile);

        try (BufferedWriter writer = Files.newBufferedWriter(baselineFile, 
                StandardCharsets.UTF_8, 
                isNewFile ? StandardOpenOption.CREATE : StandardOpenOption.APPEND)) {
            
            // Write header if it's a new file
            if (isNewFile) {
                writer.write("Name" + TAB + "WMC" + TAB + "CBO" + TAB + "RFC");
                writer.newLine();
            }

            // Write dataset results
            writer.write(String.format("%s" + TAB + "%d" + TAB + "%d" + TAB + "%d", 
                datasetName, metric.getWmcOverThreshold(), metric.getCboOverThreshold(), metric.getRfcOverThreshold()));
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to baseline.tsv", e);
        }
    }

}
