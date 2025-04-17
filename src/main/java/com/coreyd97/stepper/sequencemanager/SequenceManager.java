package com.coreyd97.stepper.sequencemanager;

import com.coreyd97.stepper.sequence.StepSequenceState;
import com.coreyd97.stepper.sequencemanager.listener.StepSequenceStateListener;
import com.coreyd97.stepper.variable.StepVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SequenceManager {
    private final List<StepSequenceState> sequenceStates;
    private final List<StepSequenceStateListener> sequenceStateListeners;

    public SequenceManager(){
        this.sequenceStates = new ArrayList<>();
        this.sequenceStateListeners = new ArrayList<>();
    }

    public void addStepSequence(StepSequenceState sequence) {
        this.sequenceStates.add(sequence);
        for (StepSequenceStateListener stepSequenceListener : this.sequenceStateListeners) {
            try {
                stepSequenceListener.onStepSequenceAdded(sequence);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void removeStepSequence(StepSequenceState sequence) {
        this.sequenceStates.remove(sequence);
        for (StepSequenceStateListener stepSequenceListener : this.sequenceStateListeners) {
            try {
                stepSequenceListener.onStepSequenceRemoved(sequence);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void addStepSequenceListener(StepSequenceStateListener listener){
        this.sequenceStateListeners.add(listener);
    }

    public void removeStepSequenceListener(StepSequenceStateListener listener){
        this.sequenceStateListeners.remove(listener);
    }

    public List<StepSequenceState> getStepSequenceStates() {
        return this.sequenceStates;
    }

    /**
     * Map of the latest variables from each sequence.
     * E.g. If a variable is defined in step 1 and step n, the variable from step n will be used.
     * @return
     */
    public HashMap<StepSequenceState, List<StepVariable>> getRollingVariablesFromAllSequences(){
        try {
            HashMap<StepSequenceState, List<StepVariable>> allVariables = new HashMap<>();
            for (StepSequenceState stepSequence : this.sequenceStates) {
                allVariables.put(stepSequence, stepSequence.getRollingVariablesForWholeSequence());
            }
            return allVariables;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
