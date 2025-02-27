package org.refactor.common;

import org.refactor.util.FileUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatasetConst {
    private static final String MAVEN_REPO = "C:/Users/Churchill Du/.m2/repository/";
    public static final String ROOT = "D:/datasource/";
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

    private static final String[] dataSetPath = new String[]{
            "xom-1.2.1/nu/xom",
            "jhotdraw-6.0b1/org",
            "ganttproject-1.11.1/src",
            "beaver-0.9.11/main",
            "mango/main",
            "apachexmlrpc-3.1.1/xmlrpc-3.1.1",
            "trama-1.0/Trama/src",
            "apache-tomcat-9.0.1-src/java",
            "roller-roller_5.1.1/app/src/main"
    };
}
