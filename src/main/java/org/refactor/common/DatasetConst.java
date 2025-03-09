package org.refactor.common;

import org.refactor.util.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class DatasetConst {
    private static final String MAVEN_REPO = "C:/Users/jesse/.m2/repository/";
    public static final String ROOT = "C:/codeRefactoring/datasource/";
    public static URLClassLoader urlCL;

    static {
        try {
            List<URL> urlList = new ArrayList<>();
            for (String jar : FileUtils.getAllJarFiles(MAVEN_REPO, ROOT)) {
                urlList.add(new File(jar).toURI().toURL());
            }

            urlCL = URLClassLoader.newInstance(urlList.toArray(new URL[0]));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

}
