package org.classtrim.core.engine;

import lombok.Getter;
import org.classtrim.model.JavaProject;

import java.util.List;

@Getter
public class RefactoringResult {
    private final JavaProject project;
    private final List<RefactoringSuggestion> suggestions;
    private final long computingTimeMs;

    public RefactoringResult(JavaProject project,
                             List<RefactoringSuggestion> suggestions,
                             long computingTimeMs) {
        this.project = project;
        this.suggestions = List.copyOf(suggestions);
        this.computingTimeMs = computingTimeMs;
    }
}
