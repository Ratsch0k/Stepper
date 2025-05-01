package com.coreyd97.stepper.hotkey;

import java.util.HashMap;
import java.util.List;

import javax.swing.Action;

import com.coreyd97.stepper.exception.HotKeyAlreadyRegisteredException;
import com.coreyd97.stepper.exception.HotKeyNotFoundException;
import com.coreyd97.stepper.exception.InvalidHotKeyException;

import burp.api.montoya.ui.hotkey.HotKeyHandler;

import javax.swing.JComponent;

/**
 * Hotkey manager.
 * 
 * Manages all hotkeys defined by Stepper.
 *  
 * Parts of this extension can register different types of hotkeys with this manager.
 * Each hotkey has at least a name and an action associated with it.
 * 
 * The following type of hotkeys exist:
 *  - EditorHotKey: Active when the user is currently in an editor
 *  - ComponentHotKey: General hotkey for any component. Active as long as the component is in the focused window
 *  - MultiComponentHotkey: Similar to ComponentHotKey but allows using the same key combination for multiple components.
 * 
 * Hotkeys use the preference with the same name to retrieve the keybinding.
 * If this preference change, the hotkey automatically updates its keybinding.
 * 
 * Every part of Stepper that wants to register a configurable hotkey should get an instance of this manager
 * with {@code HotKeyManager.getInstance()} and register the hotkey with it.
 * 
 * Additionally, the same named preferences should be configured in {@code StepperPreferenceFactory} and
 * shown with {@code OptionsPanel}.
 */
public class HotKeyManager {
    private final HashMap<String, HotKey> hotkeys;
    private static HotKeyManager instance;

    /**
     * Internal private constructor.
     */
    private HotKeyManager() {        
        this.hotkeys = new HashMap<>();
    }

    /**
     * Get the global manager instance.
     * 
     * HotKeys should be managed globally with one managing instance.
     * Thus, there should only ever be one instance of this manager.
     * Use this function to get the current instance
     * @return Instance of the manager
     */
    public static HotKeyManager getInstance() {
        if (HotKeyManager.instance != null) {
            return HotKeyManager.instance;
        }

        HotKeyManager.instance = new HotKeyManager();

        return HotKeyManager.instance;
    }

    /**
     * Get all registered hotkeys
     * @return All hotkeys
     */
    public List<HotKey> getHotKeys() {
        return this.hotkeys.values().stream().toList();
    }

    /**
     * Get the hotkey by name or throw an exception.
     * @param name The name to search for
     * @return The hotkey with the name
     * @throws HotKeyNotFoundException If no hotkey with the name exists
     */
    public HotKey getHotKey(String name) throws HotKeyNotFoundException {
        if (!this.hotkeys.containsKey(name)) {
            throw new HotKeyNotFoundException(name);
        }

        return this.hotkeys.get(name);
    }

    /**
     * Register an editor hotkey
     * @param name Name of the hotkey
     * @param handler Handler that is called when the hotkey is pressed
     * @return The hotkey
     * @throws HotKeyAlreadyRegisteredException If a hotkey with this name is already registered
     */
    public EditorHotKey registerEditorHotKey(String name, HotKeyHandler handler) throws HotKeyAlreadyRegisteredException {
        if (this.hotkeys.containsKey(name)) {
            throw new HotKeyAlreadyRegisteredException();
        }
        
        EditorHotKey editorHotKey = new EditorHotKey(name, handler);
        editorHotKey.register();

        this.hotkeys.put(name, editorHotKey);

        return editorHotKey;
    }

    public ComponentHotKey registerComponentHotKey(String name, Action action, JComponent component) throws HotKeyAlreadyRegisteredException, InvalidHotKeyException {
        if (this.hotkeys.containsKey(name)) {
            throw new HotKeyAlreadyRegisteredException();
        }

        ComponentHotKey componentHotKey = new ComponentHotKey(name, action, component);
        componentHotKey.register();

        this.hotkeys.put(name, componentHotKey);
        return componentHotKey;
    }

    /**
     * Register a multi component hotkey.
     * 
     * In contrast to other hotkeys, this hotkey allows using the same name for hotkeys with different actions
     * and on different components.
     * If a hotkey might be used on different components (for example, on each StepTab), this function can be
     * used. As all hotkeys share the same name, they can be configured using one preferences.
     * @param name Name
     * @param action The associated action
     * @param commponent The component where the hotkey should be placed on
     * @return The hotkey
     * @throws HotKeyAlreadyRegisteredException If a hotkey with the name already exists but it isn't a multi component hotkey
     */
    public MultiComponentHotKey registerMultiComponentHotKey(String name, Action action, JComponent commponent) throws HotKeyAlreadyRegisteredException {
        MultiComponentHotKey multiComponentHotKey;
        
        if (this.hotkeys.containsKey(name)) {
            HotKey registeredHotKey = this.hotkeys.get(name);

            if (!(registeredHotKey instanceof MultiComponentHotKey)) {
                throw new HotKeyAlreadyRegisteredException();
            }

            multiComponentHotKey = (MultiComponentHotKey) registeredHotKey;
        } else {
            multiComponentHotKey = new MultiComponentHotKey(name);
            multiComponentHotKey.register();
            this.hotkeys.put(name, multiComponentHotKey);
        }
        multiComponentHotKey.addComponentHotKey(commponent, action);

        return multiComponentHotKey;
    }

    /**
     * Check if any hotkey uses the given key combination
     * @param keyCombination The key combination to look for
     * @return Whether any hotkey uses this key combination.
     */
    public boolean isHotKeyUsed(String keyCombination) {
        for (HotKey registeredHotKey : this.hotkeys.values()) {
            if (registeredHotKey.hotkey.equals(keyCombination)) {
                return true;
            }
        }

        return false;
    }
}
