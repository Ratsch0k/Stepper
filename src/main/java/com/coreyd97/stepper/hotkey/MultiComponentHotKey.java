package com.coreyd97.stepper.hotkey;

import java.util.HashMap;

import javax.swing.Action;
import javax.swing.JComponent;

import com.coreyd97.stepper.exception.HotKeyAlreadyRegisteredException;

/**
 * Shared hotkey between multiple components and actions.
 */
public class MultiComponentHotKey extends HotKey {
    private HashMap<JComponent, ComponentHotKey> componentHotKeys;
    private boolean isRegistered;

    public MultiComponentHotKey(String name) {
        super(name);
        this.isRegistered = false;
        this.componentHotKeys = new HashMap<>();
    }

    public void addComponentHotKey(JComponent component, Action action) throws HotKeyAlreadyRegisteredException {
        if (this.componentHotKeys.containsKey(component)) {
            throw new HotKeyAlreadyRegisteredException();
        }

        ComponentHotKey componentHotKey = new ComponentHotKey(this.name, action, component);
        if (this.isRegistered) {
            componentHotKey.register();
        }

        this.componentHotKeys.put(component, componentHotKey);
    }

    @Override
    public void register() {
        this.isRegistered = true;
        for (ComponentHotKey componentHotKey : this.componentHotKeys.values()) {
            componentHotKey.register();
        }
    }

    @Override
    public void deregister() {
        this.isRegistered = false;
        for (ComponentHotKey componentHotKey : this.componentHotKeys.values()) {
            componentHotKey.deregister();
        }
    }
    
    @Override
    public boolean isRegistered() {
        return this.isRegistered;
    }

    @Override
    public void updateHotKey(String newHotkey) {
        // Do nothing, children will update automatically
    }
}
