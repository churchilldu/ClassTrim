package org.refactor;

import org.refactor.common.DatasetEnum;
import org.refactor.model.JavaProject;

public class Main {
    public static void main(String[] args) {
//        for (int i = 0; i < 1; i++) {
//            JMetalLogger.logger.info("------------- " + i + " -------------");
////            NSGAIII.main(new String[]{DatasetEnum.ANT_7.getName()});
//            NSGAIII.main(new String[]{DatasetEnum.CAMEL_4.getName()});
//            NSGAIII.main(new String[]{DatasetEnum.CAMEL_6.getName()});
//        }
//        NotifyUtils.notifyMyself();
        for (DatasetEnum dataset : DatasetEnum.values()) {
            JavaProject project = new JavaProject(dataset);
            project.parse();
            project.save();
        }
    }
}
