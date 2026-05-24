package org.classtrim.core.model;

import org.classtrim.core.metric.Threshold;

import java.util.Collection;

public interface ProjectSource {
    String getProjectName();

    Collection<String> getBinaryRoots();

    Threshold getThreshold();
}
