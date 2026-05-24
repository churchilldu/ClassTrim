package org.classtrim.core.repository;

import org.apache.commons.lang3.SerializationUtils;
import org.classtrim.core.config.Configuration;
import org.classtrim.core.model.JavaProject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class FilesystemProjectRepository implements ProjectRepository {
    private final Path cacheFolder;

    public FilesystemProjectRepository(Configuration configuration) {
        this.cacheFolder = Paths.get(configuration.getString("projectCacheFolder", ".project"));
    }

    @Override
    public Optional<JavaProject> getFromCache(String projectName) {
        Path cacheFile = cacheFolder.resolve(projectName);
        if (!Files.exists(cacheFile)) {
            return Optional.empty();
        }
        try (FileInputStream inputStream = new FileInputStream(cacheFile.toFile())) {
            return Optional.of(SerializationUtils.deserialize(inputStream));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to read cache for " + projectName, ex);
        }
    }

    @Override
    public void saveToCache(String projectName, JavaProject project) {
        Path cacheFile = cacheFolder.resolve(projectName);
        try {
            Files.createDirectories(cacheFolder);
            try (FileOutputStream outputStream = new FileOutputStream(cacheFile.toFile())) {
                SerializationUtils.serialize(project, outputStream);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to write cache for " + projectName, ex);
        }
    }
}
