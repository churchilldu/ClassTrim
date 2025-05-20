package org.refactor.baseline;

import org.refactor.common.DatasetEnum;
import org.refactor.util.FileUtils;

import java.nio.file.Path;

public class Utils {
    public static void main(String[] args) {
        for (DatasetEnum value : DatasetEnum.values()) {
            FileUtils.createDir(Path.of("baseline", value.getName()));
        }
    }
}