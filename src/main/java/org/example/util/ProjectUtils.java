package org.example.util;

import org.apache.commons.collections4.CollectionUtils;
import org.example.model.JavaPackage;
import org.example.model.JavaProject;

public class ProjectUtils {
    public static JavaProject convertToProject() {

        return null;
    }

    public static String delat(JavaProject project1, JavaProject project2) {

        if (project1 == null) {
            return null;
        }

        StringBuilder diff = new StringBuilder(project1.getName());

        project1.getPackageList().forEach(pack -> {
            JavaPackage thatPack = project1.getPackage(pack);
            diff.append(pack.getName());
            diff.append("\n");

            diff.append(pack.getClassList().size());
            diff.append(" -> ");
            diff.append(thatPack.getClassList().size());

            diff.append(System.lineSeparator());
            diff.append("\n");
            diff.append("--------\n");

            CollectionUtils.retainAll(thatPack.getClassList(), pack.getClassList()).forEach(c -> {
                diff.append(c.getName());
                diff.append("\n");
            });
            CollectionUtils.removeAll(thatPack.getClassList(), pack.getClassList()).forEach(c -> {
                diff.append("+ ");
                diff.append(c.getName());
                diff.append("\n");
            });
            CollectionUtils.removeAll(pack.getClassList(), thatPack.getClassList()).forEach(c -> {
                diff.append("- ");
                diff.append(c.getName());
                diff.append("\n");
            });
            diff.append("\n");
        });

        return diff.toString();
    }

}
