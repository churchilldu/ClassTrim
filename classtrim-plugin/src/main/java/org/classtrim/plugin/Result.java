package org.classtrim.plugin;

import java.util.Objects;
import java.util.Optional;

/**
 * Generic, immutable result type used by pure validation/factory functions in the plugin
 * to express success ({@link Success}) or a typed failure ({@link Failure}) without
 * resorting to exceptions or {@code null}.
 *
 * <p>Modeled as a sealed interface with two record variants so that callers can use
 * pattern matching ({@code switch (result) { case Success<S, E> ok -> ...; case Failure<S, E> err -> ...; }})
 * to handle both arms exhaustively.</p>
 *
 * <p>{@code Result} is intentionally local to {@code classtrim-plugin}: there is no
 * existing {@code Result} type in {@code classtrim-core}, and the plugin's pure layer
 * (validation, factory) is the only consumer.</p>
 *
 * @param <S> the success value type
 * @param <E> the failure (error) type
 */
public sealed interface Result<S, E> permits Result.Success, Result.Failure {

    /**
     * Successful result carrying a non-{@code null} value.
     *
     * <p>The record component is named {@code successValue} so that its
     * canonical accessor {@code successValue()} does not clash with the
     * interface's {@link #value()} default that returns
     * {@code Optional<S>}.</p>
     *
     * @param successValue the success value
     * @param <S>          the success value type
     * @param <E>          the failure type (unused on this branch)
     */
    record Success<S, E>(S successValue) implements Result<S, E> {
        public Success {
            Objects.requireNonNull(successValue, "Success.successValue");
        }
    }

    /**
     * Failed result carrying a non-{@code null} error.
     *
     * <p>The record component is named {@code errorValue} so that its
     * canonical accessor {@code errorValue()} does not clash with the
     * interface's {@link #error()} default that returns
     * {@code Optional<E>}.</p>
     *
     * @param errorValue the failure value
     * @param <S>        the success type (unused on this branch)
     * @param <E>        the failure type
     */
    record Failure<S, E>(E errorValue) implements Result<S, E> {
        public Failure {
            Objects.requireNonNull(errorValue, "Failure.errorValue");
        }
    }

    /**
     * Builds a successful result.
     *
     * @param value the success value (non-{@code null})
     * @param <S>   the success value type
     * @param <E>   the failure type
     * @return a {@link Success} carrying {@code value}
     */
    static <S, E> Result<S, E> success(S value) {
        return new Success<>(value);
    }

    /**
     * Builds a failed result.
     *
     * @param error the failure value (non-{@code null})
     * @param <S>   the success type
     * @param <E>   the failure type
     * @return a {@link Failure} carrying {@code error}
     */
    static <S, E> Result<S, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * @return {@code true} when this is a {@link Success}
     */
    default boolean isSuccess() {
        return this instanceof Success<S, E>;
    }

    /**
     * @return {@code true} when this is a {@link Failure}
     */
    default boolean isFailure() {
        return this instanceof Failure<S, E>;
    }

    /**
     * Returns the success value wrapped in an {@link Optional} when this is a
     * {@link Success}, otherwise an empty {@link Optional}.
     *
     * @return the success value, if any
     */
    default Optional<S> value() {
        if (this instanceof Success<S, E> ok) {
            return Optional.of(ok.successValue());
        }
        return Optional.empty();
    }

    /**
     * Returns the failure value wrapped in an {@link Optional} when this is a
     * {@link Failure}, otherwise an empty {@link Optional}.
     *
     * @return the failure value, if any
     */
    default Optional<E> error() {
        if (this instanceof Failure<S, E> err) {
            return Optional.of(err.errorValue());
        }
        return Optional.empty();
    }
}
