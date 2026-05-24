package org.classtrim.core.analyzer;

import org.classtrim.core.model.ProjectSource;
import org.classtrim.core.model.JavaProject;

public interface ProjectAnalyzer {
    JavaProject analyze(ProjectSource projectSource);
}
