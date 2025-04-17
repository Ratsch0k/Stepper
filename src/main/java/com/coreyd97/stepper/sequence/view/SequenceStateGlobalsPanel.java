package com.coreyd97.stepper.sequence.view;

import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.variable.VariableManager;

import javax.swing.*;
import java.awt.*;

public class SequenceStateGlobalsPanel extends JPanel {

    private final StepSequenceState sequence;
    private final VariableManager globalVariableManager;


    public SequenceStateGlobalsPanel(StepSequenceState stepSequence){
        this.sequence = stepSequence;
        this.globalVariableManager = this.sequence.getGlobalVariableManager();
        buildPanel();
    }

    private void buildPanel() {
        //Build panel here
        this.setLayout(new BorderLayout());
        SequenceGlobalsTable table = new SequenceGlobalsTable(this.globalVariableManager);
        this.add(new JScrollPane(table), BorderLayout.CENTER);
        this.add(new SequenceGlobalsControlPanel(this.globalVariableManager, table), BorderLayout.SOUTH);
    }


}
