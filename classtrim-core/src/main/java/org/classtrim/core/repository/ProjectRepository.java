package org.classtrim.core.repository;

import org.classtrim.core.model.JavaProject;

import java.util.Optional;

public interface ProjectRepository {
    Optional<JavaProject> getFromCache(String projectName);

    void saveToCache(String projectName, JavaProject project);
}
