package org.refactor.common;

import org.refactor.Config;

import java.util.Arrays;

public enum DatasetEnum {

    ANT_5 ("ant-1.5",  Config.ROOT + "ant-1.5.0/ant-1.5/org/", new Threshold(8, 7, 40)),
    ANT_6 ("ant-1.6",  Config.ROOT + "ant-1.6.0/ant-1.6/org/", new Threshold(8, 7, 31)),
    ANT_7 ("ant-1.7",  Config.ROOT + "ant-1.7.0/org/", new Threshold(8, 7, 32)),
    JEDIT_0 ("jEdit-4.0",  Config.ROOT + "jEdit-4.0/jedit40install/jedit/", new Threshold(11, 8, 32)),
    JEDIT_1 ("jEdit-4.1",  Config.ROOT + "jEdit-4.1/jedit41install/installer/jedit-program.tar/jedit-program/jedit/", new Threshold(10, 10, 31)),
    JEDIT_2 ("jEdit-4.2",  Config.ROOT + "jEdit-4.2/jedit42install/installer/jedit-program/jedit/", new Threshold(10, 11, 37)),

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
