package org.classtrim.core.engine;

import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.model.JavaProject;

public interface RefactoringEngine {
    RefactoringResult run(JavaProject project, RefactoringConfig refactoringConfig);
}
