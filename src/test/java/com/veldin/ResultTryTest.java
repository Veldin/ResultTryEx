package com.veldin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultTryTest {

    @Test
    void testDoTrySuccess() {
        ResultEx<String> r = ResultTry.doTry(() -> "Hello World");

        assertTrue(r.isOk());
        assertFalse(r.isError());

        // Safe to unwrap because we checked isOk()
        String value = r.unwrap();
        assertEquals("Hello World", value);

        // Still can use fold as alternative
        String foldValue = r.fold(v -> v, e -> "Fallback");
        assertEquals("Hello World", foldValue);
    }

    @Test
    void testGettingValueFromMethod() {
        ResultEx<Integer> r = ResultTry.doTry(() -> addXtoY(2, 3));

        assertTrue(r.isOk());

        // Safe unwrap
        int unwrapped = r.unwrap();
        assertEquals(5, unwrapped);

        // Fold alternative
        int folded = r.fold(v -> v, e -> -1);
        assertEquals(5, folded);
    }

    private int addXtoY(int x, int y) throws IOException {
        return x + y;
    }


    @Test
    void testDoTryExceptionFromMethod() {
        ResultEx<String> r = ResultTry.doTry(this::throwingMethod);

        assertFalse(r.isOk());
        assertTrue(r.isError());

        // Cannot unwrap safely here, so use fold
        String value = r.fold(v -> v, Throwable::getMessage);
        assertEquals("Oops!", value);

        // Inspect error type safely
        assertTrue(r.isErrorOfType(IOException.class));
    }

    private String throwingMethod() throws IOException {
        throw new IOException("Oops!");
    }

    @Test
    void testDoTryUncaughtExceptionFromMethod() {
        ResultEx<String> r = ResultTry.doTry(this::throwingUncaughtMethod);

        assertFalse(r.isOk());
        assertTrue(r.isError());
        assertEquals("/ by zero", r.fold(v -> "", Throwable::getMessage));

        Exception thrown = assertThrows(ResultUnwrapException.class, r::unwrap);
        assertEquals("Tried to unwrap an Error ResultEx", thrown.getMessage());
    }

    private String throwingUncaughtMethod() {
        int i = 1/0;
        return  "never happens";
    }

    @Test
    void testGettingValuesFromMethod() {
        ResultEx<Integer> r1 = ResultTry.doTry(() -> divideXbyY(10, 2));
        ResultEx<Integer> r2 = ResultTry.doTry(() -> divideXbyY(10, 0));

        assertTrue(r1.isOk());
        int value1 = r1.unwrap();
        assertEquals(5, value1);

        // r2 is error, use fold
        assertTrue(r2.isError());
        int value2 = r2.fold(v -> v, e -> -1);
        assertEquals(-1, value2);

        // Inspect error type
        assertTrue(r2.isErrorOfType(ArithmeticException.class));
    }

    private int divideXbyY(int x, int y) {
        return (x / y);
    }

    @Test
    void testDoTryTwice() {
        // wrap the method call with ResultEx
        ResultEx<Boolean> r = ResultTry.doTryTwice(this::returnsTrueSecondTime);

        assertTrue(r.isOk());
        assertFalse(r.isError());

        // get the values via fold
        boolean value = r.fold(v -> v, e -> false);
        assertTrue(value);
    }

    @Test
    void testDoTryWithRetry() {
        // wrap the method call with ResultEx
        ResultEx<Boolean> r = ResultTry.doTryWithRetry(this::returnsTrueSecondTime, 3);

        assertTrue(r.isOk());
        assertFalse(r.isError());

        // get the values via fold
        boolean value = (boolean)r.fold(v -> v, e -> false);
        assertTrue(value);
    }

    @Test
    void testDoTryWithRetryBadRequest() {
        // wrap the method call with ResultEx
        ResultEx<Boolean> r = ResultTry.doTryWithRetry(this::returnsTrueSecondTime, -1);

        assertTrue(r.isError());
        assertFalse(r.isOk());

        assertTrue(r.isErrorOfType(IllegalArgumentException.class));
        assertFalse(r.isErrorOfType(IOException.class));

        // get the values via fold
        boolean value = r.fold(v -> v, e -> false);
        assertFalse(value);
    }

    private static int returnsTrueSecondTime_attempts = 0;
    private boolean returnsTrueSecondTime() throws IOException {
        returnsTrueSecondTime_attempts++;
        if(returnsTrueSecondTime_attempts < 2){
            throw new IOException();
        }
        return true;
    }

    @Test
    void doTryWithExponentialBackoff() {
        // wrap the method call with ResultEx
        ResultEx<Boolean> r = ResultTry.doTryWithExponentialBackoff(this::returnsTrueThirdTime, 5, 20);

        assertTrue(r.isOk());
        assertFalse(r.isError());

        // get the values via fold
        boolean value = (boolean)r.fold(v -> v, e -> false);
        assertTrue(value);
    }

    private static int returnsTrueThirdTime_attempts = 0;
    private boolean returnsTrueThirdTime() throws IOException {
        returnsTrueThirdTime_attempts++;
        if(returnsTrueThirdTime_attempts < 3){
            throw new IOException();
        }
        return true;
    }

    @Test
    void testDoTryOrDefault() {
        // wrap the method call with ResultEx
        int value1 = ResultTry.doTryOrDefault(() -> divideXbyY(10, 2), -12);
        int value2 = ResultTry.doTryOrDefault(() -> divideXbyY(10, 0), -12);

        assertEquals(5, value1);
        assertEquals(-12, value2);
    }


    @Test
    void testDoTryElseTry() throws Exception {
        // wrap the method call with ResultEx
        ResultEx<Integer> r1 = ResultTry.doTryElseTry(() -> divideXbyY(10, 2), () -> divideXbyY(10, 5));
        ResultEx<Integer> r2 = ResultTry.doTryElseTry(() -> divideXbyY(10, 0),  () -> divideXbyY(10, 5));

        assertTrue(r1.isOk());
        assertFalse(r1.isError());

        assertTrue(r2.isOk());
        assertFalse(r2.isError());

        // We know we can unwrap them; (we asserted)
        int value1 = r1.unwrap();
        int value2 = r2.unwrap();

        assertEquals(5, value1);
        assertEquals(2, value2);
    }

    @Test
    void testDoTryMultiple() {
        // Prepare a list of Throwing<Integer> operations
        List<Throwing<Integer>> operations = List.of(
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 0)
        );

        // Run all operations through doTryMultiple
        List<ResultEx<Integer>> results = ResultTry.doTryMultiple(operations);

        // Extract each ResultEx
        ResultEx<Integer> r1 = results.get(0);
        ResultEx<Integer> r2 = results.get(1);

        // Assert Ok/Err states
        assertTrue(r1.isOk());
        assertFalse(r1.isError());

        assertFalse(r2.isOk());
        assertTrue(r2.isError());

        // Extract values via fold
        int value1 = r1.fold(v -> v, e -> -1);
        int value2 = r2.fold(v -> v, e -> -1);

        assertEquals(5, value1);
        assertEquals(-1, value2);
    }


    @Test
    void testDoTryMultipleAsync() {
        // Prepare a list of Throwing<Integer> operations
        List<Throwing<Boolean>> operations = List.of(
                () -> trueAfterSleep(100),
                () -> trueAfterSleep(300),
                () -> trueAfterSleep(200),
                () -> trueAfterSleep(-10),
                () -> trueAfterSleep(100)
        );

        // Run all operations through doTryMultiple
        List<ResultEx<Boolean>> results = ResultTry.doTryMultipleAsync(operations);

        int countTrues = 0;
        int countFalses = 0;

        for (ResultEx<Boolean> result : results) {
            boolean value = result.fold(v -> v, e -> false);
            if(value){
                countTrues++;
                continue;
            }

            countFalses++;
        }

        assertEquals(4, countTrues);
        assertEquals(1, countFalses);
    }

    private boolean trueAfterSleep(long time) throws InterruptedException {
        Thread.sleep(time);
        return true;
    }

    @Test
    void testDoTryChainUntilOk() {
        // Prepare a list of Throwing<Integer> operations
        List<Throwing<Integer>> operations = List.of(
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 0)
        );

        // Run all operations through doTryMultiple
        ResultEx<Integer> result = ResultTry.doTryChainUntilOk(operations);

        // Assert Ok/Err states
        assertTrue(result.isOk());
        assertFalse(result.isError());

        // Extract values via fold
        int value1 = result.fold(v -> v, e -> -1);

        assertEquals(5, value1);
    }


    @Test
    void testDoTryChainUntilError() {
        // Prepare a list of Throwing<Integer> operations
        List<Throwing<Integer>> operations = List.of(
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 2),
                () -> divideXbyY(10, 0),
                () -> divideXbyY(10, 2)
        );

        // Run all operations through doTryMultiple
        ResultEx<Integer> result = ResultTry.doTryChainUntilError(operations);

        // Assert Ok/Err states
        assertFalse(result.isOk());
        assertTrue(result.isError());

        // Extract values via fold
        int value1 = result.fold(v -> v, e -> -1);

        assertEquals(-1, value1);
    }

}
