package com.veldin;

/**
 * Represents a supplier of a result that can throw a checked Exception.
 *
 * This is a functional interface, so it can be used with lambdas or method references.
 * @param <T> the type of the result supplied
 */
@FunctionalInterface
public interface Throwing<T> {
    T get() throws Exception;
}
