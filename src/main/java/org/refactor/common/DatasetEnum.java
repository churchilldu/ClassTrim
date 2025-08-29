package org.refactor.common;

import lombok.Getter;
import org.refactor.util.AppProperties;

import java.util.Arrays;

@Getter
public enum DatasetEnum {
    ANT_5 ("ant-1_5",  "ant-1.5.0/ant-1.5/org/", new Threshold(8, 7, 40)),
    ANT_6 ("ant-1_6",  "ant-1.6.0/ant-1.6/org/", new Threshold(8, 7, 31)),
    ANT_7 ("ant-1_7",  "ant-1.7.0/org/", new Threshold(8, 7, 32)),

    JEDIT_0 ("jEdit-4_0",  "jEdit-4.0/jedit40install/jedit/", new Threshold(11, 8, 32)),
    JEDIT_1 ("jEdit-4_1",  "jEdit-4.1/jedit41install/installer/jedit-program.tar/jedit-program/jedit/", new Threshold(10, 10, 31)),
    JEDIT_2 ("jEdit-4.2",  "jEdit-4.2/jedit42install/installer/jedit-program/jedit/", new Threshold(10, 11, 37)),

    // Todo reflection
    CAMEL_4 ("camel-1_4",  "apache-camel-1.4/camel-bundle-1.4.0/", new Threshold(7, 7, 20)),
    CAMEL_6 ("camel-1_6",  "apache-camel-1.6/org/", new Threshold(2, 7, 17)),

    LUCENE_0 ("lucene-2_0",  "apache-lucene-2.0/org/", new Threshold(8, 5, 15)),
    LUCENE_2 ("lucene-2_2",  "apache-lucene-2.2/org/", new Threshold(6, 6, 17)),
    LUCENE_4 ("lucene-2_4",  "apache-lucene-2.4/org/", new Threshold(5, 5, 15)),

    SYNAPSE_0 ("synapse-1_0",  "apache-synapse-1.0/org/", new Threshold(6, 12, 35)),
    SYNAPSE_1 ("synapse-1_1",  "apache-synapse-1.1/org/", new Threshold(7, 11, 31)),
    SYNAPSE_2 ("synapse-1_2",  "apache-synapse-1.2/org/", new Threshold(5, 10, 29)),

    TEST ("test",  "test/pack/", new Threshold(0, 0, 0));

    private final String name;
    private final String path;
    private final Threshold threshold;

    DatasetEnum(String name, String path, Threshold threshold) {
        this.name = name;
        this.path = AppProperties.getString("datasetRoot") + path;
        this.threshold = threshold;
    }

    public static DatasetEnum of(String name) {
        return Arrays.stream(values()).filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

}
