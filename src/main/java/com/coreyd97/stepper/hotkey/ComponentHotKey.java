package com.coreyd97.stepper.hotkey;


import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import com.coreyd97.stepper.util.Utils;

/**
 * Hotkey for a specific component.
 */
public class ComponentHotKey extends HotKey {
    private final Action action;
    private final JComponent component;
    private boolean isRegistered;
    private final ActionMap actionMap;
    private final InputMap inputMap;

    public ComponentHotKey(String name, Action action, JComponent component) {
        super(name);

        this.action = action;
        this.component = component;
        this.inputMap = this.component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.actionMap = this.component.getActionMap();
        this.isRegistered = false;
    }

    @Override
    public void register() {
        if (this.isRegistered) {
            return;
        }

        // By default the preference contain Burp's key stoke representation so we must
        // translate it into the representation used by Java Swing.
        KeyStroke keyStroke = KeyStroke.getKeyStroke(Utils.burpHotKeyToSwing(this.hotkey));

        this.actionMap.put(this, this.action);
        this.inputMap.put(keyStroke, null);
        this.inputMap.put(keyStroke, this);
        this.isRegistered = true;
    }

    public boolean isRegistered() {
        return this.isRegistered;
    }

    @Override
    public void deregister() {
        if (!this.isRegistered) {
            return;
        }

        // By default the preference contain Burp's key stoke representation so we must
        // translate it into the representation used by Java Swing.
        KeyStroke keyStroke = KeyStroke.getKeyStroke(Utils.burpHotKeyToSwing(this.hotkey));

        this.actionMap.put(this, null);
        this.inputMap.put(keyStroke, null);
        this.isRegistered = false;
    }
}
