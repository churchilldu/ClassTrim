package org.refactor.common;

import java.net.MalformedURLException;
import java.net.URL;

public interface DataSetConst {

    String[] dataSetPath = new String[]{
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

    class Ant {
        public static final String srcPath = "C:/codeRefactoring/datasource/apache-ant-1.7.0-src/apache-ant-1.7.0/src/main";
        public static final String output = "C:/Users/jesse/Downloads/ant-1.7.0/org";
        public static final Threshold THRESHOLD = new Threshold(8, 7);
        public static final URL[] URL;
        static {
            try {
                URL = new URL[]{
                        new URL("file:///" + "C:/Users/jesse/Downloads/ant-1.7.0.jar"),
                        new URL("file:///" + "C:/Users/jesse/Downloads/ant-launcher-1.7.0.jar")
                };
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
