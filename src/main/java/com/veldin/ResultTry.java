package com.veldin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Utility class providing 'safe'(ish) execution of operations that may throw exceptions.
 * Wraps operations in ResultEx and provides helpers.
 */
public final class ResultTry {

    /* Don't instantiate this */
    private ResultTry() {}

    /**
     * Executes a Throwing operation and wraps the result in a ResultEx.
     *
     * @param supplier The operation to execute
     * @param <T> Type of the result
     * @return ResultEx.ok(value) if successful, ResultEx.err(exception) if an exception is thrown
     */
    public static <T> ResultEx<T> doTry(Throwing<T> supplier) {
        try {
            return ResultEx.ok(supplier.get());
        } catch (Exception e) {
            return ResultEx.err(e);
        }
    }

    /**
     * Executes a Throwing operation with retries.
     * If the operation fails, it is retried up to 'attempts' times.
     *
     * @param supplier The operation to execute
     * @param attempts Number of attempts (needs to be positive)
     * @param <T> Type of the result
     * @return The first successful ResultEx or the last failure
     */
    public static <T> ResultEx<T> doTryWithRetry(Throwing<T> supplier, int attempts) {
        if(attempts < 1){
            return ResultEx.err(new IllegalArgumentException("Attempts needs to be positive."));
        }

        for (int i = 0; i < attempts; i++) {
            ResultEx<T> r = doTry(supplier);
            if (r.isOk()) return r;
        }
        return doTry(supplier); // last try if all failed
    }

    /**
     * Executes a Throwing operation twice.
     * As in doTryWithRetry('supplier', 2)
     *
     * @param supplier The operation to execute
     * @param <T> Type of the result
     * @return ResultEx containing the first success or the last failure
     */
    public static <T> ResultEx<T> doTryTwice(Throwing<T> supplier) {
        return doTryWithRetry(supplier, 2);
    }

    /**
     * Executes a Throwing operation with retries and exponential backoff.
     * The delay between retries doubles after each failed attempt.
     *
     * @param supplier The operation to execute
     * @param attempts Number of attempts (needs to be positive)
     * @param initialDelay Initial delay in milliseconds before retry (needs to be zero or positive)
     * @param <T> Type of the result
     * @return The first successful ResultEx or the last failure
     */
    public static <T> ResultEx<T> doTryWithExponentialBackoff(Throwing<T> supplier, int attempts, long initialDelay) {
        if(attempts < 1){
            return ResultEx.err(new IllegalArgumentException("Attempts needs to be positive."));
        }
        if(initialDelay < 0){
            return ResultEx.err(new IllegalArgumentException("InitialDelay needs to be zero or positive."));
        }

        long delay = initialDelay;
        for (int i = 0; i < attempts; i++) {
            ResultEx<T> r = doTry(supplier);
            if (r.isOk()){
                return r;
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return ResultEx.err(e);
            }
            delay *= 2; // exponential backoff
        }
        return doTry(supplier);
    }

    /**
     * Executes a Throwing operation and returns a default value if it fails.
     *
     * @param supplier The operation to execute
     * @param defaultValue Value to return if the operation fails
     * @param <T> Type of the result
     * @return The operation result if successful, or the default value
     */
    public static <T> T doTryOrDefault(Throwing<T> supplier, T defaultValue) {
        return doTry(supplier).fold(v -> v, e -> defaultValue);
    }

    /**
     * Executes a primary operation, and if it fails, executes a fallback operation.
     *
     * @param first Primary operation
     * @param fallback Fallback operation to execute if first fails
     * @param <T> Type of the result
     * @return ResultEx of the first successful operation
     */
    public static <T> ResultEx<T> doTryElseTry(Throwing<T> first, Throwing<T> fallback) {
        ResultEx<T> r = doTry(first);
        if (r.isOk()) return r;
        return doTry(fallback);
    }

    /**
     * Executes a list of Throwing operations sequentially and collects their results.
     *
     * @param suppliers List of operations
     * @param <T> Type of the results
     * @return List of ResultEx (with an entry per supplier).
     */
    public static <T> List<ResultEx<T>> doTryMultiple(List<Throwing<T>> suppliers) {
        List<ResultEx<T>> results = new ArrayList<>();
        for (Throwing<T> supplier : suppliers) {
            results.add(doTry(supplier));
        }
        return results;
    }

    /**
     * Executes a list of Throwing operations asynchronously and collects their results.
     * All operations run in parallel. (CompletableFuture)
     *
     * @param suppliers List of operations
     * @param <T> Type of the results
     * @return List of ResultEx (with an entry per supplier).
     */
    public static <T> List<ResultEx<T>> doTryMultipleAsync(List<Throwing<T>> suppliers) {
        // Create a list of futures
        List<CompletableFuture<ResultEx<T>>> futures = new ArrayList<>();
        for (Throwing<T> supplier : suppliers) {
            CompletableFuture<ResultEx<T>> future = CompletableFuture.supplyAsync(() -> ResultTry.doTry(supplier));
            futures.add(future);
        }

        // Wait for all futures to complete and collect results
        List<ResultEx<T>> results = new ArrayList<>();
        for (CompletableFuture<ResultEx<T>> future : futures) {
            try {
                results.add(future.get()); // blocks until each future completes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                results.add(ResultEx.err(e));
            } catch (ExecutionException e) {
                results.add(ResultEx.err(e.getCause() instanceof Exception ex ? ex : new RuntimeException(e)));
            }
        }

        return results;
    }

    /**
     * Executes a list of Throwing operations sequentially until one succeeds.
     *
     * @param suppliers List of operations
     * @param <T> Type of the result
     * @return The first successful ResultEx, or the last failure if they all fail
     */
    public static <T> ResultEx<T> doTryChainUntilOk(List<Throwing<T>> suppliers) {
        for (Throwing<T> s : suppliers) {
            ResultEx<T> r = doTry(s);
            if (r.isOk()) {
                return r;
            }
        }
        return doTry(suppliers.getLast()); // last try fails
    }

}
