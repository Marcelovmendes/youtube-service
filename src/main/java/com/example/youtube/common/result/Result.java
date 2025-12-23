package com.example.youtube.common.result;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    record Success<T, E>(T value) implements Result<T, E> {}

    record Failure<T, E>(E error) implements Result<T, E> {}

    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }

    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }

    static <T, E> Result<T, E> of(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(errorMapper.apply(e));
        }
    }

    default boolean isSuccess() {
        return this instanceof Success;
    }

    default boolean isFailure() {
        return this instanceof Failure;
    }

    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(mapper.apply(value));
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    default <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> mapper.apply(value);
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    default <F> Result<T, F> mapError(Function<E, F> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> success(value);
            case Failure<T, E>(var error) -> failure(mapper.apply(error));
        };
    }

    default Result<T, E> andThen(Function<T, Result<?, E>> action) {
        return switch (this) {
            case Success<T, E>(var value) -> action.apply(value).map(_ -> value);
            case Failure<T, E>(var error) -> failure(error);
        };
    }

    default Result<T, E> recover(Function<E, T> recovery) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(var error) -> success(recovery.apply(error));
        };
    }

    default Result<T, E> recoverWith(Function<E, Result<T, E>> recovery) {
        return switch (this) {
            case Success<T, E> s -> s;
            case Failure<T, E>(var error) -> recovery.apply(error);
        };
    }

    default <U> U fold(Function<T, U> onSuccess, Function<E, U> onFailure) {
        return switch (this) {
            case Success<T, E>(var value) -> onSuccess.apply(value);
            case Failure<T, E>(var error) -> onFailure.apply(error);
        };
    }

    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E> _ -> defaultValue;
        };
    }

    default T getOrElseGet(Function<E, T> mapper) {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> mapper.apply(error);
        };
    }

    default <X extends Throwable> T getOrThrow(Function<E, X> exceptionMapper) throws X {
        return switch (this) {
            case Success<T, E>(var value) -> value;
            case Failure<T, E>(var error) -> throw exceptionMapper.apply(error);
        };
    }
}
