package org.classtrim.core.engine;

import org.classtrim.RefactoringProblem;
import org.classtrim.core.config.RefactoringConfig;
import org.classtrim.model.JavaClass;
import org.classtrim.model.JavaMethod;
import org.classtrim.model.JavaProject;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.util.ArrayList;
import java.util.List;

public class NSGAIIIRefactoringEngine implements RefactoringEngine {
    @Override
    public RefactoringResult run(JavaProject project, RefactoringConfig refactoringConfig) {
        RefactoringProblem problem = new RefactoringProblem(project, refactoringConfig.getThreshold());

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability,
                crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 5.0;
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability,
                mutationDistributionIndex);

        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>(
                new RankingAndCrowdingDistanceComparator<>());

        Algorithm<List<IntegerSolution>> algorithm =
                new NSGAIIIBuilder<>(problem)
                        .setPopulationSize(refactoringConfig.getPopulationSize())
                        .setMaxIterations(refactoringConfig.getMaxIterations())
                        .setCrossoverOperator(crossover)
                        .setMutationOperator(mutation)
                        .setSelectionOperator(selection)
                        .build();

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();
        List<IntegerSolution> population = algorithm.result();
        return new RefactoringResult(project, convertToSuggestions(project, population),
                algorithmRunner.getComputingTime());
    }

    private List<RefactoringSuggestion> convertToSuggestions(JavaProject project,
                                                             List<IntegerSolution> population) {
        List<RefactoringSuggestion> suggestions = new ArrayList<>();
        List<JavaMethod> methodList = project.getMethodsCanRefactor();
        List<JavaClass> classList = project.getClassCanRefactor();
        if (population.isEmpty()) {
            return suggestions;
        }

        IntegerSolution bestSolution = population.get(0);
        for (int i = 0; i < bestSolution.variables().size(); i++) {
            JavaMethod method = methodList.get(i);
            JavaClass sourceClass = method.getClazz();
            JavaClass targetClass = classList.get(bestSolution.variables().get(i));
            if (!sourceClass.equals(targetClass)) {
                suggestions.add(new RefactoringSuggestion(method, sourceClass, targetClass));
            }
        }
        return suggestions;
    }
}
