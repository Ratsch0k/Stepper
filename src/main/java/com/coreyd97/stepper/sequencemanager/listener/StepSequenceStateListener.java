package com.coreyd97.stepper.sequencemanager.listener;

import com.coreyd97.stepper.sequence.StepSequenceState;

public interface StepSequenceStateListener {
    void onStepSequenceAdded(StepSequenceState sequence);
    void onStepSequenceRemoved(StepSequenceState sequence);
}
