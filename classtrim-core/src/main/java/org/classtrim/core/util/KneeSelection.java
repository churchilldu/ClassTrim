package org.classtrim.core.util;

import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.List;

/**
 * Post-processing selection of a single "best" non-dominated solution from a Pareto front,
 * following Indraneel Das, "On characterizing the 'knee' of the Pareto curve based on
 * Normal-Boundary Intersection", Structural Optimization 18 (1999), doi:10.1007/BF01195985.
 *
 * <p>The method works purely on the objective vectors, so it does not depend on NSGA-II/III
 * internals: it can be applied directly to the returned population (typically the non-dominated
 * front). Only the first {@code objectiveCount} objectives are considered; in this codebase those
 * are the real objectives (WMC, CBO, RFC), while any guiding objectives (indices >= objectiveCount)
 * are search heuristics and must be excluded from knee selection.</p>
 *
 * <p><b>How the knee is chosen (Das 1999).</b>
 * <ol>
 *   <li><b>Ideal (utopian) point.</b> Take the component-wise minimum of each objective over the
 *       whole front, {@code z*}. Translating the objectives by {@code z*} anchors the objective
 *       space at the best possible (hypothetically feasible) solution.</li>
 *   <li><b>Anchor points / pay-off rows.</b> For each objective {@code j} take the solution that
 *       minimizes objective {@code j} alone; call its (translated) objective vector {@code A[j]}.
 *       These are the "individual minimizer" points of the pay-off matrix.</li>
 *   <li><b>CHIM</b> (Convex Hull of Individual Minima): the affine hyperplane spanned by the anchor
 *       points {@code A[0..objectives-1]}.</li>
 *   <li><b>Knee = maximum bulge.</b> The knee is the Pareto point at the greatest (signed) distance
 *       from the CHIM on the side of the ideal point. For a front of convex "bow" shape this is the
 *       point farthest from the line/hyperplane joining the extremes -- the "knee of the curve".
 *       Encapsulate all of the Pareto-front bulge in one point without enumerating the whole front.
 *   </li>
 * </ol>
 * The construction (translate by ideal, then measure distance to the CHIM hyperplane) means the
 * selected knee is invariant to global affine rescaling of the objective functions, matching the
 * paper's scale-invariance result.
 */
public final class KneeSelection {

    private KneeSelection() {
    }

    /**
     * Selects the knee of the Pareto front among the given non-dominated solutions.
     *
     * @param nonDominated  the (non-dominated) population returned by the algorithm.
     * @param objectiveCount number of real objectives to use (3 for WMC/CBO/RFC). Guiding
     *                       objectives beyond this index are ignored.
     * @return the solution selected as the knee; never {@code null} if the list is non-empty.
     * @throws IllegalArgumentException if the list is empty or {@code objectiveCount < 2}.
     */
    public static IntegerSolution select(List<IntegerSolution> nonDominated, int objectiveCount) {
        if (nonDominated == null || nonDominated.isEmpty()) {
            throw new IllegalArgumentException("Cannot select knee from an empty population.");
        }
        if (objectiveCount < 2) {
            throw new IllegalArgumentException("Knee selection needs at least two objectives.");
        }

        int n = objectiveCount;
        int m = nonDominated.size();

        // 1. Ideal (utopian) point: component-wise minimum over the whole front.
        double[] ideal = new double[n];
        for (int j = 0; j < n; j++) {
            double min = Double.POSITIVE_INFINITY;
            for (IntegerSolution s : nonDominated) {
                min = Math.min(min, s.objectives()[j]);
            }
            ideal[j] = min;
        }

        // 2. Anchor (pay-off row) points: the solution minimizing each objective alone,
        //    translated to the ideal-origin space. All objectives are minimized.
        double[][] anchors = new double[n][n];
        for (int j = 0; j < n; j++) {
            IntegerSolution best = null;
            double bestVal = Double.POSITIVE_INFINITY;
            for (IntegerSolution s : nonDominated) {
                if (s.objectives()[j] < bestVal) {
                    bestVal = s.objectives()[j];
                    best = s;
                }
            }
            anchors[j] = translate(best.objectives(), ideal, n);
        }

        // 3. Orthonormal basis {b_1,..,b_{n-1}} of the CHIM hyperplane through the anchors,
        //    using anchor[0] as the origin. Gram-Schmidt; robust to near-degenerate anchors.
        double[][] basis = new double[n - 1][n];
        int valid = 0;
        for (int k = 1; k < n && valid < n - 1; k++) {
            double[] v = subtract(anchors[k], anchors[0], n);
            for (int b = 0; b < valid; b++) {
                double proj = dot(v, basis[b], n);
                for (int j = 0; j < n; j++) {
                    v[j] -= proj * basis[b][j];
                }
            }
            double len = norm(v);
            if (len > 1e-12) {
                for (int j = 0; j < n; j++) {
                    basis[valid][j] = v[j] / len;
                }
                valid++;
            }
        }
        int dim = valid; // effective dimensionality of the CHIM hyperplane

        // 4. Knee = the point with the greatest distance to the CHIM (maximum bulge).
        //    Search the existing front (we already have it from NSGA) instead of solving the NLP.
        double bestDist = -1.0;
        IntegerSolution knee = null;
        for (IntegerSolution s : nonDominated) {
            double[] p = translate(s.objectives(), ideal, n);
            double[] v = subtract(p, anchors[0], n);
            // Orthogonal residual to the CHIM hyperplane.
            double[] residual = v.clone();
            for (int b = 0; b < dim; b++) {
                double proj = dot(v, basis[b], n);
                for (int j = 0; j < n; j++) {
                    residual[j] -= proj * basis[b][j];
                }
            }
            double dist = norm(residual);
            if (dist > bestDist) {
                bestDist = dist;
                knee = s;
            }
        }

        // Degenerate fallback: if the anchors are (nearly) collinear/coincident the CHIM is a point
        // and every solution is the same distance away; then fall back to distance-to-ideal.
        if (dim < 1) {
            knee = closestToIdeal(nonDominated, ideal, n);
        }
        return knee;
    }

    private static IntegerSolution closestToIdeal(List<IntegerSolution> front, double[] ideal, int n) {
        IntegerSolution best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (IntegerSolution s : front) {
            double d = 0;
            for (int j = 0; j < n; j++) {
                double diff = s.objectives()[j] - ideal[j];
                d += diff * diff;
            }
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    private static double[] translate(double[] obj, double[] ideal, int n) {
        double[] t = new double[n];
        for (int j = 0; j < n; j++) {
            t[j] = obj[j] - ideal[j];
        }
        return t;
    }

    private static double[] subtract(double[] a, double[] b, int n) {
        double[] r = new double[n];
        for (int j = 0; j < n; j++) {
            r[j] = a[j] - b[j];
        }
        return r;
    }

    private static double dot(double[] a, double[] b, int n) {
        double sum = 0;
        for (int j = 0; j < n; j++) {
            sum += a[j] * b[j];
        }
        return sum;
    }

    private static double norm(double[] v) {
        return Math.sqrt(dot(v, v, v.length));
    }
}
