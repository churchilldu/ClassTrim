package org.classtrim.core.engine;

import lombok.extern.slf4j.Slf4j;
import org.classtrim.core.config.DatasetEnum;
import org.classtrim.core.util.Stopwatch;

@Slf4j
public class Main {
    public static void main(String[] args) {
        for (int i = 0; i < 2; i++) {
        Stopwatch stopwatch = new Stopwatch();
        for (DatasetEnum dataset : DatasetEnum.values()) {
            MOEAD.main(new String[] { dataset.getName() });
            stopwatch.split(dataset.getName());
        }
        stopwatch.end();

        }
    }
}
