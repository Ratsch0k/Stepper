package com.coreyd97.stepper.exception;

public class HotKeyNotFoundException extends Exception {
    public HotKeyNotFoundException(String hotKey) {
        super("HotKey '" + hotKey + "' not found");
    }
}
