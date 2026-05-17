package org.classtrim.plugin;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.junit.jupiter.api.Assertions;

/**
 * Property-based test for {@link ClassTrimNotifier#formatFailureBody(String, String)}.
 *
 * <p>Validates: Requirements 6.3 — the "Failure notification body is well-formed
 * and bounded" property from the {@code idea-plugin} design (Correctness
 * Property 7).</p>
 *
 * <p>The system under test composes the body as
 * {@code className + ": " + truncate(messageOrNull, 500)} where the truncation
 * helper returns {@code ""} for {@code null}, the input itself when it fits in
 * 500 chars, and {@code substring(0, 500)} otherwise. The property covers all
 * three message arms together with several adversarial input shapes (empty,
 * very long, embedded newlines, non-ASCII).</p>
 *
 * <p>The "message portion" is what comes after the literal {@code ": "}
 * separator: {@code body.substring(className.length() + 2)}. Since
 * {@link ClassTrimNotifier#FAILURE_MESSAGE_MAX_LENGTH} is the documented bound
 * for that portion, the property reads it through the public constant rather
 * than hard-coding {@code 500}, so the test stays in lock-step with the
 * formatter if the constant is ever retuned.</p>
 */
class ClassTrimNotifierFailureBodyPropertyTest {

    /** The exact separator inserted between className and the truncated message. */
    private static final String SEPARATOR = ": ";

    /**
     * <strong>Feature: idea-plugin, Property 7: Failure notification body is
     * well-formed and bounded.</strong>
     *
     * <p>For any non-blank {@code className} and any {@code message} (including
     * {@code null}, empty, very long, embedded newlines, or non-ASCII):</p>
     * <ol>
     *   <li>{@code formatFailureBody} never throws;</li>
     *   <li>the returned body starts with {@code className};</li>
     *   <li>the returned body starts with {@code className + ": "} (separator
     *       follows immediately);</li>
     *   <li>the message portion (everything after {@code className + ": "}) is
     *       at most {@link ClassTrimNotifier#FAILURE_MESSAGE_MAX_LENGTH} chars
     *       long;</li>
     *   <li>when {@code message == null} the message portion is the empty
     *       string;</li>
     *   <li>when {@code message != null && message.length() <= 500} the message
     *       portion equals {@code message};</li>
     *   <li>when {@code message != null && message.length() > 500} the message
     *       portion equals {@code message.substring(0, 500)}.</li>
     * </ol>
     *
     * <p>Validates: Requirements 6.3.</p>
     */
    @Property(tries = 100)
    // JUnit 5's @Tag does not accept colons or commas, so a sanitized identifier
    // is used here for tooling-level filtering. The full human-readable label is
    // published via @Label below.
    @Tag("idea-plugin-property-7")
    @Label("Feature: idea-plugin, Property 7: Failure notification body is well-formed and bounded")
    void failureBodyIsWellFormedAndBounded(
            @ForAll("classNames") String className,
            @ForAll("messages") String message) {

        // (1) Function never throws.
        String body = Assertions.assertDoesNotThrow(
                () -> ClassTrimNotifier.formatFailureBody(className, message),
                "formatFailureBody must never throw");

        // (2) Body starts with className.
        Assertions.assertTrue(
                body.startsWith(className),
                () -> "Body did not start with className. body=<" + body
                        + ">, className=<" + className + ">");

        // (3) Separator follows immediately after className.
        String prefix = className + SEPARATOR;
        Assertions.assertTrue(
                body.startsWith(prefix),
                () -> "Body did not start with className + \": \". body=<" + body
                        + ">, prefix=<" + prefix + ">");

        // Extract the message portion past the className + ": " prefix.
        String messagePortion = body.substring(prefix.length());
        int max = ClassTrimNotifier.FAILURE_MESSAGE_MAX_LENGTH;

        // (4) Message portion length <= 500.
        Assertions.assertTrue(
                messagePortion.length() <= max,
                () -> "Message portion exceeded the " + max
                        + "-char cap; actual length=" + messagePortion.length());

        // (5)/(6)/(7) Per-arm equality with the truncate contract.
        if (message == null) {
            Assertions.assertEquals("", messagePortion,
                    "Message portion must be empty when message is null");
        } else if (message.length() <= max) {
            Assertions.assertEquals(message, messagePortion,
                    "Message portion must equal message when length <= " + max);
        } else {
            Assertions.assertEquals(message.substring(0, max), messagePortion,
                    "Message portion must equal message.substring(0, "
                            + max + ") when length > " + max);
        }
    }

