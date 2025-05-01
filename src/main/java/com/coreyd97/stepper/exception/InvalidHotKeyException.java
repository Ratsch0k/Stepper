package com.coreyd97.stepper.exception;

/**
 * Exception thrown if an invalid hotkey is configured.
 */
public class InvalidHotKeyException extends Exception {
    public InvalidHotKeyException(String invalidHotkey) {
        super("Invalid hotkey: " + invalidHotkey);
    }
}
