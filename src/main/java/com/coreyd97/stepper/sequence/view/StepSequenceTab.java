package com.coreyd97.stepper.sequence.view;

import com.coreyd97.stepper.Globals;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.hotkey.HotKeyManager;
import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.step.view.StepPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StepSequenceTab extends JPanel {
    private final StepSequence stepSequence;

    private SequenceContainer stepsContainer;
    private ControlPanel controlPanel;

    public StepSequenceTab(StepSequence stepSequence){
        super(new BorderLayout());
        this.stepSequence = stepSequence;
        this.stepsContainer = new SequenceContainer(this.stepSequence);
        this.controlPanel = new ControlPanel(this.stepSequence);
        add(this.stepsContainer, BorderLayout.CENTER);
        add(this.controlPanel, BorderLayout.SOUTH);

        this.registerExecuteStepKeybind();
    }

    private void registerExecuteStepKeybind() {
        HotKeyManager manager = HotKeyManager.getInstance();
        AbstractAction action = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(stepSequence::executeAsync);
            }
            
        };

        try {
            manager.registerMultiComponentHotKey(Globals.HOTKEY_EXECUTE_SEQUENCE, action, this);
        } catch (Exception e) {
            Stepper.callbacks.printError("Could not register hotkey to execute sequence");
        }
    }


    public SequenceContainer getStepsContainer() {
        return stepsContainer;
    }

    public StepPanel getSelectedStepPanel(){
        return stepsContainer.getSelectedStepPanel();
    }

    public StepSequence getStepSequence() {
        return this.stepSequence;
    }
}
