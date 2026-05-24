package org.classtrim.core.model;

import org.classtrim.core.metric.Threshold;

import java.util.Collection;
import java.util.List;

public class BinaryPathProjectSource implements ProjectSource {
    private final String projectName;
    private final List<String> binaryRoots;
    private final Threshold threshold;

    public BinaryPathProjectSource(String projectName, Collection<String> binaryRoots, Threshold threshold) {
        this.projectName = projectName;
        this.binaryRoots = List.copyOf(binaryRoots);
        this.threshold = threshold;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    @Override
    public Collection<String> getBinaryRoots() {
        return binaryRoots;
    }

    @Override
    public Threshold getThreshold() {
        return threshold;
    }
}
