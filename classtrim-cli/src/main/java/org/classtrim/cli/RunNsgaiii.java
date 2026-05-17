package org.classtrim.cli;

import org.classtrim.common.Threshold;
import org.classtrim.core.analyzer.ProjectAnalyzer;
import org.classtrim.core.analyzer.StandardProjectAnalyzer;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.core.engine.NSGAIIIRefactoringEngine;
import org.classtrim.core.engine.RefactoringEngine;
import org.classtrim.core.engine.RefactoringResult;
import org.classtrim.core.engine.RefactoringSuggestion;
import org.classtrim.core.model.BinaryPathProjectSource;
import org.classtrim.core.model.ProjectSource;
import org.classtrim.core.repository.InMemoryProjectRepository;
import org.classtrim.core.repository.ProjectRepository;
import org.classtrim.core.service.ClassTrimService;

import java.util.Collections;

public class RunNsgaiii {
    public static void main(String[] args) {
        if (args.length < 6) {
            throw new IllegalArgumentException(
                    "Usage: <projectName> <classRoot> <wmc> <cbo> <rfc> <population> [iterations]"
            );
        }

        String projectName = args[0];
        String classRoot = args[1];
        int wmc = Integer.parseInt(args[2]);
        int cbo = Integer.parseInt(args[3]);
        int rfc = Integer.parseInt(args[4]);
        int population = Integer.parseInt(args[5]);
        int iterations = args.length > 6 ? Integer.parseInt(args[6]) : 2000;

        ProjectRepository repository = new InMemoryProjectRepository();
        ProjectAnalyzer analyzer = new StandardProjectAnalyzer(repository);
        RefactoringEngine engine = new NSGAIIIRefactoringEngine();
        ClassTrimService service = new ClassTrimService(analyzer, engine);
        ProjectSource source = new BinaryPathProjectSource(
                projectName,
                Collections.singletonList(classRoot),
                new Threshold(wmc, cbo, rfc)
        );

        RefactoringResult result = service.analyze(source, new RefactoringConfig(
                source.getThreshold(), population, iterations
        ));

        System.out.println("Computed in " + result.getComputingTimeMs() + "ms");
        for (RefactoringSuggestion suggestion : result.getSuggestions()) {
            System.out.printf("Move %s from %s to %s%n",
                    suggestion.getMethod(),
                    suggestion.getSourceClass(),
                    suggestion.getTargetClass());
        }
    }
}
