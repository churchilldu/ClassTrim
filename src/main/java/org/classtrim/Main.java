package org.classtrim;

import lombok.extern.slf4j.Slf4j;
import org.classtrim.common.DatasetEnum;
import org.classtrim.util.Stopwatch;

@Slf4j
public class Main {
    public static void main(String[] args) {
        Stopwatch stopwatch = new Stopwatch();
        for (DatasetEnum dataset : DatasetEnum.values()) {
            NSGAIII.main(new String[] { dataset.getName() });
            stopwatch.split(dataset.getName());
        }
        stopwatch.end();
    }
}
