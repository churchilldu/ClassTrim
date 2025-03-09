package org.refactor;

import org.refactor.common.DatasetEnum;
import org.refactor.util.NotifyUtils;
import org.refactor.util.RefactorOutput;
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
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NSGAIII extends AbstractAlgorithmRunner {
    public static void main(String[] args) throws JMetalException {
        String datasetName = args[0];
        DatasetEnum dataset = DatasetEnum.of(datasetName);
        RefactoringProblem problem = new RefactoringProblem(dataset);

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        CrossoverOperator<IntegerSolution> crossover = new IntegerSBXCrossover(crossoverProbability,
                crossoverDistributionIndex);
        JMetalLogger.logger.info("Crossover distribution index = " + crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 5.0;
        MutationOperator<IntegerSolution> mutation = new IntegerPolynomialMutation(mutationProbability,
                mutationDistributionIndex);
        JMetalLogger.logger.info("Mutation distribution index = " + mutationDistributionIndex);

        SelectionOperator<List<IntegerSolution>, IntegerSolution> selection = new BinaryTournamentSelection<>(
                new RankingAndCrowdingDistanceComparator<>());

        int populationSize = 150;
        int maxIterations = 300;

        Algorithm<List<IntegerSolution>> algorithm =
                new NSGAIIIBuilder<>(problem)
                        .setPopulationSize(populationSize)
                        .setMaxIterations(maxIterations)
                        .setCrossoverOperator(crossover)
                        .setMutationOperator(mutation)
                        .setSelectionOperator(selection)
                        .build();

        JMetalLogger.logger.info("Population size = " + populationSize);
        JMetalLogger.logger.info("Max iterations = " + maxIterations);

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        List<IntegerSolution> population = algorithm.result();
        long computingTime = algorithmRunner.getComputingTime();
        Map<String, Object> configs = new HashMap<>();
        configs.put("Algorithm", "NSGA3");
        configs.put("Date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")));
        configs.put("Population size", populationSize);
        configs.put("Iteration", maxIterations);
        configs.put("ComputingTime", computingTime);
        new RefactorOutput(problem.getProject(), population, configs)
                .write();
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
        JMetalLogger.logger.info("Objectives values have been written to file FUN.csv");
        JMetalLogger.logger.info("Variables values have been written to file VAR.csv");

        NotifyUtils.notifyMyself();
    }

}
