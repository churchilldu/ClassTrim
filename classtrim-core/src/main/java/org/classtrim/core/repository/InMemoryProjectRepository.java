package org.classtrim.core.repository;

import org.classtrim.model.JavaProject;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryProjectRepository implements ProjectRepository {
    private final Map<String, JavaProject> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<JavaProject> getFromCache(String projectName) {
        return Optional.ofNullable(cache.get(projectName));
    }

    @Override
    public void saveToCache(String projectName, JavaProject project) {
        cache.put(projectName, project);
    }
}