    // --- Generators -----------------------------------------------------------

    /**
     * Class names: non-blank fully-qualified-class-name-like strings, e.g.
     * {@code "pkg.foo"}, {@code "java.lang.NullPointerException"}. The generator
     * draws a 1..20-char lower-case suffix and prefixes a stable {@code "pkg."}
     * package segment so every produced value is non-blank, non-null, and
     * contains a {@code "."} as one would see in a real JVM class name. Length
     * stays well under any plausible 200-char ceiling.
     */
    @Provide
    Arbitrary<String> classNames() {
        Arbitrary<String> simpleName = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(20);
        return simpleName.map(s -> "pkg." + s);
    }

    /**
     * Messages: weighted mixture biased to exercise every arm of the
     * {@link ClassTrimNotifier#truncate(String, int)} contract and the
     * adversarial shapes called out in the task brief:
     * <ul>
     *   <li>{@code null} (drives the null-message arm of {@code truncate}),</li>
     *   <li>{@code ""} (boundary at zero-length),</li>
     *   <li>short normal strings (the typical happy path),</li>
     *   <li>very long strings ({@code length > 500}) so the truncation arm is
     *       hit; bounded above at {@code 1500} so the test stays fast,</li>
     *   <li>strings containing embedded newlines (verifies the formatter does
     *       not split on line breaks),</li>
     *   <li>non-ASCII strings (Unicode supplementary characters and accented
     *       letters) so we exercise the formatter on multi-byte content.</li>
     * </ul>
     */
    @Provide
    Arbitrary<String> messages() {
        int max = ClassTrimNotifier.FAILURE_MESSAGE_MAX_LENGTH;

        Arbitrary<String> shortNormal = Arbitraries.strings()
                .ascii()
                .ofMinLength(1)
                .ofMaxLength(80);

        Arbitrary<String> veryLong = Arbitraries.strings()
                .ascii()
                .ofMinLength(max + 1)
                .ofMaxLength(max * 3);

        // Embedded newlines: build a string that mixes line-feeds with random
        // letters. The min length straddles the 500-char cap so this generator
        // also exercises the truncation arm in combination with newlines.
        Arbitrary<String> withNewlines = Arbitraries.strings()
                .withChars('\n', '\r', 'a', 'b', 'c', ' ')
                .ofMinLength(1)
                .ofMaxLength(max + 100);

        // Non-ASCII: accented Latin, Cyrillic, CJK ideographs, and an emoji
        // (U+1F600). The emoji is a surrogate pair so each occurrence consumes
        // two char positions, which exercises substring(0, max) at a code-point
        // boundary that may fall mid-pair — that is acceptable here because the
        // formatter operates on chars, and the property mirrors that contract.
        Arbitrary<String> nonAscii = Arbitraries.strings()
                .withChars('é', 'ñ', 'ü', 'ø', 'Ж', '中', '日', '本', '\uD83D', '\uDE00')
                .ofMinLength(1)
                .ofMaxLength(max + 50);

        return Arbitraries.frequencyOf(
                net.jqwik.api.Tuple.of(2, Arbitraries.just((String) null)),
                net.jqwik.api.Tuple.of(1, Arbitraries.just("")),
                net.jqwik.api.Tuple.of(4, shortNormal),
                net.jqwik.api.Tuple.of(3, veryLong),
                net.jqwik.api.Tuple.of(2, withNewlines),
                net.jqwik.api.Tuple.of(2, nonAscii)
        );
    }
}
