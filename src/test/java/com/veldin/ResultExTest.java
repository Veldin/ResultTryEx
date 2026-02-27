package com.veldin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResultExTest {

    @Test
    void testOkAndErrString() {
        // Basic creation of an ResultEx.ok
        ResultEx<String> success = ResultEx.ok("Hello");

        assertTrue(success.isOk());
        assertFalse(success.isError());

        // Basic creation of an ResultEx.err
        ResultEx<String> failure = ResultEx.err(new RuntimeException("Oops"));

        assertFalse(failure.isOk());
        assertTrue(failure.isError());

        // Get values using Fold
        String foldedSuccess = success.fold(v -> v, e -> "Failed");
        String foldedErr = failure.fold(v -> v, e -> "Failed");

        assertEquals("Hello", foldedSuccess);
        assertEquals("Failed", foldedErr);
    }

    @Test
    void testOkAndErrInt() {
        ResultEx<Integer> success = ResultEx.ok(1);

        assertTrue(success.isOk());
        assertFalse(success.isError());

        ResultEx<Integer> failure = ResultEx.err(new RuntimeException("Oops"));

        assertFalse(failure.isOk());
        assertTrue(failure.isError());

        // Fold
        int foldedSuccess = success.fold(v -> v, e -> -1);
        int foldedErr = failure.fold(v -> v, e -> -1);

        assertEquals(1, foldedSuccess);
        assertEquals(-1, foldedErr);
    }

    @Test
    void testPeek(){
        ResultEx<String> success = ResultEx.ok("Hello");

        assertTrue(success.isOk());
        assertFalse(success.isError());

        ResultEx<String> failure = ResultEx.err(new RuntimeException("Oops"));

        assertFalse(failure.isOk());
        assertTrue(failure.isError());

        // Do add on the ResultEx's
        List<String> captured = new ArrayList<>();
        success.peek(captured::add);
        failure.peek(captured::add);

        // Assert if only 'success' was added.
        assertEquals(1, captured.size());
        assertEquals("Hello", captured.getFirst());
    }

    @Test
    void testMapAndFlatMap() {
        ResultEx<Integer> success = ResultEx.ok(10);
        ResultEx<Integer> failure = ResultEx.err(new RuntimeException("fail"));

        // map
        ResultEx<Integer> mapped = success.map(x -> x * 2);
        ResultEx<Integer> mappedErr = failure.map(x -> x * 2);

        assertEquals(20, (int) mapped.fold(v -> v, e -> 0));
        assertEquals("fail", mappedErr.fold(v -> "ok", Exception::getMessage));

        // flatMap
        ResultEx<Integer> flatMapped = success.flatMap(x -> ResultEx.ok(x + 5));
        ResultEx<Integer> flatMappedErr = failure.flatMap(x -> ResultEx.ok(x + 5));

        assertEquals(15, (int) flatMapped.fold(v -> v, e -> 0));
        assertEquals("fail", flatMappedErr.fold(v -> 0, Exception::getMessage));
    }

    @Test
    void testFold() {
        ResultEx<String> ok = ResultEx.ok("yes");
        ResultEx<String> err = ResultEx.err(new RuntimeException("no"));

        String okFolded = ok.fold(v -> "Got: " + v, e -> "Fail: " + e.getMessage());
        String errFolded = err.fold(v -> "Got: " + v, e -> "Fail: " + e.getMessage());

        assertEquals("Got: yes", okFolded);
        assertEquals("Fail: no", errFolded);
    }

    // ---- flatMap chaining test ----
    @Test
    void testChain() {
        ResultEx<Integer> r =
                ResultEx.ok(2)
                        .flatMap(x -> ResultEx.ok(x * 3))
                        .flatMap(x -> ResultEx.ok(x + 1));

        assertEquals(7, (int) r.fold(v -> v, e -> 0));

        ResultEx<Integer> r2 =
                ResultEx.ok(2)
                        .flatMap(x -> ResultEx.err(new RuntimeException("boom")))
                        .flatMap(x -> ResultEx.ok((int)x + 1));

        assertEquals("boom", r2.fold(v -> "", Exception::getMessage));
    }

    @Test
    void testSideEffects() {
        AtomicInteger counter = new AtomicInteger(0);
        ResultEx.ok(5).map(counter::addAndGet);
        assertEquals(5, counter.get());

        ResultEx.<Integer>err(new RuntimeException("fail")).map(counter::addAndGet);
        // counter should stay the same
        assertEquals(5, counter.get());
    }

    @Test
    void testUnwrap() {
        ResultEx<String> ok = ResultEx.ok("value");
        ResultEx<String> err = ResultEx.err(new IOException("fail"));

        try {
            assertEquals("value", ok.unwrap());
        } catch (Exception e) {
            fail("Unwrap on Ok should not throw");
        }

        Exception thrown = assertThrows(ResultUnwrapException.class, err::unwrap);
        assertEquals("Tried to unwrap an Error ResultEx", thrown.getMessage());
    }

    @Test
    void testUnwrapError() {
        ResultEx<String> err = ResultEx.err(new IOException("fail"));

        assertFalse(err.isOk());
        assertTrue(err.isError());
        assertTrue(err.isErrorOfType(IOException.class));

        Exception exception = err.unwrapError();

        assertInstanceOf(IOException.class, exception);
        assertEquals("fail", exception.getMessage());

        assertNotNull(exception.getStackTrace());
    }
}
