package org.refactor.common;

public class DataSetConst {
    static {
        DataSet.setBase("C:/codeRefactoring/datasource/");
    }

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


    public static final DataSet ANT = new DataSet(
            "org/apache/tools/",
            "ant-1.7.0/",
            "org",
            new Threshold(8, 7),
            "ant-1.7.0.jar",
            "ant-launcher-1.7.0.jar"
    );
}
