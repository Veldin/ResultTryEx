package com.veldin;

/**
 * Thrown when ResultEx's Unwrap() is called on a Error.
 *
 * This is EXPLICITLY an unchecked exception.
 * This so we don't require a try/catch block.
 *
 * Note: Before calling  Unwrap(), you should ensure that the
 * ResultEx's isOk() returns true / isError() returns false,
 */
class ResultUnwrapException extends RuntimeException {
    public ResultUnwrapException(Exception cause) {
        // This exception is a Dunce Cap ;)
        super("Tried to unwrap an Error ResultEx", cause);
    }
}