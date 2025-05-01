package com.coreyd97.stepper.sequence.view;

import com.coreyd97.stepper.Globals;
import com.coreyd97.stepper.StepSequenceExecutor;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.hotkey.HotKeyManager;
import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.step.view.StepStatePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StepSequenceStateTab extends JPanel {
    private final StepSequenceState stepSequence;

    private SequenceStateContainer stepsContainer;
    private SequenceStateControlPanel controlPanel;

    public StepSequenceStateTab(StepSequenceState stepSequence){
        super(new BorderLayout());
        this.stepSequence = stepSequence;
        this.stepsContainer = new SequenceStateContainer(this.stepSequence);
        this.controlPanel = new SequenceStateControlPanel(this.stepSequence);
        add(this.stepsContainer, BorderLayout.CENTER);
        add(this.controlPanel, BorderLayout.SOUTH);

        this.registerExecuteStepKeybind();
    }

    private void registerExecuteStepKeybind() {
        HotKeyManager manager = HotKeyManager.getInstance();
        AbstractAction action = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> StepSequenceExecutor.execute(stepSequence, true)).start();
            }
            
        };

        try {
            manager.registerMultiComponentHotKey(Globals.HOTKEY_EXECUTE_SEQUENCE, action, this);
        } catch (Exception e) {
            Stepper.callbacks.printError("Could not register hotkey to execute sequence");
        }
    }


    public SequenceStateContainer getStepsContainer() {
        return stepsContainer;
    }

    public StepStatePanel getSelectedStepPanel(){
        return stepsContainer.getSelectedStepPanel();
    }

    public StepSequenceState getStepSequence() {
        return this.stepSequence;
    }
}
