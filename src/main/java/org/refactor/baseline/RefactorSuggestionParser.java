package org.refactor.baseline;

import org.apache.commons.lang3.tuple.Pair;
import org.refactor.model.JavaClass;
import org.refactor.model.JavaMethod;
import org.refactor.model.JavaProject;

import java.nio.file.Path;
import java.util.List;

public interface RefactorSuggestionParser {
    /**
     * Parse the refactoring suggestions from the given file.
     * @param file The file containing the refactoring suggestions.
     * @param project The JavaProject used to resolve classes and methods.
     * @return A list of pairs of JavaMethod and JavaClass representing the refactoring suggestions.
     * the first element of the pair is the method to be refactored
     * the second element of the pair is the class to which the method will be refactored
     */
    List<Pair<JavaMethod, JavaClass>> parse(Path file, JavaProject project);
}


