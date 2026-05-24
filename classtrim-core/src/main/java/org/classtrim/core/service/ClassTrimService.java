package org.classtrim.core.service;

import org.classtrim.core.analyzer.ProjectAnalyzer;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.engine.RefactoringEngine;
import org.classtrim.core.engine.RefactoringResult;
import org.classtrim.core.model.ProjectSource;
import org.classtrim.core.model.JavaProject;

public class ClassTrimService {
    private final ProjectAnalyzer projectAnalyzer;
    private final RefactoringEngine refactoringEngine;

    public ClassTrimService(ProjectAnalyzer projectAnalyzer, RefactoringEngine refactoringEngine) {
        this.projectAnalyzer = projectAnalyzer;
        this.refactoringEngine = refactoringEngine;
    }

    public RefactoringResult analyze(ProjectSource projectSource, RefactoringConfig refactoringConfig) {
        JavaProject project = projectAnalyzer.analyze(projectSource);
        return refactoringEngine.run(project, refactoringConfig);
    }
}
