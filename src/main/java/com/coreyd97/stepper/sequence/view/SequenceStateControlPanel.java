package com.coreyd97.stepper.sequence.view;

import com.coreyd97.stepper.sequence.listener.SequenceExecutionListener;
import com.coreyd97.stepper.step.StepExecutionInfo;
import com.coreyd97.stepper.step.StepState;
import com.coreyd97.stepper.StepSequenceExecutor;
import com.coreyd97.stepper.sequence.StepSequenceState;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class SequenceStateControlPanel extends JPanel implements SequenceExecutionListener {

    private final StepSequenceState stepSequence;
    private final JButton executeButton;
    private final JButton cancelButton;
    private int stepsToExecute;
    private int stepsExecuted;

    public SequenceStateControlPanel(StepSequenceState stepSequence){
        this.stepSequence = stepSequence;
        this.setLayout(new BorderLayout());

        this.executeButton = new JButton("Execute Sequence");
        this.executeButton.addActionListener(actionEvent -> {
            new Thread(() -> {
                StepSequenceExecutor.execute(this.stepSequence, true);
            }).start();
        });

        //TODO Implement sequence cancel
        this.cancelButton = new JButton("Cancel");
//        this.cancelButton.setEnabled(false);
//        this.cancelButton.addActionListener(actionEvent -> {
////            this.stepSequence.cancelSequence();
//        });
//
//        add(cancelButton, BorderLayout.EAST);
        add(executeButton, BorderLayout.CENTER);
        this.stepSequence.addSequenceExecutionListener(this);
    }

    @Override
    public void beforeSequenceStateStart(List<StepState> steps) {
        this.stepsToExecute = steps.size();
        this.stepsExecuted = 0;
        this.executeButton.setEnabled(false);
        this.executeButton.setText("Executing... (" + stepsExecuted + "/" + stepsToExecute + ")");
        this.cancelButton.setEnabled(true);
    }

    @Override
    public void sequenceStepExecuted(StepExecutionInfo stepExecutionInfo) {
        this.stepsExecuted++;
        this.executeButton.setText("Executing... (" + stepsExecuted + "/" + stepsToExecute + ")");
    }

    @Override
    public void afterSequenceEnd(boolean success) {
        this.cancelButton.setEnabled(false);
        this.executeButton.setEnabled(true);
        this.executeButton.setText("Execute Sequence");
    }
}
