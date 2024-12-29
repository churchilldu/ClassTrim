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

    public DataSet(String name, String root, String classFolder, Threshold threshold) {
        this.name = name;
        this.path = BASE + root + classFolder;
        this.threshold = threshold;
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
}
