package org.classtrim;

import org.classtrim.common.AlgorithmParameter;
import org.classtrim.common.DatasetEnum;
import org.classtrim.util.RefactorOutput;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.NSGAIIBuilder;
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

import java.util.List;

public class NSGAII extends AbstractAlgorithmRunner {
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

        int populationSize = 500;
        int maxEvaluations = 75000;

        Algorithm<List<IntegerSolution>> algorithm =
                new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
                        .setMaxEvaluations(maxEvaluations)
                        .build();

        JMetalLogger.logger.info("Population size = " + populationSize);

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        List<IntegerSolution> population = algorithm.result();
        long computingTime = algorithmRunner.getComputingTime();

        // generation = max evaluation / (number of objectives * population)
        AlgorithmParameter nsga2 = new AlgorithmParameter("NSGA2", populationSize, maxEvaluations / (3 * populationSize));
        new RefactorOutput(problem.getProject(), population, nsga2)
                .write();
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
        JMetalLogger.logger.info("Objectives values have been written to file FUN.csv");
        JMetalLogger.logger.info("Variables values have been written to file VAR.csv");
    }


}
