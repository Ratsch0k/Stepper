package com.coreyd97.stepper.sequence.listener;

import com.coreyd97.stepper.step.StepState;

public interface SequenceStateListener {
    void onStepAdded(StepState step);
    void onStepUpdated(StepState step);
    void onStepRemoved(StepState step);
}
