package com.coreyd97.stepper.sequence.view;

import com.coreyd97.stepper.Globals;
import com.coreyd97.stepper.StepSequenceExecutor;
import com.coreyd97.stepper.Stepper;
import com.coreyd97.stepper.sequence.StepSequence;
import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.step.view.StepStatePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;

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

        ActionMap actionMap = getActionMap();
        actionMap.put("ExecuteSequence", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                //Execute sequence
                if(Stepper.getPreferences().getSetting(Globals.PREF_ENABLE_SHORTCUT)){
                    new Thread(() -> StepSequenceExecutor.execute(stepSequence)).start();
                }
            }
        });

        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "ExecuteSequence");
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
