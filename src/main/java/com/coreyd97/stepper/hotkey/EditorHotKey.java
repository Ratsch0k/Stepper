package com.coreyd97.stepper.hotkey;

import java.util.Optional;

import com.coreyd97.stepper.Stepper;

import burp.api.montoya.core.Registration;
import burp.api.montoya.ui.hotkey.HotKeyContext;
import burp.api.montoya.ui.hotkey.HotKeyHandler;

/**
 * Hotkey for the editor.
 */
public class EditorHotKey extends HotKey {
    private final HotKeyHandler handler;
    private Optional<Registration> registration;

    public EditorHotKey(String name, HotKeyHandler handler) {
        super(name);

        this.handler = handler;
        this.registration = Optional.empty();
    }

    @Override
    public void register() {
        if (this.isRegistered()) {
            return;
        }

        try {
            Registration registration =  Stepper.montoyaApi.userInterface().registerHotKeyHandler(HotKeyContext.HTTP_MESSAGE_EDITOR, this.hotkey, this.handler);
            this.registration =  Optional.of(registration);
        } catch (IllegalArgumentException e) {
            
        }

    }

    public boolean isRegistered() {
        return this.registration.isPresent();
    }

    @Override
    public void deregister() {
        if (!this.isRegistered()) {
            return;
        }
        
        Registration registration = this.registration.get();
        registration.deregister();
        this.registration = Optional.empty();
    }
}
