package com.coreyd97.stepper;

import burp.IBurpExtender;
import burp.IBurpExtenderCallbacks;
import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;

import com.coreyd97.BurpExtenderUtilities.DefaultGsonProvider;
import com.coreyd97.BurpExtenderUtilities.IGsonProvider;
import com.coreyd97.BurpExtenderUtilities.Preferences;
import com.coreyd97.stepper.preferences.StepperPreferenceFactory;
import com.coreyd97.stepper.sequencemanager.SequenceManager;
import com.coreyd97.stepper.util.variablereplacementstab.VariableReplacementsTabFactory;

import javax.swing.*;

public class Stepper implements IBurpExtender, BurpExtension {

    public static Stepper instance;
    private static StepperUI ui;
    public static IBurpExtenderCallbacks callbacks;
    public static IGsonProvider gsonProvider = new DefaultGsonProvider();
    private static Preferences preferences;
    private static SequenceManager sequenceManager;
    public static MontoyaApi montoyaApi;

    private StateManager stateManager;
    private MessageProcessor messageProcessor;

    public Stepper(){
        Stepper.instance = this;

        //Fix Darcula's issue with JSpinner UI.
        try {
            @SuppressWarnings("rawtypes")
            Class spinnerUI = Class.forName("com.bulenkov.darcula.ui.DarculaSpinnerUI");
            UIManager.put("com.bulenkov.darcula.ui.DarculaSpinnerUI", spinnerUI);
        } catch (ClassNotFoundException e) {
            //Darcula is not installed.
        }
    }

    public static Stepper getInstance() {
        return instance;
    }

    public static SequenceManager getSequenceManager(){
        return sequenceManager;
    }

    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        Stepper.callbacks = callbacks;
        Stepper.preferences = new StepperPreferenceFactory(Globals.EXTENSION_NAME, gsonProvider, callbacks).buildPreferences();

        Stepper.sequenceManager = new SequenceManager();
        this.stateManager = new StateManager(sequenceManager, preferences);
        this.stateManager.loadSavedSequences();
        this.messageProcessor = new MessageProcessor(sequenceManager, preferences);

        Stepper.callbacks.setExtensionName(Globals.EXTENSION_NAME);
        Stepper.callbacks.registerMessageEditorTabFactory(new VariableReplacementsTabFactory(sequenceManager));
        Stepper.callbacks.registerContextMenuFactory(new ContextMenuFactory(sequenceManager));
        Stepper.callbacks.registerHttpListener(messageProcessor);
        Stepper.callbacks.registerExtensionStateListener(stateManager);

        SwingUtilities.invokeLater(() -> {
            try {
                ui = new StepperUI(sequenceManager);
                Stepper.callbacks.addSuiteTab(Stepper.ui);
            } catch (Exception e) {
                Stepper.callbacks.printError("Error while initiating StepperUI: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    Stepper.callbacks.printError("\t" + element.toString());
                }
            }

        });

    }

    /**
     * Initialize extension with Montoya API.
     * 
     * Some features require the Montoya API.
     * However, the extension does not yet fully support the Montoya API
     * and most features rely on the legacy API.
     * 
     * Thus, the extension is registered with both the legacy and the Montoya API.
     */
    @Override
    public void initialize(MontoyaApi api) {
        Stepper.montoyaApi = api;
    }

    public static Preferences getPreferences() {
        return preferences;
    }

    public static StepperUI getUI() {
        return ui;
    }

    public static IGsonProvider getGsonProvider() {
        return gsonProvider;
    }
}
