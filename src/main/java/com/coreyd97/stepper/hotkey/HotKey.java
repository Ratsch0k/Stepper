package com.coreyd97.stepper.hotkey;

import com.coreyd97.BurpExtenderUtilities.PreferenceListener;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.coreyd97.stepper.Stepper;

/**
 * Base class for all hotkeys.
 * 
 * Implements some of the shared logic of all hotkeys such as adding the preference listener.
 */
public abstract class HotKey {
    final String name;
    String hotkey;

    public HotKey(String name) {
        Preferences preferences = Stepper.getPreferences();

        this.name = name;
        this.hotkey = (String) preferences.getSetting(name);

        preferences.addSettingListener(new PreferenceListener() {
            @Override
            public void onPreferenceSet(Object source, String settingName, Object newValue) {
                if (!settingName.equals(HotKey.this.name) || !(newValue instanceof String)) {
                    return;
                }

                HotKey.this.updateHotKey((String) newValue);
            }
            
        });
    }

    /**
     * Register this hotkey
     */
    public abstract void register();


    /**
     * Deregisters the hotkey
     */
    public abstract void deregister();

    /**
     * Check whether this hotkey is registered.
     * @return
     */
    public abstract boolean isRegistered();

    /**
     * Updates this hotkey with a new key combination.
     * @param newKeyCombination New key combination.
     */
    public void updateHotKey(String newKeyCombination) {
        boolean wasRegistered = HotKey.this.isRegistered();
        if (wasRegistered) {
            HotKey.this.deregister();
        }
        
        HotKey.this.hotkey = (String) newKeyCombination;

        if (wasRegistered) {
            HotKey.this.register();
        }
    }
}
