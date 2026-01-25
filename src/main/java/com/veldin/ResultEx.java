package com.veldin;

import java.util.function.Function;

/**
 * Represents the outcome of an operation that can either succeed with a value (Ok)
 * or fail with an Exception (Error).
 */
public sealed interface ResultEx<T>
        permits ResultEx.Ok, ResultEx.Error {

    /**
     * @return true if this ResultEx is Ok
     * returns false otherwise.
     */
    boolean isOk();

    /**
     * @return true if this ResultEx is an Error
     * returns false otherwise.
     */
    boolean isError();

    /**
     * Construct a successful ResultEx
     *
     * @param value The value to wrap
     * @param <T> Type of the value
     * @return an Ok ResultEx
     */
    static <T> ResultEx<T> ok(T value) {
        return new Ok<>(value);
    }

    /**
     * Construct an error ResultEx
     *
     * @param ex The Exception to wrap
     * @param <T> Type of the value that would have been returned
     * @return an Error ResultEx
     */
    static <T> ResultEx<T> err(Exception ex) {
        return new Error<>(ex);
    }

    /**
     * Transforms the value using the given function if Ok.
     * Errors are passed through.
     *
     * @param f The mapping function to apply to the value
     * @param <U> The type of the resulting value
     * @return a new ResultEx containing the mapped value or the original error
     */
    <U> ResultEx<U> map(Function<? super T, ? extends U> f);

    /**
     * Transforms the value using a function that returns a ResultEx.
     * This is useful for chaining operations that may also fail.
     *
     * @param f The function returning a ResultEx
     * @param <U> The type of the resulting value
     * @return a new ResultEx containing the result of the function or the original error
     */
    <U> ResultEx<U> flatMap(Function<? super T, ResultEx<U>> f);

    /**
     * Performs pattern matching on this ResultEx:
     * applies onOk to the value if Ok, or onErr to the Exception if Error.
     *
     * @param onOk Function to handle the Ok value
     * @param onErr Function to handle the Exception
     * @param <R> The return type
     * @return the result of applying the appropriate function
     */
    <R> R fold(
            Function<? super T, ? extends R> onOk,
            Function<? super Exception, ? extends R> onErr
    );

    /**
     * Return the value if Ok, or throw an unchecked ResultUnwrapException if Error.
     * This method is explicitly UNCHECKED, make sure isOk() returns True / isError() returns false beforehand.
     */
    T unwrap();

    /**
     * Return the Exception if Error, or throw an unchecked ResultUnwrapException if Ok.
     */
    Exception unwrapError();

    /**
     * Check if this Error is of given type.
     *
     * @param type The Exception to check
     * @return true if this is an Error and its Exception is an instance of type
     */
    default boolean isErrorOfType(Class<? extends Exception> type) {
        if (this instanceof Error<T>(Exception error)) {
            return type.isInstance(error);
        }
        return false;
    }

    /**
     * Represents a successful result.
     *
     * @param <T> Type of the value
     */
    record Ok<T>(T value) implements ResultEx<T> {

        public boolean isOk() {
            return true;
        }

        public boolean isError() {
            return false;
        }

        public <U> ResultEx<U> map(Function<? super T, ? extends U> f) {
            return ok(f.apply(value));
        }

        public <U> ResultEx<U> flatMap(Function<? super T, ResultEx<U>> f) {
            return f.apply(value);
        }

        public <R> R fold(Function<? super T, ? extends R> onOk,
                          Function<? super Exception, ? extends R> onErr) {
            return onOk.apply(value);
        }

        public T unwrap() {
            return value;
        }

        public Exception unwrapError() {
            throw new ResultUnwrapException(new IllegalStateException("Called unwrapError() on an Ok value"));
        }
    }

    /**
     * Represents a failed result.
     *
     * @param <T> Type of the value that would have been returned
     */
    record Error<T>(Exception error) implements ResultEx<T> {

        public boolean isOk() {
            return false;
        }

        public boolean isError() {
            return true;
        }

        public <U> ResultEx<U> map(Function<? super T, ? extends U> f) {
            return err(error);
        }

        public <U> ResultEx<U> flatMap(Function<? super T, ResultEx<U>> f) {
            return err(error);
        }

        public <R> R fold(Function<? super T, ? extends R> onOk,
                          Function<? super Exception, ? extends R> onErr) {
            return onErr.apply(error);
        }

        public T unwrap() {
            throw new ResultUnwrapException(error);
        }

        public Exception unwrapError() {
            return error;
        }
    }
}
