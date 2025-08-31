package org.refactor;

import lombok.extern.slf4j.Slf4j;
import org.refactor.common.DatasetEnum;
import org.refactor.model.JavaProject;

@Slf4j
public class Main {
    public static void main(String[] args) {
        for (DatasetEnum dataset : DatasetEnum.values()) {
            JavaProject.load(dataset);
        }
    }
}
