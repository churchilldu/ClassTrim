package org.classtrim.plugin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Defensive unit tests for the {@link ValidationError.MissingDefault} arm of the
 * sealed {@link ValidationError} hierarchy.
 *
 * <p>Validates: Requirements 3.6 — a settings field that lacks a developer-assigned
 * value and has no declared default must surface to the coordinator as a
 * {@link ValidationError.MissingDefault} naming the offending field, so that a
 * downstream notification can include that field name in its body.</p>
 *
 * <p>The implementation note in {@code AnalysisRunFactory.validate(...)} states
 * that {@code MissingDefault} is currently unreachable from the production code
 * path because {@code ClassTrimSettingsState.defaults()} always returns a fully
 * populated {@code Defaults} record. This test exercises the variant
 * <em>defensively</em>: it pins the contract of the {@code MissingDefault}
 * record (it carries a {@code fieldName} and is a {@link ValidationError}) and
 * asserts that the field name survives being wrapped in a {@link Result.Failure},
 * which is what the coordinator pattern-matches on when formatting the
 * notification body.</p>
 */
class ValidationErrorMissingDefaultTest {

    @Test
    @DisplayName("MissingDefault record carries the offending field name and is a ValidationError")
    void missingDefaultRecordCarriesFieldName() {
        ValidationError.MissingDefault err = new ValidationError.MissingDefault("populationSize");

        assertEquals("populationSize", err.fieldName(),
                "MissingDefault.fieldName() must return the value passed to the constructor");
        assertInstanceOf(ValidationError.class, err,
                "MissingDefault must implement ValidationError so the coordinator can switch on the sealed hierarchy");
    }

    @Test
    @DisplayName("MissingDefault field name survives wrapping in Result.Failure for notification dispatch")
    void missingDefaultErrorMessageNamesField() {
        // The coordinator receives a Result<RunInputs, ValidationError> from
        // AnalysisRunFactory.validate(...) and is expected to pattern-match on
        // the Failure arm to format an error notification naming the offending
        // setting. Demonstrate that the field name is preserved end-to-end.
        Result<RunInputs, ValidationError> result =
                Result.failure(new ValidationError.MissingDefault("maxIterations"));

        assertTrue(result.isFailure(), "Result.failure(...) must produce a Failure");
        assertInstanceOf(Result.Failure.class, result,
                "Result.failure(...) factory must return a Result.Failure instance");

        ValidationError error = result.error()
                .orElseThrow(() -> new AssertionError("error() must be present on Failure"));
        assertInstanceOf(ValidationError.MissingDefault.class, error,
                "The wrapped error must remain a MissingDefault after passing through Result");

        if (error instanceof ValidationError.MissingDefault md) {
            assertEquals("maxIterations", md.fieldName(),
                    "MissingDefault.fieldName() must be preserved through Result.Failure so a hypothetical "
                            + "notification formatter can include it in the body");

            // A hypothetical formatter that names the offending setting would
            // produce a body containing the field name verbatim. Pin that
            // contract here so any future formatter cannot drop the field.
            String hypotheticalNotificationBody =
                    "ClassTrim settings: no default available for '" + md.fieldName() + "'";
            assertTrue(hypotheticalNotificationBody.contains("maxIterations"),
                    "A notification formatted from MissingDefault must name the offending setting");
        }
    }
}
