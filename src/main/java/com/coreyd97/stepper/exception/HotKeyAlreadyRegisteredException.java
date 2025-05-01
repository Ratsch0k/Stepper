package com.coreyd97.stepper.exception;

public class HotKeyAlreadyRegisteredException extends Exception {
    public HotKeyAlreadyRegisteredException() {
        super("HotKey is already registered");
    }   
}
