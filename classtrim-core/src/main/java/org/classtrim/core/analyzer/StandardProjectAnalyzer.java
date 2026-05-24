package org.classtrim.core.analyzer;

import org.classtrim.core.model.ProjectSource;
import org.classtrim.core.repository.ProjectRepository;
import org.classtrim.core.model.JavaProject;

public class StandardProjectAnalyzer implements ProjectAnalyzer {
    private final ProjectRepository projectRepository;

    public StandardProjectAnalyzer(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public JavaProject analyze(ProjectSource projectSource) {
        return projectRepository.getFromCache(projectSource.getProjectName())
                .orElseGet(() -> parseAndCache(projectSource));
    }

    private JavaProject parseAndCache(ProjectSource projectSource) {
        JavaProject project = JavaProject.parse(
                projectSource.getProjectName(),
                projectSource.getBinaryRoots(),
                projectSource.getThreshold()
        );
        projectRepository.saveToCache(projectSource.getProjectName(), project);
        return project;
    }
}
