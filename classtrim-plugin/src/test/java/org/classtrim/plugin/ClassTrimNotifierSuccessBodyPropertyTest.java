package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.junit.jupiter.api.Assertions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Property-based test for {@link ClassTrimNotifier#formatSuccessBody(int)}.
 *
 * <p>Validates: Requirements 5.2, 5.3 — the "Success notification body reports
 * the suggestion count" property from the {@code idea-plugin} design
 * (Correctness Property 6).</p>
 *
 * <p>The system under test is a pure formatter:</p>
 * <ul>
 *   <li>{@code K == 0} → {@code "No move-method suggestions were produced (count: 0)"}</li>
 *   <li>{@code K  > 0} → {@code "Generated " + K + " move-method suggestions"}</li>
 * </ul>
 *
 * <p>The property asserts that, for any {@code K >= 0}, the first decimal
 * integer literal in the body parses back to {@code K}; that the formatter
 * never throws; and that — when {@code K == 0} — the body additionally
 * carries a phrase signalling that no suggestions were produced (R5.3).</p>
 *
 * <p>The generator is biased to include the boundary cases called out by the
 * task description: {@code 0}, {@code 1}, the small-integer band
 * {@code 1..100}, and large integers up to {@link Integer#MAX_VALUE}.</p>
 */
class ClassTrimNotifierSuccessBodyPropertyTest {

    /**
     * Matches the first run of one or more decimal digits in the body. The
     * formatter only ever embeds a single integer literal (the count itself),
     * so the first {@code \d+} match is unambiguously the count.
     */
    private static final Pattern FIRST_DIGITS = Pattern.compile("\\d+");

    /**
     * <strong>Feature: idea-plugin, Property 6: Success notification body
     * reports the suggestion count.</strong>
     *
     * <p>For any {@code K >= 0}:</p>
     * <ol>
     *   <li>{@code formatSuccessBody(K)} never throws;</li>
     *   <li>parsing the first {@code \d+} substring of the body returns
     *       {@code K} (R5.2);</li>
     *   <li>when {@code K == 0}, the body also contains a phrase indicating
     *       that no suggestions were produced (R5.3) — specifically, a
     *       case-insensitive match of {@code "No move-method suggestions"} or
     *       {@code "no suggestions"}.</li>
     * </ol>
     *
     * <p>Validates: Requirements 5.2, 5.3.</p>
     */
    @Property(tries = 100)
    // JUnit 5's @Tag does not accept colons or commas; the human-readable
    // label is published via @Label below. The sanitized identifier here is
    // used for tooling-level filtering.
    @Tag("idea-plugin-property-6")
    @Label("Feature: idea-plugin, Property 6: Success notification body reports the suggestion count")
    void successBodyReportsSuggestionCount(@ForAll("nonNegativeCounts") int k) {
        // (1) Totality: the formatter must never throw.
        String body = Assertions.assertDoesNotThrow(
                () -> ClassTrimNotifier.formatSuccessBody(k),
                "formatSuccessBody must never throw");
        Assertions.assertNotNull(body, "formatSuccessBody must never return null");

        // (2) The first decimal integer literal in the body parses back to K.
        Matcher matcher = FIRST_DIGITS.matcher(body);
        Assertions.assertTrue(
                matcher.find(),
                () -> "Body did not contain any decimal integer literal: '" + body + "'");
        String firstNumber = matcher.group();
        int parsed;
        try {
            parsed = Integer.parseInt(firstNumber);
        } catch (NumberFormatException nfe) {
            Assertions.fail("First digit run '" + firstNumber + "' did not parse as int: " + nfe);
            return; // unreachable; keeps the compiler happy about `parsed`'s definite assignment.
        }
        Assertions.assertEquals(
                k, parsed,
                () -> "First integer literal in body did not equal K. body='" + body + "', K=" + k);

        // (3) When K == 0, the body must also carry a phrase indicating that
        //     no suggestions were produced (R5.3).
        if (k == 0) {
            String lower = body.toLowerCase();
            boolean hasNoSuggestionsPhrase =
                    lower.contains("no move-method suggestions")
                            || lower.contains("no suggestions");
            Assertions.assertTrue(
                    hasNoSuggestionsPhrase,
                    () -> "Body for K=0 did not contain a 'no suggestions' phrase: '" + body + "'");
        }
    }

    // --- Generators ----------------------------------------------------------

    /**
     * Counts {@code K >= 0}, weighted with the edge cases called out by the
     * task description:
     * <ul>
     *   <li>{@code 0} (the zero-suggestion branch);</li>
     *   <li>{@code 1} (the smallest "non-zero" branch);</li>
     *   <li>small integers in {@code [1, 100]};</li>
     *   <li>large integers up to {@link Integer#MAX_VALUE}.</li>
     * </ul>
     */
    @Provide
    Arbitrary<Integer> nonNegativeCounts() {
        Arbitrary<Integer> zero = Arbitraries.just(0);
        Arbitrary<Integer> one = Arbitraries.just(1);
        Arbitrary<Integer> small = Arbitraries.integers().between(1, 100);
        Arbitrary<Integer> medium = Arbitraries.integers().between(101, 1_000_000);
        Arbitrary<Integer> large = Arbitraries.integers().between(1_000_000, Integer.MAX_VALUE);
        Arbitrary<Integer> maxValue = Arbitraries.just(Integer.MAX_VALUE);

        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, zero),
                net.jqwik.api.Tuple.of(2, one),
                net.jqwik.api.Tuple.of(4, small),
                net.jqwik.api.Tuple.of(2, medium),
                net.jqwik.api.Tuple.of(2, large),
                net.jqwik.api.Tuple.of(1, maxValue)
        );
    }
}
