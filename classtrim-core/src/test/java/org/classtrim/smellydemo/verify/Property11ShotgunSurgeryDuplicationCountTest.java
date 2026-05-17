package org.classtrim.smellydemo.verify;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Property 11: Shotgun Surgery duplication count.
 *
 * <p>For each cross-cutting concern {@code c} in
 * {@code {tax computation, currency conversion}}, asserts that the set of
 * Demo_Classes drawn from {@code c}'s candidate set whose source contains at
 * least one method body computing {@code c} inline has cardinality at least
 * 3.
 *
 * <p>"Computing the concern inline" is detected at the bytecode level: a
 * method body qualifies when it pushes the concern's numeric literal via an
 * {@code LDC} instruction. The tax-computation literal is {@code 0.0825d},
 * the currency-conversion literal (USD&rarr;EUR) is {@code 0.92d}. A
 * Demo_Class qualifies when any one of its methods contains the literal.
 *
 * <p>Detection delegates to
 * {@link BytecodeAccessCounter#containsLdcDouble(DemoMethodModel, double)},
 * which compares with {@link Double#compare} for deterministic NaN/-0
 * handling.
 *
 * <p><b>Validates: Requirements 7.1, 7.2</b>
 */
public class Property11ShotgunSurgeryDuplicationCountTest {

    /** Tax-rate literal inlined at every tax-computation site. */
    private static final double TAX_LITERAL = 0.0825d;

    /** USD&rarr;EUR conversion factor inlined at every currency-conversion site. */
    private static final double CURRENCY_LITERAL = 0.92d;

    /** Candidate Demo_Classes for tax computation (Requirement 7.1). */
    private static final Set<String> TAX_CANDIDATE_SET = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("Order", "OrderItem", "Invoice", "OrderProcessor")));

    /** Candidate Demo_Classes for currency conversion (Requirement 7.2). */
    private static final Set<String> CURRENCY_CANDIDATE_SET = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList("Order", "OrderItem", "Invoice", "Product", "OrderProcessor")));

    /** Minimum number of duplicating Demo_Classes per concern (Requirements 7.1, 7.2). */
    private static final int MIN_DUPLICATING_CLASSES = 3;

    /**
     * Tax-computation concern: the set of Demo_Classes from
     * {@code {Order, OrderItem, Invoice, OrderProcessor}} whose source
     * contains at least one method body that pushes the {@code 0.0825d}
     * literal must have cardinality at least 3.
     *
     * <p><b>Validates: Requirement 7.1</b>
     */
    @Test
    public void taxComputationDuplicatedAcrossAtLeastThreeCandidateClasses() {
        assertConcernIsDuplicatedAcrossAtLeast(
                "tax computation",
                TAX_LITERAL,
                TAX_CANDIDATE_SET);
    }

    /**
     * Currency-conversion concern: the set of Demo_Classes from
     * {@code {Order, OrderItem, Invoice, Product, OrderProcessor}} whose
     * source contains at least one method body that pushes the {@code 0.92d}
     * literal must have cardinality at least 3.
     *
     * <p><b>Validates: Requirement 7.2</b>
     */
    @Test
    public void currencyConversionDuplicatedAcrossAtLeastThreeCandidateClasses() {
        assertConcernIsDuplicatedAcrossAtLeast(
                "currency conversion",
                CURRENCY_LITERAL,
                CURRENCY_CANDIDATE_SET);
    }

    /**
     * Run the duplication-count check for one concern. Iterates every method
     * on each candidate Demo_Class; a class qualifies iff any one of its
     * methods contains an {@code LDC} push of {@code literal}. Asserts the
     * resulting set has cardinality at least {@link #MIN_DUPLICATING_CLASSES},
     * with a message that lists which candidates qualified and which did not.
     */
    private static void assertConcernIsDuplicatedAcrossAtLeast(String concernLabel,
                                                                double literal,
                                                                Set<String> candidateSet) {
        Map<String, DemoClassModel> byName = SmellyDemoLoader.loadAllDemoClassModels();

        Set<String> qualifying = new TreeSet<>();
        Set<String> nonQualifying = new TreeSet<>();

        for (String candidate : candidateSet) {
            DemoClassModel model = byName.get(candidate);
            assertNotNull(
                    "Candidate Demo_Class `" + candidate + "` for concern `" + concernLabel
                            + "` must be present in the compiled inventory. "
                            + "Run `mvn -pl smelly-demo -am compile` first.",
                    model);

            if (anyMethodContainsLdcDouble(model.methods, literal)) {
                qualifying.add(candidate);
            } else {
                nonQualifying.add(candidate);
            }
        }

        assertTrue(
                "Shotgun_Surgery duplication count for concern `" + concernLabel
                        + "` (literal " + literal + ") must be at least "
                        + MIN_DUPLICATING_CLASSES
                        + " but was " + qualifying.size() + "."
                        + " Qualifying classes (LDC " + literal + " present): " + qualifying + "."
                        + " Non-qualifying classes (LDC " + literal + " absent): " + nonQualifying + ".",
                qualifying.size() >= MIN_DUPLICATING_CLASSES);
    }

    /** True iff at least one method in {@code methods} pushes {@code literal} via {@code LDC}. */
    private static boolean anyMethodContainsLdcDouble(List<DemoMethodModel> methods, double literal) {
        for (DemoMethodModel m : methods) {
            if (BytecodeAccessCounter.containsLdcDouble(m, literal)) {
                return true;
            }
        }
        return false;
    }
}
