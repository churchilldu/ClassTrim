package org.classtrim.core.model;

import org.classtrim.common.Threshold;

import java.util.Collection;

public interface ProjectSource {
    String getProjectName();

    Collection<String> getBinaryRoots();

    Threshold getThreshold();
}
