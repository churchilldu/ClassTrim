package org.classtrim.core.util;

import org.junit.Test;
import org.uma.jmetal.problem.integerproblem.impl.AbstractIntegerProblem;
import org.uma.jmetal.solution.integersolution.IntegerSolution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link KneeSelection} (Das 1999, NBI-based "maximum bulge").
 */
public class KneeSelectionTest {

    /** Minimal 3-objective integer problem, just to allocate IntegerSolution objects. */
    private static final class StubProblem extends AbstractIntegerProblem {
        private final int vars;

        StubProblem(int vars) {
            this.vars = vars;
            List<Integer> lower = new ArrayList<>(vars);
            List<Integer> upper = new ArrayList<>(vars);
            IntStream.range(0, vars).forEach(i -> {
                lower.add(0);
                upper.add(1);
            });
            variableBounds(lower, upper);
        }

        @Override
        public int numberOfObjectives() {
            return 3;
        }

        @Override
        public int numberOfConstraints() {
            return 0;
        }

        @Override
        public String name() {
            return "stub";
        }

        @Override
        public IntegerSolution evaluate(IntegerSolution s) {
            return s;
        }
    }

    private static IntegerSolution solution(StubProblem p, int o0, int o1, int o2) {
        IntegerSolution s = p.createSolution();
        s.objectives()[0] = o0;
        s.objectives()[1] = o1;
        s.objectives()[2] = o2;
        return s;
    }

    /**
     * 2-objective-like convex front in the plane {o0, o1} (o2 constant):
     *   (1,5)  (3,3)  (5,1)   -- o2 = 5 everywhere
     * The chord between the anchors (1,5)-(5,1) has equation o0+o1=6; the bulge point (3,3) sums
     * to 6, so bump it to (2,3) (sums to 5 < 6) so it protrudes toward the ideal origin and is
     * the maximum-bulge knee.
     */
    @Test
    public void selectsCentralPointNotAnchor() {
        StubProblem p = new StubProblem(2);
        List<IntegerSolution> front = new ArrayList<>();
        IntegerSolution anchorLO = solution(p, 1, 5, 5);
        IntegerSolution bulge = solution(p, 2, 3, 5);   // below chord -> knee
        IntegerSolution anchorHI = solution(p, 5, 1, 5);
        front.addAll(Arrays.asList(anchorLO, bulge, anchorHI));

        IntegerSolution knee = KneeSelection.select(front, 3);
        assertNotNull(knee);
        System.out.println("knee objectives = " + Arrays.toString(knee.objectives()));
        // The knee must be the bulged central point, not an anchor.
        assert knee == bulge : "knee should be the central bulge point but was " + Arrays.toString(knee.objectives());
    }

    /** When all objectives share a single optimum, fallback picks the closest-to-ideal point. */
    @Test
    public void degenerateFrontFallsBackToClosestToIdeal() {
        StubProblem p = new StubProblem(2);
        IntegerSolution s1 = solution(p, 5, 5, 5);
        IntegerSolution s2 = solution(p, 6, 6, 6);
        IntegerSolution s3 = solution(p, 7, 7, 7);
        List<IntegerSolution> front = Arrays.asList(s1, s2, s3);

        IntegerSolution knee = KneeSelection.select(front, 3);
        assertNotNull(knee);
        assert knee == s1 : "expected s1 (closest to ideal), got " + Arrays.toString(knee.objectives());
    }

    /** A 3D bulge point must be selected as the knee over the anchors. */
    @Test
    public void selectsThreeDimensionalBulge() {
        StubProblem p = new StubProblem(2);
        // anchors: (o0=1), (o1=1) but at cost of others
        IntegerSolution a0 = solution(p, 1, 5, 5);
        IntegerSolution a1 = solution(p, 5, 1, 5);
        IntegerSolution a2 = solution(p, 5, 5, 1);
        // bulge: balanced, close to ideal (1,1,1)-ish region
        IntegerSolution bulge = solution(p, 2, 2, 2);
        List<IntegerSolution> front = Arrays.asList(a0, a1, a2, bulge);

        IntegerSolution knee = KneeSelection.select(front, 3);
        assertNotNull(knee);
        assert knee == bulge : "expected bulge (2,2,2), got " + Arrays.toString(knee.objectives());
    }

    /** Two objectives are enough (2D chord knee) on a genuinely convex (bulging) front. */
    @Test
    public void twoObjectives() {
        StubProblem p = new StubProblem(1);
        List<IntegerSolution> front = new ArrayList<>();
        // Chord between extremes (0,8)-(8,0) has equation o0+o1=8. The bulge point (3,3) sums
        // to 6 < 8, so it protrudes toward the ideal origin and is the maximum-bulge knee.
        IntegerSolution lo = solution(p, 0, 8, 9);
        IntegerSolution mid = solution(p, 3, 3, 9);   // bulge
        IntegerSolution hi = solution(p, 8, 0, 9);
        front.add(lo);
        front.add(mid);
        front.add(hi);

        IntegerSolution knee = KneeSelection.select(front, 2);
        assertNotNull(knee);
        assert knee == mid : "expected bulged central point as 2D knee, got " + Arrays.toString(knee.objectives());
    }
}
