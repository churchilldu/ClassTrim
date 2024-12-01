package org.refactor.common;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class DataSet {
    private static String BASE = "";

    private final String name;
    private final String path;
    private final Threshold threshold;
    private final URLClassLoader urlCL;

    public DataSet(String name, String root, String classFolder, Threshold threshold, String... jars) {
        this.name = name;
        this.path = BASE + root + classFolder;
        this.threshold = threshold;
        List<URL> urls = new ArrayList<>();
        try {
            for (String jar : jars) {
                urls.add(new URL("file:///" + BASE + root + jar));
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        urlCL = URLClassLoader.newInstance(urls.toArray(new URL[0]));
    }

    public static void setBase(String b) {
        BASE = b;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Threshold getThreshold() {
        return threshold;
    }

    public URLClassLoader getUrlCL() {
        return urlCL;
    }
}
