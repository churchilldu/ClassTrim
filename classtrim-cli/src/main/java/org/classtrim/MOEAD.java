package org.classtrim.core.engine;

import org.classtrim.core.config.AlgorithmParameter;
import org.classtrim.core.config.DatasetEnum;
import org.classtrim.core.util.RefactorOutput;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.algorithm.multiobjective.moead.MOEADBuilder;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.bounds.Bounds;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MOEAD extends AbstractAlgorithmRunner {
    public static void main(String[] args) throws JMetalException {
        String datasetName = args[0];
        DatasetEnum dataset = DatasetEnum.of(datasetName);
        Objects.requireNonNull(dataset, "Unsupported dataset.");

        RefactoringProblem integerProblem = new RefactoringProblem(dataset);
        DoubleRefactoringProblem problem = new DoubleRefactoringProblem(integerProblem);

        double crossoverProbability = 0.5;
        double crossoverDistributionIndex = 0.5;
        CrossoverOperator<DoubleSolution> crossover = new DifferentialEvolutionCrossover(
                crossoverProbability,
                crossoverDistributionIndex,
                DifferentialEvolutionCrossover.DE_VARIANT.RAND_1_BIN);
        JMetalLogger.logger.info("Crossover probability = " + crossoverProbability);
        JMetalLogger.logger.info("Differential evolution factor = " + crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        MutationOperator<DoubleSolution> mutation = new PolynomialMutation(mutationProbability,
                mutationDistributionIndex);
        JMetalLogger.logger.info("Mutation distribution index = " + mutationDistributionIndex);

        int populationSize = 500;
        int maxEvaluations = 2000;
        Path weightDirectory = prepareWeightDirectory(problem.numberOfObjectives(), populationSize);

        Algorithm<List<DoubleSolution>> algorithm =
                new MOEADBuilder(problem, MOEADBuilder.Variant.MOEAD)
                        .setPopulationSize(populationSize)
                        .setResultPopulationSize(populationSize)
                        .setMaxEvaluations(maxEvaluations)
                        .setDataDirectory(weightDirectory.toString())
                        .setCrossover(crossover)
                        .setMutation(mutation)
                        .build();

        JMetalLogger.logger.info("Population size = " + populationSize);
        JMetalLogger.logger.info("Max evaluations = " + maxEvaluations);

        AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

        List<DoubleSolution> population = algorithm.result();
        List<IntegerSolution> integerPopulation = toIntegerPopulation(integerProblem, population);
        long computingTime = algorithmRunner.getComputingTime();
        AlgorithmParameter moead = new AlgorithmParameter("MOEAD", populationSize, maxEvaluations);
        new RefactorOutput(integerProblem.getProject(), integerPopulation, moead)
                .write();
        JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
        JMetalLogger.logger.info("Objectives values have been written to file FUN.csv");
        JMetalLogger.logger.info("Variables values have been written to file VAR.csv");
    }

    private static Path prepareWeightDirectory(int objectives, int populationSize) {
        Path directory = Paths.get(System.getProperty("java.io.tmpdir"),
                "classtrim-moead-weights",
                objectives + "d-" + populationSize);
        Path weightFile = directory.resolve("W" + objectives + "D_" + populationSize + ".dat");

        try {
            Files.createDirectories(directory);
            if (Files.notExists(weightFile)) {
                writeWeightVectors(weightFile, objectives, populationSize);
            }
        } catch (IOException e) {
            throw new JMetalException("Unable to prepare MOEAD weight vectors at " + weightFile, e);
        }

        return directory;
    }

    private static void writeWeightVectors(Path weightFile, int objectives, int populationSize)
            throws IOException {
        Random random = new Random(42L);
        List<String> lines = new ArrayList<>(populationSize);
        for (int i = 0; i < populationSize; i++) {
            double[] weights = new double[objectives];
            double sum = 0.0;
            for (int j = 0; j < objectives; j++) {
                weights[j] = 0.1 + random.nextDouble();
                sum += weights[j];
            }

            StringBuilder line = new StringBuilder();
            for (int j = 0; j < objectives; j++) {
                if (j > 0) {
                    line.append(' ');
                }
                line.append(weights[j] / sum);
            }
            lines.add(line.toString());
        }

        Files.write(weightFile, lines, StandardCharsets.UTF_8);
    }

    private static List<IntegerSolution> toIntegerPopulation(RefactoringProblem problem,
                                                             List<DoubleSolution> population) {
        List<IntegerSolution> result = new ArrayList<>(population.size());
        for (DoubleSolution solution : population) {
            result.add(toIntegerSolution(problem, solution));
        }
        return result;
    }

    private static IntegerSolution toIntegerSolution(RefactoringProblem problem, DoubleSolution source) {
        IntegerSolution target = problem.createSolution();
        for (int i = 0; i < source.variables().size(); i++) {
            Bounds<Integer> bounds = target.getBounds(i);
            int value = (int) Math.round(source.variables().get(i));
            int clamped = Math.max(bounds.getLowerBound(), Math.min(bounds.getUpperBound(), value));
            target.variables().set(i, clamped);
        }
        for (int i = 0; i < source.objectives().length; i++) {
            target.objectives()[i] = source.objectives()[i];
        }
        return target;
    }

    private static final class DoubleRefactoringProblem extends AbstractDoubleProblem {
        private final RefactoringProblem integerProblem;

        private DoubleRefactoringProblem(RefactoringProblem integerProblem) {
            this.integerProblem = integerProblem;

            IntegerSolution prototype = integerProblem.createSolution();
            List<Double> lowerBounds = new ArrayList<>(prototype.variables().size());
            List<Double> upperBounds = new ArrayList<>(prototype.variables().size());
            for (int i = 0; i < prototype.variables().size(); i++) {
                lowerBounds.add(prototype.getBounds(i).getLowerBound().doubleValue());
                upperBounds.add(prototype.getBounds(i).getUpperBound().doubleValue());
            }

            variableBounds(lowerBounds, upperBounds);
            numberOfObjectives(integerProblem.numberOfObjectives());
            numberOfConstraints(integerProblem.numberOfConstraints());
            name("Method refactoring (MOEAD)");
        }

        @Override
        public DoubleSolution evaluate(DoubleSolution solution) {
            IntegerSolution integerSolution = integerProblem.createSolution();
            for (int i = 0; i < solution.variables().size(); i++) {
                Bounds<Integer> bounds = integerSolution.getBounds(i);
                int value = (int) Math.round(solution.variables().get(i));
                integerSolution.variables().set(i,
                        Math.max(bounds.getLowerBound(), Math.min(bounds.getUpperBound(), value)));
            }

            integerProblem.evaluate(integerSolution);
            for (int i = 0; i < solution.objectives().length; i++) {
                solution.objectives()[i] = integerSolution.objectives()[i];
            }

            return solution;
        }
    }
}
