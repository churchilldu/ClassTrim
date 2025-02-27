package org.refactor.common;

import java.util.Arrays;

public enum DatasetEnum {

    ANT_5 ("ant-1.5",  DatasetConst.ROOT + "ant-1.5.0/ant-1.5/org/", new Threshold(8, 7, 8)),
    ANT_6 ("ant-1.6",  DatasetConst.ROOT + "ant-1.6.0/ant-1.6/org/", new Threshold(8, 7, 7)),
    ANT_7 ("ant-1.7",  DatasetConst.ROOT + "ant-1.7.0/org/", new Threshold(8, 7, 7)),
    JEDIT_0 ("jEdit-4.0",  DatasetConst.ROOT + "jEdit-4.0/jedit40install/jedit/org/", new Threshold(11, 8, 8)),
    JEDIT_1 ("jEdit-4.1",  DatasetConst.ROOT + "jEdit-4.0/jedit40install/jedit/org/", new Threshold(10, 10, 10)),
    JEDIT_2 ("jEdit-4.2",  DatasetConst.ROOT + "jEdit-4.0/jedit40install/jedit/org/", new Threshold(10, 11, 11)),
    SYNAPSE_1 ("synapse-1.1",  DatasetConst.ROOT + "apache-synapse-1.1/synapse-core-1.1/org/", new Threshold(7, 11, 11)),

    TEST ("test",  "target/test-classes/pack", new Threshold(0, 0, 0));

    private final String name;
    private final String path;
    private final Threshold threshold;
    DatasetEnum(String name, String path, Threshold threshold) {
        this.name = name;
        this.path = path;
        this.threshold = threshold;
    }

    public static DatasetEnum of(String name) {
        return Arrays.stream(values()).filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public Threshold getThreshold() {
        return this.threshold;
    }

}
